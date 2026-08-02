package io.github.dexclub.app.scene.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dexclub.codeview.core.text.LineSelection
import io.github.dexclub.DexClubLogger
import io.github.dexclub.LogExportRequest
import io.github.dexclub.app.model.OPEN_TAB_KIND_JAVA
import io.github.dexclub.app.model.OPEN_TAB_KIND_SMALI
import io.github.dexclub.app.model.OPEN_TAB_TARGET_TYPE_CLASS
import io.github.dexclub.app.model.OpenTabUiModel
import io.github.dexclub.compat.directoriesCompat
import io.github.dexclub.core.DexEngine
import io.github.dexclub.core.editor.CodeContentRuntime
import io.github.dexclub.core.editor.CodeContentService
import io.github.dexclub.core.editor.EditorInPageSearchSource
import io.github.dexclub.core.editor.EditorInPageSearchState
import io.github.dexclub.core.editor.EditorStateRepository
import io.github.dexclub.core.editor.EditorSessionSidePanelSnapshot
import io.github.dexclub.core.editor.EditorSessionRepository
import io.github.dexclub.core.editor.OpenTabService
import io.github.dexclub.core.editor.buildEditorContentKey
import io.github.dexclub.core.navigation.JumpResolveResult
import io.github.dexclub.core.navigation.JumpTarget
import io.github.dexclub.core.navigation.NavigateRequestContext
import io.github.dexclub.core.navigation.NavigationService
import io.github.dexclub.core.navigation.PreparedNavigationDestination
import io.github.dexclub.core.search.ClassSearchService
import io.github.dexclub.core.search.StringSearchLocationResolver
import io.github.dexclub.core.search.StringSearchService
import io.github.dexclub.core.settings.AppSettingsRepository
import io.github.dexclub.core.workspace.WorkspaceIndexedClassRecord
import io.github.dexclub.core.workspace.WorkspaceInitializer
import io.github.dexclub.core.workspace.WorkspaceIndexService
import io.github.dexclub.loggerDebug
import io.github.dexclub.loggerError
import io.github.dexclub.loggerInfo
import io.github.dexclub.loggerWarn
import io.github.dexclub.node.ClassTreeNode
import io.github.dexclub.node.flatten
import io.github.dexclub.settings.AppSettings
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class WorkspaceSceneViewModel internal constructor(
    private val workspaceContext: WorkspaceSceneContext,
    private val appSettingsRepository: AppSettingsRepository,
    private val openTabService: OpenTabService,
    private val workspaceIndexService: WorkspaceIndexService,
    private val workspaceInitializer: WorkspaceInitializer,
    private val editorSessionRepository: EditorSessionRepository,
) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    private val _loadingMessage = MutableStateFlow("")

    private val _classTreeRoot = MutableStateFlow<ClassTreeNode?>(null)
    private val _expandedPaths = MutableStateFlow<Set<String>>(emptySet())
    private val _sidePanelFirstVisibleItemIndex = MutableStateFlow(0)
    private val _sidePanelFirstVisibleItemScrollOffset = MutableStateFlow(0)
    private val _sidePanelHorizontalScrollOffset = MutableStateFlow(0)
    private val _sideSelection = MutableStateFlow<WorkspaceSideSelection?>(null)
    private val _appSettings = MutableStateFlow(AppSettings())
    private val _searchDialogUiState = MutableStateFlow(WorkspaceSearchDialogUiState())

    private val _openTabs = MutableStateFlow<List<OpenTabUiModel>>(emptyList())
    private val _selectedTabId = MutableStateFlow<String?>(null)

    private val _stateLock = Any()
    private val _appSettingsSaveRequests = Channel<AppSettingsSaveRequest>(capacity = Channel.CONFLATED)
    private val _effects = Channel<WorkspaceUiEffect>(capacity = Channel.BUFFERED)

    private val _pendingNavigation = MutableStateFlow<NavigationRequest?>(null)
    private val _navigationRevealTarget = MutableStateFlow<NavigationRevealTarget?>(null)
    private var _navigationJob: Job? = null
    private var _navigationRequestId = 0L

    private var _codeLoadJob: Job? = null
    private var _sidePanelStateSaveJob: Job? = null
    private var _appSettingsLoadJob: Job? = null
    private var _searchDialogJob: Job? = null
    private var _appSettingsRevision = 0L
    private var _lastPersistedAppSettings = _appSettings.value
    private val workspaceRootDir by lazy(LazyThreadSafetyMode.NONE) {
        PlatformFile(workspaceContext.absolutePath)
    }

    private val headerState: StateFlow<WorkspaceHeaderUiState> = combine(
        _searchDialogUiState,
        _appSettings,
    ) { searchDialogUiState, appSettings ->
        WorkspaceHeaderUiState(
            workspaceName = workspaceContext.workspaceName,
            displayPath = workspaceContext.displayPath,
            searchDialogUiState = searchDialogUiState,
            settingsUiState = buildWorkspaceSettingsUiState(appSettings),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WorkspaceHeaderUiState(),
    )

    val effects = _effects.receiveAsFlow()
    private val selectedOpenTab: StateFlow<OpenTabUiModel?> = combine(
        _openTabs,
        _selectedTabId,
    ) { tabs, tabId ->
        tabs.find { it.tabId == tabId }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null,
    )
    private val codeContents: StateFlow<Map<String, String>>
        get() = codeContentRuntime.codeContents
    private val flattenedList = combine(
        _classTreeRoot,
        _expandedPaths,
    ) { root, expandedSet ->
        root?.flatten(0, expandedSet) ?: emptyList()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )
    private val sidePanelScrollState = combine(
        _sidePanelFirstVisibleItemIndex,
        _sidePanelFirstVisibleItemScrollOffset,
        _sidePanelHorizontalScrollOffset,
    ) { firstVisibleItemIndex, firstVisibleItemScrollOffset, horizontalScrollOffset ->
        WorkspaceSidePanelScrollUiState(
            firstVisibleItemIndex = firstVisibleItemIndex,
            firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
            horizontalScrollOffset = horizontalScrollOffset,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WorkspaceSidePanelScrollUiState(),
    )
    private val sidePanelState: StateFlow<WorkspaceSidePanelUiState> = combine(
        flattenedList,
        _expandedPaths,
        _sideSelection,
        sidePanelScrollState,
    ) { flattenedItems, expandedPaths, selection, scrollState ->
        buildWorkspaceSidePanelUiState(
            flattenedItems = flattenedItems,
            expandedPaths = expandedPaths,
            selection = selection,
            scrollUiState = scrollState,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WorkspaceSidePanelUiState(),
    )
    private val codePanelState: StateFlow<WorkspaceCodePanelUiState> by lazy(LazyThreadSafetyMode.NONE) {
        val paneContentState = combine(
            codeContents,
            editorStateRepository.searchHighlightRevision,
            editorStateRepository.inPageSearchRevision,
        ) { codeContents, _, _ ->
            codeContents
        }

        combine(
            _openTabs,
            selectedOpenTab,
            _navigationRevealTarget,
            paneContentState,
            _appSettings,
        ) { openTabs, selectedOpenTab, navigationRevealTarget, codeTexts, appSettings ->
            buildWorkspaceCodePanelUiState(
                openTabs = openTabs,
                selectedOpenTab = selectedOpenTab,
                navigationRevealTarget = navigationRevealTarget,
                codeContents = codeTexts,
                appSettings = appSettings,
                resolveEditorState = { tab, kind ->
                    editorStateRepository.getContentStateSnapshot(
                        tabId = tab.tabId,
                        kind = kind,
                        fallbackContent = tab.contents[kind],
                    )
                },
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            WorkspaceCodePanelUiState(),
        )
    }
    val uiState: StateFlow<WorkspaceUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            _loading,
            _loadingMessage,
            headerState,
            sidePanelState,
            codePanelState,
        ) { loading, loadingMessage, headerUiState, sidePanelUiState, codePanelUiState ->
            WorkspaceUiState(
                loading = loading,
                loadingMessage = loadingMessage,
                headerUiState = headerUiState,
                sidePanelUiState = sidePanelUiState,
                codePanelUiState = codePanelUiState,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            WorkspaceUiState(),
        )
    }

    private val dexEngine by lazy {
        DexEngine(workspaceContext.dexsAbsolutePathList)
    }

    private fun logDebug(
        text: String,
        throwable: Throwable? = null,
    ) {
        loggerDebug(
            text = text,
            throwable = throwable,
            tag = TAG,
            workspaceId = workspaceContext.workspaceId,
            workspaceName = workspaceContext.workspaceName,
        )
    }

    private fun logInfo(
        text: String,
        throwable: Throwable? = null,
    ) {
        loggerInfo(
            text = text,
            throwable = throwable,
            tag = TAG,
            workspaceId = workspaceContext.workspaceId,
            workspaceName = workspaceContext.workspaceName,
        )
    }

    private fun logWarn(
        text: String,
        throwable: Throwable? = null,
    ) {
        loggerWarn(
            text = text,
            throwable = throwable,
            tag = TAG,
            workspaceId = workspaceContext.workspaceId,
            workspaceName = workspaceContext.workspaceName,
        )
    }

    private fun logError(
        text: String,
        throwable: Throwable? = null,
    ) {
        loggerError(
            text = text,
            throwable = throwable,
            tag = TAG,
            workspaceId = workspaceContext.workspaceId,
            workspaceName = workspaceContext.workspaceName,
        )
    }

    private fun workspaceExportDir(kind: String): PlatformFile {
        return workspaceRootDir.directoriesCompat("export", kind)
    }

    private fun updateLoadingMessage(message: String) {
        _loadingMessage.value = message
    }

    private fun updateThrowableLoadingFailure(
        prefix: String,
        throwable: Throwable,
        fallbackMessage: String = "未知错误",
    ) {
        updateLoadingMessage("$prefix: ${throwable.message ?: fallbackMessage}")
    }

    private fun handleTaskFailure(
        failureText: String,
        throwable: Throwable,
        asError: Boolean,
        loadingFailurePrefix: String? = null,
    ) {
        if (asError) {
            logError(failureText, throwable)
        } else {
            logWarn(failureText, throwable)
        }

        if (loadingFailurePrefix != null) {
            updateThrowableLoadingFailure(loadingFailurePrefix, throwable)
        }
    }

    private val codeContentRuntime = CodeContentRuntime(
        codeContentService = CodeContentService(
            openTabService = openTabService,
            onWarn = { text, throwable -> logWarn(text, throwable) },
        ),
    )
    private val editorStateRepository = EditorStateRepository(
        editorSessionRepository = editorSessionRepository,
        scope = viewModelScope,
        onWarn = { text, throwable -> logWarn(text, throwable) },
    )
    private val classSearchService = ClassSearchService(
        workspaceIndexService = workspaceIndexService,
        dexEngineProvider = { dexEngine },
    )
    private val stringSearchService = StringSearchService(
        workspaceIndexService = workspaceIndexService,
        dexEngineProvider = { dexEngine },
    )
    private val stringSearchLocationResolver = StringSearchLocationResolver()
    private val navigationService = NavigationService(
        openTabService = openTabService,
        workspaceIndexService = workspaceIndexService,
    )

    init {
        _appSettingsLoadJob = viewModelScope.launch {
            loadAppSettings()
        }

        viewModelScope.launch {
            for (request in _appSettingsSaveRequests) {
                persistAppSettingsRequest(request)
            }
        }
    }

    private suspend fun printDexKitDexNum() {
        runIoCatching {
            dexEngine.readDexNum()
        }.onSuccess { dexNum ->
            if (dexNum != null) {
                logDebug("DexKit dex 数量: $dexNum")
            }
        }.onFailure { throwable ->
            logError(
                text = "读取 DexKit dex 数量失败",
                throwable = throwable,
            )
        }
    }

    private fun launchLoadingTask(
        initialMessage: String? = null,
        onFailure: (Exception) -> Unit = {},
        block: suspend () -> Unit,
    ) {
        launchHandledTask(
            showLoading = true,
            initialMessage = initialMessage,
            onFailure = { throwable ->
                when (throwable) {
                    is Exception -> onFailure(throwable)
                    else -> throw throwable
                }
            },
            block = block,
        )
    }

    private fun launchCancelableLoadingTask(
        initialMessage: String? = null,
        onFailure: (Throwable) -> Unit = {},
        block: suspend () -> Unit,
    ) {
        launchHandledTask(
            showLoading = true,
            initialMessage = initialMessage,
            onFailure = onFailure,
            block = block,
        )
    }

    private fun launchTask(
        onFailure: (Exception) -> Unit = {},
        block: suspend () -> Unit,
    ) {
        launchHandledTask(
            showLoading = false,
            onFailure = { throwable ->
                when (throwable) {
                    is Exception -> onFailure(throwable)
                    else -> throw throwable
                }
            },
            block = block,
        )
    }

    private fun launchWarnTask(
        failureText: String,
        block: suspend () -> Unit,
    ) {
        launchTask(
            onFailure = { exception ->
                handleTaskFailure(
                    failureText = failureText,
                    throwable = exception,
                    asError = false,
                )
            },
            block = block,
        )
    }

    private fun launchErrorTask(
        failureText: String,
        block: suspend () -> Unit,
    ) {
        launchTask(
            onFailure = { exception ->
                handleTaskFailure(
                    failureText = failureText,
                    throwable = exception,
                    asError = true,
                )
            },
            block = block,
        )
    }

    private fun launchWarnLoadingTask(
        initialMessage: String? = null,
        failureText: String,
        loadingFailurePrefix: String? = null,
        block: suspend () -> Unit,
    ) {
        launchLoadingTask(
            initialMessage = initialMessage,
            onFailure = { exception ->
                handleTaskFailure(
                    failureText = failureText,
                    throwable = exception,
                    asError = false,
                    loadingFailurePrefix = loadingFailurePrefix,
                )
            },
            block = block,
        )
    }

    private fun launchErrorLoadingTask(
        initialMessage: String? = null,
        failureText: String,
        loadingFailurePrefix: String? = null,
        block: suspend () -> Unit,
    ) {
        launchLoadingTask(
            initialMessage = initialMessage,
            onFailure = { exception ->
                handleTaskFailure(
                    failureText = failureText,
                    throwable = exception,
                    asError = true,
                    loadingFailurePrefix = loadingFailurePrefix,
                )
            },
            block = block,
        )
    }

    private fun launchWarnCancelableLoadingTask(
        initialMessage: String? = null,
        failureText: String,
        loadingFailurePrefix: String? = null,
        block: suspend () -> Unit,
    ) {
        launchCancelableLoadingTask(
            initialMessage = initialMessage,
            onFailure = { throwable ->
                handleTaskFailure(
                    failureText = failureText,
                    throwable = throwable,
                    asError = false,
                    loadingFailurePrefix = loadingFailurePrefix,
                )
            },
            block = block,
        )
    }

    private fun launchHandledTask(
        showLoading: Boolean,
        initialMessage: String? = null,
        onFailure: (Throwable) -> Unit = {},
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            if (showLoading) {
                _loading.value = true
                if (initialMessage != null) {
                    updateLoadingMessage(initialMessage)
                }
            }
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                onFailure(throwable)
            } finally {
                if (showLoading) {
                    _loading.value = false
                }
            }
        }
    }

    private suspend fun <T> runTabSessionMutation(
        preferredTabId: (T) -> String?,
        mutation: suspend () -> T,
    ): T {
        return runIo {
            val result = mutation()
            refreshOpenTabsInternal(preferredTabId(result))
            result
        }
    }

    private suspend fun <T> runIo(
        block: suspend () -> T,
    ): T {
        return withContext(Dispatchers.IO) {
            block()
        }
    }

    private suspend fun <T> runIoCatching(
        block: suspend () -> T,
    ): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(block())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Result.failure(throwable)
            }
        }
    }

    private fun runBlockingCleanupStep(
        failureText: String,
        block: suspend () -> Unit,
    ) {
        runCatching {
            runBlocking {
                block()
            }
        }.onFailure { throwable ->
            logWarn(
                text = failureText,
                throwable = throwable,
            )
        }
    }

    private fun runCleanupStep(
        failureText: String,
        block: () -> Unit,
    ) {
        runCatching {
            block()
        }.onFailure { throwable ->
            logWarn(
                text = failureText,
                throwable = throwable,
            )
        }
    }

    private suspend fun saveAppSettingsOnClear() {
        val settingsToSave = if (_appSettingsRevision == 0L) {
            runIo {
                appSettingsRepository.load()
            }
        } else {
            awaitAppSettingsLoaded()
            _appSettings.value
        }

        runIo {
            appSettingsRepository.save(settingsToSave)
        }
    }

    private fun selectOpenTabById(tabId: String) {
        _openTabs.value.find { it.tabId == tabId }?.let(::onToggleOpenTab)
    }

    override fun onCleared() {
        _sidePanelStateSaveJob?.cancel()
        codeContentRuntime.clearActiveHighlighters()
        editorStateRepository.close()
        runBlockingCleanupStep("关闭前保存侧边栏状态失败") {
            persistSidePanelStateToDatabase()
        }
        runBlockingCleanupStep("关闭前保存当前标签页失败") {
            persistSelectedTabToDatabase()
        }
        runBlockingCleanupStep("关闭前保存设置失败") {
            saveAppSettingsOnClear()
        }
        runCleanupStep("关闭 DexKit 失败") {
            dexEngine.close()
        }
        super.onCleared()
        workspaceIndexService.close()
        editorSessionRepository.close()
    }

    fun initialize() {
        launchErrorLoadingTask(
            initialMessage = "初始化中..",
            failureText = "项目初始化失败",
            loadingFailurePrefix = "加载失败",
        ) {
            val currentSelectedTabId = _selectedTabId.value
            val bootstrap = runIo {
                workspaceInitializer.bootstrap(
                    expectedIndexedClassCount = dexEngine.classCount(),
                    indexedClassesProvider = dexEngine::workspaceIndexEntries,
                    currentSelectedTabId = currentSelectedTabId,
                    onProgress = { progress ->
                        updateLoadingMessage(progress)
                    },
                    onClassIndexMismatch = { actual, expected ->
                        logInfo("类索引不完整，触发重建: expected=$expected, actual=$actual")
                    },
                    onWarmUpDexKit = {
                        printDexKitDexNum()
                    },
                )
            }

            restoreSidePanelState(bootstrap.sidePanelSnapshot)
            _classTreeRoot.value = bootstrap.classIndexState.classTreeRoot
            refreshOpenTabsInternal(bootstrap.selectedTabId)

            val firstTabId = _selectedTabId.value
            if (firstTabId != null) {
                loadParsedContentsForTab(firstTabId)
            }
        }
    }

    private suspend fun refreshOpenTabsInternal(preferredTabId: String?) {
        val snapshot = openTabService.loadSessionSnapshot(
            preferredTabId = preferredTabId,
            currentSelectedTabId = _selectedTabId.value,
        )
        snapshot.contentsByTabId.forEach { (tabId, contents) ->
            editorStateRepository.hydrateContents(tabId, contents)
        }
        _openTabs.value = snapshot.tabs
        _selectedTabId.value = snapshot.selectedTabId
    }

    private suspend fun persistSelectedTabToDatabase() {
        val selectedTabId = _selectedTabId.value ?: return
        persistTabViewed(
            tabId = selectedTabId,
            viewedAt = System.currentTimeMillis(),
        )
    }

    private fun persistTabViewedAsync(tabId: String) {
        val viewedAt = System.currentTimeMillis()
        launchWarnTask(
            failureText = "保存当前标签浏览时间失败",
        ) {
            persistTabViewed(
                tabId = tabId,
                viewedAt = viewedAt,
            )
        }
    }

    private suspend fun persistTabViewed(tabId: String, viewedAt: Long) {
        runIo {
            openTabService.updateTabLastViewedAt(
                tabId = tabId,
                lastViewedAt = viewedAt,
            )
        }
    }

    private suspend fun exportCodePathForClass(
        cls: WorkspaceIndexedClassRecord,
        kind: String,
    ): String {
        awaitAppSettingsLoaded()

        return when (kind) {
            OPEN_TAB_KIND_SMALI -> {
                updateLoadingMessage("export single smali: ${cls.signature}")
                val smaliOutput = workspaceExportDir("smali")
                val smaliOutFile = PlatformFile(smaliOutput, cls.className)
                dexEngine.exportSingleSmali(
                    smaliUnicodeDecode = _appSettings.value.smaliUnicodeDecode,
                    className = cls.signature,
                    dexPath = cls.dexAbsolutePath,
                    outputPath = smaliOutFile.absolutePath(),
                ).also {
                    logDebug("导出 Smali: $it")
                }
            }

            OPEN_TAB_KIND_JAVA -> {
                updateLoadingMessage("export single dex: ${cls.signature}")
                val dexOutput = workspaceExportDir("dex")
                val dexOutFile = PlatformFile(dexOutput, cls.className)
                dexEngine.exportSingleDex(
                    className = cls.signature,
                    dexPath = cls.dexAbsolutePath,
                    outputPath = dexOutFile.absolutePath(),
                ).also {
                    logDebug("导出 Dex: $it")
                }

                updateLoadingMessage("Dex to Java: ${cls.signature}")
                val clsOutputDir = workspaceExportDir("java")
                val clsOutput = PlatformFile(clsOutputDir, "${cls.className}.java")
                dexEngine.exportSingleJavaSource(
                    escapeUnicode = !_appSettings.value.javaUnicodeDecode,
                    className = cls.signature,
                    dexPath = cls.dexAbsolutePath,
                    outputPath = clsOutput.absolutePath(),
                ).also {
                    logDebug("导出 Java: $it")
                }
            }

            else -> throw IllegalArgumentException("Unsupported kind: $kind")
        }
    }

    fun onOpenClassByName(className: String) {
        val normalizedClassName = navigationService.normalizeTargetClassName(className)
        if (normalizedClassName.isEmpty()) {
            updateLoadingMessage("打开失败: 目标类名为空")
            return
        }

        openClassTab(
            className = normalizedClassName,
            classLoader = {
                // 仅允许全类名精确匹配。无包名类（如 Main）同样会以精确 name 命中。
                workspaceIndexService.findByName(normalizedClassName)
                    ?: throw IllegalStateException("无法打开目标类：$normalizedClassName")
            },
        )
    }

    fun onOpenClassSearchResult(result: WorkspaceClassSearchResult) {
        launchWarnCancelableLoadingTask(
            initialMessage = "正在打开类搜索结果...",
            failureText = "类搜索结果打开失败: class=${result.className}",
            loadingFailurePrefix = "打开失败",
        ) {
            val destination = withContext(Dispatchers.IO) {
                navigationService.prepareSearchDestination(
                    className = result.className,
                    preferredKind = OPEN_TAB_KIND_SMALI,
                    exportCodePathForClass = ::exportCodePathForClass,
                )
            } ?: throw IllegalStateException("无法打开目标类：${result.className}")

            val destinationTab = loadPreparedDestination(destination)
                ?: throw IllegalStateException("目标标签页状态未同步")
            val targetKinds = destinationTab.resolveNavigationTargetKinds(destination.kind)
            val activeSearchKinds = mutableSetOf<String>()

            for (kind in targetKinds) {
                val lines = loadedCodeLines(
                    tabId = destination.tabId,
                    kind = kind,
                )
                if (lines.isEmpty()) {
                    continue
                }

                val query = resolveDexKitClassSearchQuery(
                    tab = destinationTab,
                    result = result,
                    kind = kind,
                )
                val matches = resolveInPageSearchMatches(
                    lines = lines,
                    query = query,
                )

                seedInPageSearchState(
                    tabId = destination.tabId,
                    kind = kind,
                    queryText = query,
                    matchQuery = query,
                    source = EditorInPageSearchSource.DexKitClass,
                    activeMatchIndex = 0,
                    isVisible = false,
                    requestFocus = false,
                )
                if (matches.isNotEmpty()) {
                    val activeMatch = matches.first()
                    updateCursorSelection(
                        tabId = destination.tabId,
                        kind = kind,
                        cursorLine = activeMatch.cursor.line,
                        cursorOffset = activeMatch.cursor.offset,
                        selection = activeMatch.selection,
                    )
                }
                activeSearchKinds += kind
            }

            focusPreparedDestination(
                destination = destination,
                destinationTab = destinationTab,
                revealPlan = destinationTab.resolveNavigationRevealPlan(
                    preferredKind = if (destination.kind in activeSearchKinds) {
                        destination.kind
                    } else {
                        activeSearchKinds.firstOrNull() ?: destination.kind
                    },
                    fallbackPaneIndex = destination.paneIndex,
                    revealAllKindsInMixedMode = true,
                ),
                token = nextNavigationRequestId(),
            )
            syncSidePanelToClass(result.className)
        }
    }

    private fun openClassTab(
        className: String,
        classLoader: suspend () -> WorkspaceIndexedClassRecord,
    ) {
        val existing = _openTabs.value.find { it.targetType == OPEN_TAB_TARGET_TYPE_CLASS && it.targetKey == className }
        if (existing != null) {
            onToggleOpenTab(existing)
            return
        }

        launchErrorLoadingTask(
            failureText = "打开类失败",
            loadingFailurePrefix = "打开失败",
        ) {
            val openedTabId = runTabSessionMutation(
                preferredTabId = { tabId -> tabId },
            ) {
                val cls = classLoader()
                openTabService.openClassTab(
                    cls = cls,
                    exportCodePathForClass = ::exportCodePathForClass,
                ).tabId
            }

            selectOpenTabById(openedTabId)
        }
    }

    fun onSideNodeClick(node: ClassTreeNode) {
        updateSideSelection(node.toWorkspaceSideSelection())

        when (node) {
            is ClassTreeNode.PackageNode -> {
                onToggleExpand(node.fullPath)
            }

            is ClassTreeNode.ClassNode -> onOpenClassByName(node.className)
        }
    }

    private fun updateSideSelection(selection: WorkspaceSideSelection) {
        if (_sideSelection.value == selection) return
        _sideSelection.value = selection
        scheduleSidePanelStateSave()
    }

    private fun syncSidePanelToClass(className: String) {
        val normalizedClassName = navigationService.normalizeTargetClassName(className)
        if (normalizedClassName.isEmpty()) return

        val packagePaths = packagePathsForClass(normalizedClassName)
        val selection = WorkspaceSideSelection.Class(normalizedClassName)
        val expandedPaths = _expandedPaths.value + packagePaths
        _expandedPaths.value = expandedPaths
        _sideSelection.value = selection

        val targetItems = _classTreeRoot.value?.flatten(0, expandedPaths).orEmpty()
        val targetIndex = targetItems.indexOfFirst { flattenedNode ->
            selection.matches(flattenedNode.node)
        }
        if (targetIndex >= 0) {
            _sidePanelFirstVisibleItemIndex.value = targetIndex
            _sidePanelFirstVisibleItemScrollOffset.value = 0
            _sidePanelHorizontalScrollOffset.value = 0
        }

        scheduleSidePanelStateSave()
    }

    private fun packagePathsForClass(className: String): Set<String> {
        val parts = className.replace('/', '.').split('.')
        if (parts.size <= 1) return emptySet()

        val packagePaths = linkedSetOf<String>()
        val currentParts = mutableListOf<String>()
        for (part in parts.dropLast(1)) {
            if (part.isEmpty()) continue
            currentParts += part
            packagePaths += currentParts.joinToString(".")
        }
        return packagePaths
    }

    internal fun resetSearchDialogState() {
        _searchDialogJob?.cancel()
        _searchDialogJob = null
        _searchDialogUiState.value = WorkspaceSearchDialogUiState()
    }

    internal fun selectSearchDialogTab(tab: WorkspaceSearchTab) {
        _searchDialogUiState.update { current ->
            current.selectTab(tab)
        }
    }

    internal fun updateSearchDialogQuery(query: String) {
        _searchDialogUiState.update { current ->
            current.updateActiveQuery(query)
        }
    }

    internal fun submitSearchDialog() {
        val currentState = _searchDialogUiState.value
        if (currentState.searchingTab != null) return

        val submittedTab = currentState.currentTab
        val normalizedQuery = currentState.activeQuery.trim()
        if (normalizedQuery.isEmpty()) {
            _searchDialogUiState.update { state ->
                state.withEmptyQueryError(submittedTab)
            }
            return
        }

        _searchDialogUiState.update { state ->
            state.markSearchStarted(submittedTab)
        }

        val searchJob = viewModelScope.launch {
            try {
                when (submittedTab) {
                    WorkspaceSearchTab.ClassName -> {
                        val response = searchClassesByName(normalizedQuery)
                        _searchDialogUiState.update { state ->
                            state.withClassSearchSuccess(response)
                        }
                    }

                    WorkspaceSearchTab.StringLiteral -> {
                        val response = searchClassesByString(normalizedQuery)
                        _searchDialogUiState.update { state ->
                            state.withStringSearchSuccess(
                                submittedQuery = normalizedQuery,
                                response = response,
                            )
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                _searchDialogUiState.update { state ->
                    state.withSearchFailure(
                        tab = submittedTab,
                        message = throwable.message ?: "搜索失败",
                    )
                }
            }
        }
        searchJob.invokeOnCompletion {
            if (_searchDialogJob == searchJob) {
                _searchDialogJob = null
            }
        }
        _searchDialogJob = searchJob
    }

    private suspend fun <T> runWorkspaceSearch(
        keyword: String,
        successLogLabel: String,
        failureLogLabel: String,
        search: suspend (String) -> List<T>,
    ): WorkspaceSearchResponse<T> {
        val normalizedKeyword = keyword.trim()
        return withContext(Dispatchers.IO) {
            try {
                val items = search(normalizedKeyword)
                logDebug("$successLogLabel: keyword=$normalizedKeyword, result=${items.size}")
                WorkspaceSearchResponse(items = items)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                logWarn(
                    text = "$failureLogLabel: $normalizedKeyword",
                    throwable = throwable,
                )
                throw throwable
            }
        }
    }

    private suspend fun searchClassesByName(
        keyword: String,
    ): WorkspaceSearchResponse<WorkspaceClassSearchResult> {
        return runWorkspaceSearch(
            keyword = keyword,
            successLogLabel = "DexKit 类名搜索完成",
            failureLogLabel = "DexKit 类名搜索失败",
        ) { normalizedKeyword ->
            classSearchService.searchByName(
                keyword = normalizedKeyword,
                sortByRelevance = true,
            ).map { it.toWorkspaceClassSearchResult() }
        }
    }

    private suspend fun searchClassesByString(
        keyword: String,
    ): WorkspaceSearchResponse<WorkspaceStringSearchResult> {
        return runWorkspaceSearch(
            keyword = keyword,
            successLogLabel = "DexKit 字符串搜索完成",
            failureLogLabel = "DexKit 字符串搜索失败",
        ) { normalizedKeyword ->
            stringSearchService.searchByString(
                keyword = normalizedKeyword,
            ).map { it.toWorkspaceStringSearchResult() }
        }
    }

    fun onOpenStringSearchResult(result: WorkspaceStringSearchResult) {
        val query = _searchDialogUiState.value.stringSearch.submittedQuery
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            updateLoadingMessage("定位失败：搜索关键词为空")
            return
        }

        launchWarnCancelableLoadingTask(
            initialMessage = "正在定位字符串...",
            failureText = "字符串搜索结果定位失败: query=$normalizedQuery, class=${result.className}",
            loadingFailurePrefix = "定位失败",
        ) {
            val destination = withContext(Dispatchers.IO) {
                navigationService.prepareSearchDestination(
                    className = result.className,
                    preferredKind = OPEN_TAB_KIND_SMALI,
                    exportCodePathForClass = ::exportCodePathForClass,
                )
            } ?: throw IllegalStateException("无法打开目标类：${result.className}")

            val destinationTab = loadPreparedDestination(destination)
                ?: throw IllegalStateException("目标标签页状态未同步")
            val targetKinds = destinationTab.resolveNavigationTargetKinds(destination.kind)
            val resolvedKinds = mutableSetOf<String>()

            for (kind in targetKinds) {
                val activeLines = loadedCodeLines(
                    tabId = destination.tabId,
                    kind = kind,
                )
                if (activeLines.isEmpty()) {
                    continue
                }

                val location = stringSearchLocationResolver.resolveLocation(
                    lines = activeLines,
                    query = normalizedQuery,
                    targetKind = kind,
                    methodDescriptor = result.methodDescriptor,
                    methodName = result.methodName,
                ) ?: continue
                val matches = resolveInPageSearchMatches(
                    lines = activeLines,
                    query = normalizedQuery,
                )
                val activeMatchIndex = findInPageSearchMatchIndex(
                    matches = matches,
                    selection = location.selection,
                ) ?: 0

                updateCursorSelection(
                    tabId = destination.tabId,
                    kind = kind,
                    cursorLine = location.line,
                    cursorOffset = location.selection.endOffset,
                    selection = location.selection,
                )
                seedInPageSearchState(
                    tabId = destination.tabId,
                    kind = kind,
                    queryText = normalizedQuery,
                    matchQuery = normalizedQuery,
                    source = EditorInPageSearchSource.DexKitString,
                    activeMatchIndex = activeMatchIndex,
                    isVisible = false,
                    requestFocus = false,
                )
                resolvedKinds += kind
            }

            if (resolvedKinds.isEmpty()) {
                throw IllegalStateException("未能在目标代码中定位到 \"$normalizedQuery\"")
            }

            val revealKind = if (destination.kind in resolvedKinds) {
                destination.kind
            } else {
                resolvedKinds.first()
            }
            focusPreparedDestination(
                destination = destination,
                destinationTab = destinationTab,
                revealPlan = destinationTab.resolveNavigationRevealPlan(
                    preferredKind = revealKind,
                    fallbackPaneIndex = destination.paneIndex,
                    revealAllKindsInMixedMode = true,
                ),
                token = nextNavigationRequestId(),
            )
            syncSidePanelToClass(result.className)
        }
    }

    fun onToggleOpenTab(tab: OpenTabUiModel) {
        _selectedTabId.value = tab.tabId
        persistTabViewedAsync(tab.tabId)

        _codeLoadJob?.cancel()
        _codeLoadJob = viewModelScope.launch {
            loadParsedContentsForTab(tab.tabId)
        }
    }

    private suspend fun loadParsedContentsForTab(tabId: String) {
        try {
            codeContentRuntime.loadTabContents(
                tabId = tabId,
                exportCodePathForClass = ::exportCodePathForClass,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            logError(
                text = "加载代码内容失败",
                throwable = e,
            )
        }
    }

    private fun loadedCodeLines(
        tabId: String,
        kind: String,
    ): List<String> {
        return codeContents.value[buildEditorContentKey(tabId, kind)]
            ?.lines()
            .orEmpty()
    }

    private fun resolveDexKitClassSearchQuery(
        tab: OpenTabUiModel,
        result: WorkspaceClassSearchResult,
        kind: String,
    ): String {
        return when (kind) {
            OPEN_TAB_KIND_SMALI -> result.descriptor.ifBlank { result.className }
            OPEN_TAB_KIND_JAVA -> tab.title.ifBlank { result.className.substringAfterLast('.') }
            else -> result.className
        }
    }

    private fun seedInPageSearchState(
        tabId: String,
        kind: String,
        queryText: String,
        matchQuery: String,
        source: EditorInPageSearchSource,
        activeMatchIndex: Int,
        isVisible: Boolean = true,
        requestFocus: Boolean,
    ) {
        val currentState = editorStateRepository.getInPageSearchState(tabId, kind)
        editorStateRepository.updateInPageSearchState(
            tabId = tabId,
            kind = kind,
            state = currentState.copy(
                queryText = queryText,
                matchQuery = matchQuery,
                source = source,
                activeMatchIndex = activeMatchIndex,
                isVisible = isVisible,
                requestFocusToken = if (requestFocus) {
                    currentState.requestFocusToken + 1
                } else {
                    currentState.requestFocusToken
                },
            ),
        )
    }

    private suspend fun loadPreparedDestination(
        destination: PreparedNavigationDestination,
        requestId: Long? = null,
    ): OpenTabUiModel? {
        if (!canContinueNavigationRequest(requestId)) return null

        refreshOpenTabsInternal(destination.tabId)
        _selectedTabId.value = destination.tabId
        loadParsedContentsForTab(destination.tabId)
        if (!canContinueNavigationRequest(requestId)) return null

        return _openTabs.value.firstOrNull { it.tabId == destination.tabId }
    }

    private fun focusPreparedDestination(
        destination: PreparedNavigationDestination,
        destinationTab: OpenTabUiModel,
        revealPlan: WorkspaceNavigationRevealPlan,
        token: Long,
    ) {
        _selectedTabId.value = destination.tabId
        activatePane(
            tab = destinationTab,
            paneIndex = revealPlan.paneIndex,
            kind = revealPlan.activeKind,
        )
        _navigationRevealTarget.value = NavigationRevealTarget(
            tabId = destination.tabId,
            kind = revealPlan.revealKind,
            token = token,
        )
    }

    fun onCodeViewportChanged(
        tabId: String,
        kind: String,
        firstVisibleLine: Int,
        lastVisibleLine: Int,
    ) {
        // no-op: highlight pipeline removed in P8
    }

    fun onCloseOpenTab(tab: OpenTabUiModel) {
        closeOpenTabs(
            tabIds = setOf(tab.tabId),
            preferredTabId = null,
        )
    }

    fun closeAllOpenTabs() {
        closeOpenTabs(
            tabIds = _openTabs.value.map { it.tabId }.toSet(),
            preferredTabId = null,
        )
    }

    fun closeOtherOpenTabs(anchorTab: OpenTabUiModel) {
        if (_openTabs.value.none { it.tabId == anchorTab.tabId }) return
        closeOpenTabs(
            tabIds = _openTabs.value
                .asSequence()
                .map { it.tabId }
                .filter { it != anchorTab.tabId }
                .toSet(),
            preferredTabId = anchorTab.tabId,
        )
    }

    fun closeOpenTabsToRight(anchorTab: OpenTabUiModel) {
        val currentTabs = _openTabs.value
        val anchorIndex = currentTabs.indexOfFirst { it.tabId == anchorTab.tabId }
        if (anchorIndex < 0) return

        closeOpenTabs(
            tabIds = currentTabs
                .drop(anchorIndex + 1)
                .map { it.tabId }
                .toSet(),
            preferredTabId = anchorTab.tabId,
        )
    }

    fun closeOpenTabsToLeft(anchorTab: OpenTabUiModel) {
        val currentTabs = _openTabs.value
        val anchorIndex = currentTabs.indexOfFirst { it.tabId == anchorTab.tabId }
        if (anchorIndex < 0) return

        closeOpenTabs(
            tabIds = currentTabs
                .take(anchorIndex)
                .map { it.tabId }
                .toSet(),
            preferredTabId = anchorTab.tabId,
        )
    }

    private fun closeOpenTabs(
        tabIds: Set<String>,
        preferredTabId: String?,
    ) {
        if (tabIds.isEmpty()) return
        val existingTabIds = _openTabs.value
            .asSequence()
            .map { it.tabId }
            .filter { it in tabIds }
            .toSet()
        if (existingTabIds.isEmpty()) return

        launchErrorTask(
            failureText = "关闭标签页失败",
        ) {
            val previousSelectedTabId = _selectedTabId.value
            val effectivePreferredTabId = preferredTabId?.takeUnless { it in existingTabIds }
            runTabSessionMutation(
                preferredTabId = {
                    effectivePreferredTabId ?: previousSelectedTabId?.takeUnless { it in existingTabIds }
                },
            ) {
                openTabService.deleteTabsByIds(existingTabIds.toList())
            }

            clearTabRuntimeStates(existingTabIds)

            val selectedTabId = _selectedTabId.value
            if (selectedTabId == null) {
                codeContentRuntime.clearActiveHighlighters()
            } else if (selectedTabId != previousSelectedTabId || previousSelectedTabId in existingTabIds) {
                loadParsedContentsForTab(selectedTabId)
            }
        }
    }

    private fun clearTabRuntimeStates(tabIds: Set<String>) {
        if (tabIds.isEmpty()) return

        editorStateRepository.clearTabStates(tabIds)
        codeContentRuntime.clearTabRuntimeStates(tabIds)
    }

    fun updateScrollOffset(tabId: String, kind: String, offsetY: Int, offsetX: Int) {
        editorStateRepository.updateScrollOffset(tabId, kind, offsetY, offsetX)
    }

    fun updateCursorSelection(
        tabId: String,
        kind: String,
        cursorLine: Int,
        cursorOffset: Int,
        selection: LineSelection?,
    ) {
        editorStateRepository.updateCursorSelection(
            tabId = tabId,
            kind = kind,
            cursorLine = cursorLine,
            cursorOffset = cursorOffset,
            selection = selection,
        )
    }

    fun updateSearchHighlight(
        tabId: String,
        kind: String,
        highlight: LineSelection?,
    ) {
        editorStateRepository.updateSearchHighlight(tabId, kind, highlight)
    }

    fun updateInPageSearchState(
        tabId: String,
        kind: String,
        state: EditorInPageSearchState,
    ) {
        syncVisibleInPageSearchWithinTab(
            tabId = tabId,
            activeKind = kind,
            isVisible = state.isVisible,
        )
        editorStateRepository.updateInPageSearchState(tabId, kind, state)
    }

    fun requestInPageSearchForSelectedPane() {
        val selectedTab = _openTabs.value.firstOrNull { tab ->
            tab.tabId == _selectedTabId.value
        } ?: return
        val activeKind = selectedTab.activeKind
        val currentState = editorStateRepository.getInPageSearchState(selectedTab.tabId, activeKind)
        syncVisibleInPageSearchWithinTab(
            tabId = selectedTab.tabId,
            activeKind = activeKind,
            isVisible = true,
        )
        editorStateRepository.updateInPageSearchState(
            tabId = selectedTab.tabId,
            kind = activeKind,
            state = currentState.copy(
                isVisible = true,
                source = if (currentState.queryText.isEmpty() && currentState.matchQuery.isEmpty()) {
                    EditorInPageSearchSource.Manual
                } else {
                    currentState.source
                },
                requestFocusToken = currentState.requestFocusToken + 1,
            ),
        )
    }

    fun clearSearchHighlightsForTab(tabId: String) {
        editorStateRepository.clearSearchHighlightsForTab(tabId)
    }

    private fun syncVisibleInPageSearchWithinTab(
        tabId: String,
        activeKind: String,
        isVisible: Boolean,
    ) {
        if (!isVisible) return
        val targetTab = _openTabs.value.firstOrNull { tab -> tab.tabId == tabId } ?: return
        val allKinds = (targetTab.contents.keys + targetTab.requiredKinds)
            .distinct()

        allKinds.forEach { kind ->
            if (kind == activeKind) return@forEach
            val state = editorStateRepository.getInPageSearchState(tabId, kind)
            if (!state.isVisible) return@forEach
            editorStateRepository.updateInPageSearchState(
                tabId = tabId,
                kind = kind,
                state = state.copy(isVisible = false),
            )
        }
    }

    fun activatePane(tab: OpenTabUiModel, paneIndex: Int, kind: String) {
        if (tab.activePaneIndex == paneIndex && tab.activeKind == kind) return

        launchWarnTask(
            failureText = "更新激活 Pane 失败",
        ) {
            runTabSessionMutation(
                preferredTabId = { tab.tabId },
            ) {
                openTabService.updateActivePane(
                    tabId = tab.tabId,
                    paneIndex = paneIndex,
                    kind = kind,
                )
            }
        }
    }

    // 三态切换：single(primary) -> single(secondary) -> mixed(primary+secondary) -> single(primary)
    fun toggleCodeView(tab: OpenTabUiModel) {
        launchWarnLoadingTask(
            failureText = "切换视图模式失败",
        ) {
            runTabSessionMutation(
                preferredTabId = { tab.tabId },
            ) {
                openTabService.toggleCodeView(
                    tabId = tab.tabId,
                    exportCodePathForClass = ::exportCodePathForClass,
                )
            }

            selectOpenTabById(tab.tabId)
        }
    }

    fun prioritizeKind(tab: OpenTabUiModel, preferredKind: String) {
        launchWarnLoadingTask(
            failureText = "更新 Kind 优先级失败",
        ) {
            runTabSessionMutation(
                preferredTabId = { tab.tabId },
            ) {
                openTabService.prioritizeKind(
                    tabId = tab.tabId,
                    preferredKind = preferredKind,
                    exportCodePathForClass = ::exportCodePathForClass,
                )
            }

            selectOpenTabById(tab.tabId)
        }
    }

    fun onToggleExpand(fullPath: String) {
        _expandedPaths.update { current ->
            if (fullPath in current) current - fullPath else current + fullPath
        }
        scheduleSidePanelStateSave()
    }

    fun updateSidePanelScrollOffsets(
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int,
        horizontalScrollOffset: Int,
    ) {
        val normalizedIndex = firstVisibleItemIndex.coerceAtLeast(0)
        val normalizedOffset = firstVisibleItemScrollOffset.coerceAtLeast(0)
        val normalizedHorizontal = horizontalScrollOffset.coerceAtLeast(0)

        if (
            _sidePanelFirstVisibleItemIndex.value == normalizedIndex &&
            _sidePanelFirstVisibleItemScrollOffset.value == normalizedOffset &&
            _sidePanelHorizontalScrollOffset.value == normalizedHorizontal
        ) {
            return
        }

        _sidePanelFirstVisibleItemIndex.value = normalizedIndex
        _sidePanelFirstVisibleItemScrollOffset.value = normalizedOffset
        _sidePanelHorizontalScrollOffset.value = normalizedHorizontal
        scheduleSidePanelStateSave()
    }

    fun updateSmaliUnicodeDecode(enabled: Boolean) {
        val previousSettings = _appSettings.value
        if (previousSettings.smaliUnicodeDecode == enabled) {
            return
        }

        val updatedSettings = previousSettings.copy(smaliUnicodeDecode = enabled)
        _appSettings.value = updatedSettings

        _appSettingsRevision++
        val revision = _appSettingsRevision
        val sendResult = _appSettingsSaveRequests.trySend(
            AppSettingsSaveRequest(
                revision = revision,
                settings = updatedSettings,
            ),
        )
        if (sendResult.isFailure) {
            _appSettings.value = previousSettings
            logWarn(
                text = "提交设置保存任务失败",
                throwable = sendResult.exceptionOrNull(),
            )
            viewModelScope.launch {
                emitMessageEffect("提交设置保存任务失败")
            }
        }
    }

    fun updateJavaUnicodeDecode(enabled: Boolean) {
        val previousSettings = _appSettings.value
        if (previousSettings.javaUnicodeDecode == enabled) {
            return
        }

        val updatedSettings = previousSettings.copy(javaUnicodeDecode = enabled)
        _appSettings.value = updatedSettings

        _appSettingsRevision++
        val revision = _appSettingsRevision
        val sendResult = _appSettingsSaveRequests.trySend(
            AppSettingsSaveRequest(
                revision = revision,
                settings = updatedSettings,
            ),
        )
        if (sendResult.isFailure) {
            _appSettings.value = previousSettings
            logWarn(
                text = "提交设置保存任务失败",
                throwable = sendResult.exceptionOrNull(),
            )
            viewModelScope.launch {
                emitMessageEffect("提交设置保存任务失败")
            }
        }
    }

    fun updateCodeScrollPastEnd(lines: Int) {
        val normalizedLines = lines.coerceAtLeast(0)
        val previousSettings = _appSettings.value
        if (previousSettings.codeScrollPastEnd == normalizedLines) {
            return
        }

        val updatedSettings = previousSettings.copy(codeScrollPastEnd = normalizedLines)
        _appSettings.value = updatedSettings

        _appSettingsRevision++
        val revision = _appSettingsRevision
        val sendResult = _appSettingsSaveRequests.trySend(
            AppSettingsSaveRequest(
                revision = revision,
                settings = updatedSettings,
            ),
        )
        if (sendResult.isFailure) {
            _appSettings.value = previousSettings
            logWarn(
                text = "提交设置保存任务失败",
                throwable = sendResult.exceptionOrNull(),
            )
            viewModelScope.launch {
                emitMessageEffect("提交设置保存任务失败")
            }
        }
    }

    private suspend fun persistAppSettingsRequest(request: AppSettingsSaveRequest) {
        awaitAppSettingsLoaded()

        val result = runIoCatching {
            appSettingsRepository.save(request.settings)
        }

        result.onSuccess {
            _lastPersistedAppSettings = request.settings
        }
        result.onFailure { throwable ->
            val isLatestRequest = request.revision == _appSettingsRevision
            if (isLatestRequest) {
                _appSettings.value = _lastPersistedAppSettings
            }

            logWarn(
                text = "保存设置失败",
                throwable = throwable,
            )

            if (isLatestRequest) {
                emitMessageEffect(throwable.message ?: "保存设置失败")
            }
        }
    }

    private suspend fun loadAppSettings() {
        val loadedSettings = runIo {
            appSettingsRepository.load()
        }

        _lastPersistedAppSettings = loadedSettings
        if (_appSettingsRevision == 0L) {
            _appSettings.value = loadedSettings
        }
    }

    private suspend fun awaitAppSettingsLoaded() {
        _appSettingsLoadJob?.join()
    }

    private data class AppSettingsSaveRequest(
        val revision: Long,
        val settings: AppSettings,
    )

    fun navigateToDefinition(context: NavigateRequestContext) {
        val request = buildNavigationRequest(context) ?: return
        _pendingNavigation.value = request
        _navigationJob?.cancel()
        _navigationJob = viewModelScope.launch {
            val execution = executeNavigationRequestWithTimeout(request)

            // 只接受最新请求，避免旧请求覆盖新请求
            if (!isNavigationRequestCurrent(request.id)) return@launch

            handleNavigationExecution(request, execution)
            clearPendingNavigationRequest(request.id)
        }
    }

    private fun buildNavigationRequest(context: NavigateRequestContext): NavigationRequest? {
        if (!DEFINITION_NAVIGATION_ENABLED) {
            logDebug("定义跳转已禁用")
            return null
        }

        if (!context.semanticNode.hasNavigationIdentity()) {
            logDebug(context.navigationMissDebugMessage())
            return null
        }

        return NavigationRequest(
            id = nextNavigationRequestId(),
            context = context,
        )
    }

    private suspend fun executeNavigationRequestWithTimeout(
        request: NavigationRequest,
    ): NavigationExecution {
        return withTimeoutOrNull(NAVIGATION_TIMEOUT_MS) {
            runNavigationRequest(request)
        } ?: NavigationExecution.Timeout
    }

    private suspend fun handleNavigationExecution(
        request: NavigationRequest,
        execution: NavigationExecution,
    ) {
        when (execution) {
            is NavigationExecution.Resolved -> handleResolvedNavigationExecution(
                context = request.context,
                execution = execution,
            )

            is NavigationExecution.Finished -> handleFinishedNavigationExecution(execution.result)
            NavigationExecution.Timeout -> handleNavigationTimeout()
            NavigationExecution.Stale -> logWarn("导航请求过期，忽略旧请求: id=${request.id}")
        }
    }

    private suspend fun handleResolvedNavigationExecution(
        context: NavigateRequestContext,
        execution: NavigationExecution.Resolved,
    ) {
        logInfo(
            "导航解析成功: tabId=${context.tabId}, pane=${context.paneIndex}, kind=${context.activeKind}, " +
                    "target=${execution.target.targetClassName}:${execution.target.targetLine}:${execution.target.targetOffset}, " +
                    "reason=${execution.target.reason}",
        )
        if (execution.applied) {
            return
        }

        emitMessageEffect("跳转失败，无法打开目标: ${execution.target.targetClassName}")
        logWarn("目标已解析但当前阶段无法执行落点: class=${execution.target.targetClassName}, kind=${execution.target.targetKind}")
    }

    private suspend fun handleFinishedNavigationExecution(result: JumpResolveResult) {
        when (result) {
            is JumpResolveResult.NotFound -> {
                emitMessageEffect("未找到定义：${result.reason}")
                logWarn("导航未命中: ${result.reason}")
            }

            is JumpResolveResult.Unsupported -> {
                emitMessageEffect("暂不支持：${result.reason}")
                logWarn("导航不支持: ${result.reason}")
            }

            is JumpResolveResult.Error -> {
                emitMessageEffect("跳转失败：${result.reason}")
                logError("导航失败: ${result.reason}")
            }

            is JumpResolveResult.Resolved -> {
                emitMessageEffect("跳转失败：内部状态异常")
                logError("导航状态异常: resolved 未进入执行阶段")
            }
        }
    }

    private suspend fun handleNavigationTimeout() {
        val reason = "导航超时(${NAVIGATION_TIMEOUT_MS}ms)"
        emitMessageEffect("跳转失败：$reason")
        logError("导航失败: $reason")
    }

    private fun clearPendingNavigationRequest(requestId: Long) {
        if (isNavigationRequestCurrent(requestId)) {
            _pendingNavigation.value = null
        }
    }

    private fun nextNavigationRequestId(): Long {
        synchronized(_stateLock) {
            _navigationRequestId += 1
            return _navigationRequestId
        }
    }

    fun consumeNavigationRevealTarget(target: NavigationRevealTarget) {
        if (_navigationRevealTarget.value == target) {
            _navigationRevealTarget.value = null
        }
    }

    private suspend fun emitMessageEffect(message: String) {
        if (message.isBlank()) return
        _effects.send(WorkspaceUiEffect.ShowMessage(message = message))
    }

    private suspend fun runNavigationRequest(request: NavigationRequest): NavigationExecution {
        if (!isNavigationRequestCurrent(request.id)) return NavigationExecution.Stale

        val result = resolveDeclaration(request.context)
        if (!isNavigationRequestCurrent(request.id)) return NavigationExecution.Stale

        return when (result) {
            is JumpResolveResult.Resolved -> {
                val applied = applyResolvedTarget(
                    context = request.context,
                    target = result.target,
                    requestId = request.id,
                )
                NavigationExecution.Resolved(
                    target = result.target,
                    applied = applied,
                )
            }

            else -> NavigationExecution.Finished(result)
        }
    }

    private fun isNavigationRequestCurrent(requestId: Long): Boolean {
        return _pendingNavigation.value?.id == requestId
    }

    private fun canContinueNavigationRequest(requestId: Long?): Boolean {
        return requestId == null || isNavigationRequestCurrent(requestId)
    }

    private suspend fun resolveDeclaration(context: NavigateRequestContext): JumpResolveResult {
        val lines = loadedCodeLines(
            tabId = context.tabId,
            kind = context.activeKind,
        )
        val sourceClassName = _openTabs.value
            .firstOrNull { it.tabId == context.tabId }
            ?.targetKey
            .orEmpty()

        return navigationService.resolveDeclaration(
            context = context,
            activeLines = lines,
            sourceClassName = sourceClassName,
            workspaceId = workspaceContext.workspaceId,
            workspaceName = workspaceContext.workspaceName,
        )
    }

    private suspend fun applyResolvedTarget(
        context: NavigateRequestContext,
        target: JumpTarget,
        requestId: Long,
    ): Boolean {
        if (!canContinueNavigationRequest(requestId)) return false

        val sourceTab = _openTabs.value.firstOrNull { it.tabId == context.tabId } ?: return false
        val destination = withContext(Dispatchers.IO) {
            navigationService.prepareDeclarationDestination(
                sourceTab = sourceTab,
                targetClassName = target.targetClassName,
                preferredKind = target.targetKind,
                exportCodePathForClass = ::exportCodePathForClass,
            )
        } ?: return false
        if (!canContinueNavigationRequest(requestId)) return false

        val destinationTab = loadPreparedDestination(
            destination = destination,
            requestId = requestId,
        ) ?: return false

        val activeLines = loadedCodeLines(
            tabId = destination.tabId,
            kind = destination.kind,
        )
        if (activeLines.isEmpty()) return false

        val (targetLine, targetOffset) = navigationService.resolveTargetCursor(
            context = context,
            target = target,
            lines = activeLines,
            targetKind = destination.kind,
        )
        if (!canContinueNavigationRequest(requestId)) return false

        updateCursorSelection(
            tabId = destination.tabId,
            kind = destination.kind,
            cursorLine = targetLine,
            cursorOffset = targetOffset,
            selection = null,
        )
        if (!canContinueNavigationRequest(requestId)) return false
        focusPreparedDestination(
            destination = destination,
            destinationTab = destinationTab,
            revealPlan = destinationTab.resolveNavigationRevealPlan(
                preferredKind = destination.kind,
                fallbackPaneIndex = destination.paneIndex,
                revealAllKindsInMixedMode = false,
            ),
            token = requestId,
        )
        return true
    }

    private sealed interface NavigationExecution {
        data class Resolved(
            val target: JumpTarget,
            val applied: Boolean,
        ) : NavigationExecution

        data class Finished(
            val result: JumpResolveResult,
        ) : NavigationExecution

        data object Timeout : NavigationExecution

        data object Stale : NavigationExecution
    }

    fun exportWorkspaceLogs(outputDir: PlatformFile?) {
        if (outputDir == null) {
            return
        }

        launchErrorTask(
            failureText = "日志导出失败",
        ) {
            val result = runIo {
                DexClubLogger.exportWorkspaceLogs(
                    LogExportRequest(
                        outputDir = outputDir,
                    )
                )
            }

            result.onSuccess { exported ->
                logInfo("日志导出成功: path=${exported.outputPath}, files=${exported.fileCount}")
                emitMessageEffect("日志导出成功：${exported.outputPath}（共 ${exported.fileCount} 个文件）")
            }.onFailure { throwable ->
                logError(
                    text = "日志导出失败",
                    throwable = throwable,
                )
                emitMessageEffect("日志导出失败：${throwable.message ?: "未知错误"}")
            }
        }
    }

    private fun scheduleSidePanelStateSave() {
        _sidePanelStateSaveJob?.cancel()
        _sidePanelStateSaveJob = viewModelScope.launch {
            delay(SIDE_PANEL_STATE_SAVE_DEBOUNCE_MS)
            persistSidePanelStateToDatabase()
        }
    }

    private fun restoreSidePanelState(
        snapshot: EditorSessionSidePanelSnapshot,
    ) {
        val restoredState = snapshot.toWorkspaceSidePanelStateSnapshot()
        _expandedPaths.value = restoredState.expandedPaths
        _sidePanelFirstVisibleItemIndex.value = restoredState.firstVisibleItemIndex
        _sidePanelFirstVisibleItemScrollOffset.value = restoredState.firstVisibleItemScrollOffset
        _sidePanelHorizontalScrollOffset.value = restoredState.horizontalScrollOffset
        _sideSelection.value = restoredState.selection
    }

    private suspend fun persistSidePanelStateToDatabase() {
        val request = buildWorkspaceSidePanelPersistRequest(
            expandedPaths = _expandedPaths.value,
            firstVisibleItemIndex = _sidePanelFirstVisibleItemIndex.value,
            firstVisibleItemScrollOffset = _sidePanelFirstVisibleItemScrollOffset.value,
            horizontalScrollOffset = _sidePanelHorizontalScrollOffset.value,
            selection = _sideSelection.value,
            updatedAt = System.currentTimeMillis(),
        )

        runIoCatching {
            editorSessionRepository.replaceSidePanelState(request)
        }.onFailure { throwable ->
            logWarn(
                text = "保存侧边栏状态失败",
                throwable = throwable,
            )
        }
    }

    companion object {
        private const val TAG = "WorkspaceVM"
        private const val DEFINITION_NAVIGATION_ENABLED = true
        private const val NAVIGATION_TIMEOUT_MS = 10_000L
        private const val SIDE_PANEL_STATE_SAVE_DEBOUNCE_MS = 250L
    }
}
