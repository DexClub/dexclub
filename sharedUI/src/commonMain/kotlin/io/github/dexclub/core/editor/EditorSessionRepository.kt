package io.github.dexclub.core.editor

data class EditorSessionSidePanelStateSnapshot(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0,
    val horizontalScrollOffset: Int = 0,
    val selectedNodeType: String? = null,
    val selectedNodeKey: String? = null,
    val updatedAt: Long = 0L,
)

data class EditorSessionSidePanelSnapshot(
    val state: EditorSessionSidePanelStateSnapshot?,
    val expandedPaths: List<String>,
)

data class EditorSessionSidePanelPersistRequest(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0,
    val horizontalScrollOffset: Int = 0,
    val selectedNodeType: String? = null,
    val selectedNodeKey: String? = null,
    val expandedPaths: List<String> = emptyList(),
    val updatedAt: Long = 0L,
)

interface EditorSessionRepository {
    suspend fun getAllTabs(): List<EditorSessionTabRecord>

    suspend fun getTabById(tabId: String): EditorSessionTabRecord?

    suspend fun getTabByTarget(targetType: String, targetKey: String): EditorSessionTabRecord?

    suspend fun insertTabWithInitialState(
        tab: EditorSessionTabRecord,
        contents: List<EditorSessionContentRecord>,
        panes: List<EditorSessionPaneRecord>,
        kindPriorities: List<EditorSessionKindPriorityRecord>,
    )

    suspend fun updateTab(tab: EditorSessionTabRecord)

    suspend fun updateTabLastViewedAt(tabId: String, lastViewedAt: Long)

    suspend fun deleteTabsByIds(tabIds: List<String>)

    suspend fun getContentsByTabId(tabId: String): List<EditorSessionContentRecord>

    suspend fun getContent(tabId: String, kind: String): EditorSessionContentRecord?

    suspend fun insertContent(content: EditorSessionContentRecord)

    suspend fun getPanesByTabId(tabId: String): List<EditorSessionPaneRecord>

    suspend fun replacePanesAndUpdateTab(
        tab: EditorSessionTabRecord,
        panes: List<EditorSessionPaneRecord>,
    )

    suspend fun getKindPrioritiesByTabId(tabId: String): List<EditorSessionKindPriorityRecord>

    suspend fun insertKindPriorities(priorities: List<EditorSessionKindPriorityRecord>)

    suspend fun replaceKindPriorities(
        tabId: String,
        priorities: List<EditorSessionKindPriorityRecord>,
    )

    suspend fun updateContentScrollOffset(
        tabId: String,
        kind: String,
        offsetY: Int,
        offsetX: Int,
        updatedAt: Long,
    )

    suspend fun updateContentCursorSelection(
        tabId: String,
        kind: String,
        cursorLine: Int,
        cursorOffset: Int,
        selectionStartLine: Int,
        selectionStartOffset: Int,
        selectionEndLine: Int,
        selectionEndOffset: Int,
        updatedAt: Long,
    )

    suspend fun getSidePanelSnapshot(): EditorSessionSidePanelSnapshot

    suspend fun replaceSidePanelState(request: EditorSessionSidePanelPersistRequest)

    fun close()
}
