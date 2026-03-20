package io.github.dexclub.core.workspace

import io.github.dexclub.core.editor.EditorSessionRepository
import io.github.dexclub.core.editor.EditorSessionSidePanelSnapshot
import io.github.dexclub.core.editor.EditorSessionTabRecord

data class WorkspaceBootstrapResult(
    val sidePanelSnapshot: EditorSessionSidePanelSnapshot,
    val classIndexState: WorkspaceIndexState,
    val openTabs: List<EditorSessionTabRecord>,
    val selectedTabId: String?,
    val actualIndexedClassCount: Int,
    val expectedIndexedClassCount: Int,
)

class WorkspaceInitializer(
    private val workspaceIndexService: WorkspaceIndexService,
    private val editorSessionRepository: EditorSessionRepository,
) {
    suspend fun bootstrap(
        classSource: WorkspaceClassSource,
        preferredTabId: String? = null,
        currentSelectedTabId: String? = null,
        onProgress: (String) -> Unit = {},
        onClassIndexMismatch: ((actual: Int, expected: Int) -> Unit)? = null,
        onWarmUpDexKit: (suspend () -> Unit)? = null,
    ): WorkspaceBootstrapResult {
        onProgress("恢复侧边栏状态..")
        val sidePanelSnapshot = editorSessionRepository.getSidePanelSnapshot()

        onProgress("检查类索引完整性..")
        val actualIndexedClassCount = workspaceIndexService.countIndexedClasses()
        val expectedIndexedClassCount = classSource.classCount
        val classIndexState = if (actualIndexedClassCount > 0 && actualIndexedClassCount == expectedIndexedClassCount) {
            onProgress("读取类索引..")
            workspaceIndexService.loadClassTree(onProgress = onProgress)
        } else {
            if (actualIndexedClassCount > 0 && actualIndexedClassCount != expectedIndexedClassCount) {
                onClassIndexMismatch?.invoke(actualIndexedClassCount, expectedIndexedClassCount)
            }
            onProgress("构建类索引..")
            workspaceIndexService.rebuildClassTree(
                classSource = classSource,
                onProgress = onProgress,
            )
        }

        onProgress("恢复编辑会话..")
        val openTabs = editorSessionRepository.getAllTabs()
        val selectedTabId = resolveSelectedTabId(
            tabs = openTabs,
            preferredTabId = preferredTabId,
            currentSelectedTabId = currentSelectedTabId,
        )

        onProgress("初始化 DexKit..")
        onWarmUpDexKit?.invoke()

        return WorkspaceBootstrapResult(
            sidePanelSnapshot = sidePanelSnapshot,
            classIndexState = classIndexState,
            openTabs = openTabs,
            selectedTabId = selectedTabId,
            actualIndexedClassCount = actualIndexedClassCount,
            expectedIndexedClassCount = expectedIndexedClassCount,
        )
    }

    private fun resolveSelectedTabId(
        tabs: List<EditorSessionTabRecord>,
        preferredTabId: String?,
        currentSelectedTabId: String?,
    ): String? {
        if (preferredTabId != null && tabs.any { it.tabId == preferredTabId }) {
            return preferredTabId
        }
        if (currentSelectedTabId != null && tabs.any { it.tabId == currentSelectedTabId }) {
            return currentSelectedTabId
        }
        return tabs.maxWithOrNull(
            compareBy<EditorSessionTabRecord> { it.lastViewedAt }
                .thenBy { it.createdAt }
        )?.tabId
    }
}
