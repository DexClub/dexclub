package io.github.dexclub.app.scene.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dexclub.app.model.WorkspaceSummary
import io.github.dexclub.app.model.toRouteArgs
import io.github.dexclub.app.model.toWorkspaceSummary
import io.github.dexclub.compat.deleteCompat
import io.github.dexclub.core.workspace.WorkspaceCreationResult
import io.github.dexclub.core.workspace.WorkspaceImporter
import io.github.dexclub.core.workspace.WorkspaceRecord
import io.github.dexclub.core.workspace.WorkspaceRepository
import io.github.dexclub.loggerError
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeSceneViewModel(
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceImporter: WorkspaceImporter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    private val _effects = Channel<HomeUiEffect>(capacity = Channel.BUFFERED)

    val uiState: StateFlow<HomeUiState> = _uiState
    val effects = _effects.receiveAsFlow()

    fun onShowNewWorkspaceDialog() {
        _uiState.update { it.copy(newWorkspaceDialog = true) }
    }

    fun onCloseNewWorkspaceDialog() {
        _uiState.update { it.copy(newWorkspaceDialog = false) }
    }

    fun onShowDeleteConfirmDialog(item: WorkspaceSummary) {
        _uiState.update {
            it.copy(
                deleteConfirmDialog = true,
                selectedWorkspaceItem = item,
            )
        }
    }

    fun onCloseDeleteConfirmDialog() {
        _uiState.update { it.copy(deleteConfirmDialog = false) }
    }

    fun onDeleteConfirm() {
        val item = _uiState.value.selectedWorkspaceItem ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    workspaceRepository.deleteById(item.id)
                    PlatformFile(item.absolutePath).deleteCompat()
                }
                refreshWorkspaceItems()
                _uiState.update {
                    it.copy(
                        deleteConfirmDialog = false,
                        selectedWorkspaceItem = null,
                    )
                }
            }.onFailure { throwable ->
                loggerError(
                    text = "删除项目失败",
                    throwable = throwable,
                    tag = TAG,
                )
                emitEffect(HomeUiEffect.ShowMessage("删除项目失败: ${throwable.message}"))
            }
        }
    }

    fun onOpen(file: PlatformFile?) {
        if (file == null) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    loadingMessage = "检查工作区中...",
                )
            }
            try {
                val selectedPath = normalizeWorkspacePath(file.absolutePath())
                val matchedWorkspace = withContext(Dispatchers.IO) {
                    workspaceRepository.getAll()
                        .firstOrNull { workspace ->
                            normalizeWorkspacePath(workspace.absolutePath) == selectedPath
                        }
                }

                if (matchedWorkspace == null) {
                    emitEffect(HomeUiEffect.ShowMessage("未找到对应的工作区记录"))
                    return@launch
                }

                markWorkspaceOpened(matchedWorkspace.id)
                emitEffect(
                    HomeUiEffect.EnterWorkspace(
                        matchedWorkspace.toWorkspaceSummary().toRouteArgs()
                    )
                )
            } catch (throwable: Throwable) {
                loggerError(
                    text = "打开已有工作区失败",
                    throwable = throwable,
                    tag = TAG,
                )
                emitEffect(HomeUiEffect.ShowMessage("打开已有工作区失败: ${throwable.message}"))
            } finally {
                _uiState.update {
                    it.copy(
                        loading = false,
                        loadingMessage = "",
                    )
                }
            }
        }
    }

    fun onNew(
        workspaceName: String,
        targetFile: PlatformFile?,
    ) {
        val actualTargetFile = targetFile ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    loadingMessage = "",
                )
            }
            try {
                when (
                    val result = workspaceImporter.importWorkspace(
                        workspaceName = workspaceName,
                        targetFile = actualTargetFile,
                    ) { progress ->
                        _uiState.update { state -> state.copy(loadingMessage = progress) }
                    }
                ) {
                    is WorkspaceCreationResult.Success -> {
                        markWorkspaceOpened(result.workspace.id)
                        refreshWorkspaceItems()
                        _uiState.update {
                            it.copy(
                                newWorkspaceDialog = false,
                            )
                        }
                        emitEffect(
                            HomeUiEffect.EnterWorkspace(
                                result.workspace.toWorkspaceSummary().toRouteArgs()
                            )
                        )
                    }

                    is WorkspaceCreationResult.Failure -> {
                        emitEffect(HomeUiEffect.ShowMessage(result.message))
                    }
                }
            } finally {
                _uiState.update {
                    it.copy(
                        loading = false,
                        loadingMessage = "",
                    )
                }
            }
        }
    }

    private suspend fun refreshWorkspaceItems() {
        val items = withContext(Dispatchers.IO) {
            workspaceRepository.getAll()
        }
            .sortedWith(
                compareByDescending<WorkspaceRecord> { it.lastOpenedAt }
                    .thenByDescending { it.id },
            )
            .map { it.toWorkspaceSummary() }
        _uiState.update { it.copy(workspaceItems = items) }
    }

    fun onEnterWorkspace(item: WorkspaceSummary) {
        viewModelScope.launch {
            markWorkspaceOpened(item.id)
            refreshWorkspaceItems()
            emitEffect(HomeUiEffect.EnterWorkspace(item.toRouteArgs()))
        }
    }

    private suspend fun markWorkspaceOpened(workspaceId: Long) {
        withContext(Dispatchers.IO) {
            workspaceRepository.updateLastOpenedAt(
                id = workspaceId,
                lastOpenedAt = System.currentTimeMillis(),
            )
        }
    }

    private suspend fun emitEffect(effect: HomeUiEffect) {
        _effects.send(effect)
    }

    init {
        viewModelScope.launch {
            runCatching {
                refreshWorkspaceItems()
            }.onFailure { throwable ->
                loggerError(
                    text = "加载工作区列表失败",
                    throwable = throwable,
                    tag = TAG,
                )
                emitEffect(HomeUiEffect.ShowMessage("加载工作区列表失败: ${throwable.message}"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        workspaceRepository.close()
    }

    private fun normalizeWorkspacePath(path: String): String {
        return path
            .replace('\\', '/')
            .trimEnd('/')
            .lowercase()
    }

    companion object {
        private const val TAG = "HomeSceneVM"
    }
}
