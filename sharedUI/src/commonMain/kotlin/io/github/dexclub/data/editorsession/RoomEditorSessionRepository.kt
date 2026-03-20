package io.github.dexclub.data.editorsession

import io.github.dexclub.core.editor.EditorSessionContentRecord
import io.github.dexclub.core.editor.EditorSessionKindPriorityRecord
import io.github.dexclub.core.editor.EditorSessionSidePanelPersistRequest
import io.github.dexclub.core.editor.EditorSessionRepository
import io.github.dexclub.core.editor.EditorSessionPaneRecord
import io.github.dexclub.core.editor.EditorSessionSidePanelSnapshot
import io.github.dexclub.core.editor.EditorSessionSidePanelStateSnapshot
import io.github.dexclub.core.editor.EditorSessionTabRecord
import io.github.dexclub.database.editorsession.EditorSessionDatabase
import io.github.dexclub.database.editorsession.entities.SidePanelExpandedPathEntity
import io.github.dexclub.database.editorsession.entities.SidePanelStateEntity

class RoomEditorSessionRepository(
    private val databaseDir: String,
) : EditorSessionRepository {
    private val database: EditorSessionDatabase by lazy {
        EditorSessionDatabase.open(databaseDir)
    }

    override suspend fun getAllTabs(): List<EditorSessionTabRecord> {
        return database.openTabDao().getAllTabs().map { entity -> entity.toRecord() }
    }

    override suspend fun getTabById(tabId: String): EditorSessionTabRecord? {
        return database.openTabDao().getTabById(tabId)?.toRecord()
    }

    override suspend fun getTabByTarget(targetType: String, targetKey: String): EditorSessionTabRecord? {
        return database.openTabDao().getTabByTarget(targetType, targetKey)?.toRecord()
    }

    override suspend fun insertTabWithInitialState(
        tab: EditorSessionTabRecord,
        contents: List<EditorSessionContentRecord>,
        panes: List<EditorSessionPaneRecord>,
        kindPriorities: List<EditorSessionKindPriorityRecord>,
    ) {
        database.openTabDao().insertTabWithInitialState(
            tab = tab.toEntity(),
            contents = contents.map(EditorSessionContentRecord::toEntity),
            panes = panes.map(EditorSessionPaneRecord::toEntity),
            kindPriorities = kindPriorities.map(EditorSessionKindPriorityRecord::toEntity),
        )
    }

    override suspend fun updateTab(tab: EditorSessionTabRecord) {
        database.openTabDao().updateTab(tab.toEntity())
    }

    override suspend fun updateTabLastViewedAt(tabId: String, lastViewedAt: Long) {
        database.openTabDao().updateTabLastViewedAt(
            tabId = tabId,
            lastViewedAt = lastViewedAt,
        )
    }

    override suspend fun deleteTabsByIds(tabIds: List<String>) {
        database.openTabDao().deleteTabsByIds(tabIds)
    }

    override suspend fun getContentsByTabId(tabId: String): List<EditorSessionContentRecord> {
        return database.openTabDao().getContentsByTabId(tabId).map { entity -> entity.toRecord() }
    }

    override suspend fun getContent(tabId: String, kind: String): EditorSessionContentRecord? {
        return database.openTabDao().getContent(tabId, kind)?.toRecord()
    }

    override suspend fun insertContent(content: EditorSessionContentRecord) {
        database.openTabDao().insertContent(content.toEntity())
    }

    override suspend fun getPanesByTabId(tabId: String): List<EditorSessionPaneRecord> {
        return database.openTabDao().getPanesByTabId(tabId).map { entity -> entity.toRecord() }
    }

    override suspend fun replacePanesAndUpdateTab(
        tab: EditorSessionTabRecord,
        panes: List<EditorSessionPaneRecord>,
    ) {
        database.openTabDao().replacePanesAndUpdateTab(
            tab = tab.toEntity(),
            panes = panes.map(EditorSessionPaneRecord::toEntity),
        )
    }

    override suspend fun getKindPrioritiesByTabId(tabId: String): List<EditorSessionKindPriorityRecord> {
        return database.openTabDao().getKindPrioritiesByTabId(tabId).map { entity -> entity.toRecord() }
    }

    override suspend fun insertKindPriorities(priorities: List<EditorSessionKindPriorityRecord>) {
        database.openTabDao().insertKindPriorities(priorities.map(EditorSessionKindPriorityRecord::toEntity))
    }

    override suspend fun replaceKindPriorities(
        tabId: String,
        priorities: List<EditorSessionKindPriorityRecord>,
    ) {
        database.openTabDao().replaceKindPriorities(
            tabId = tabId,
            priorities = priorities.map(EditorSessionKindPriorityRecord::toEntity),
        )
    }

    override suspend fun updateContentScrollOffset(
        tabId: String,
        kind: String,
        offsetY: Int,
        offsetX: Int,
        updatedAt: Long,
    ) {
        database.openTabDao().updateContentScrollOffset(
            tabId = tabId,
            kind = kind,
            offsetY = offsetY,
            offsetX = offsetX,
            updatedAt = updatedAt,
        )
    }

    override suspend fun updateContentCursorSelection(
        tabId: String,
        kind: String,
        cursorLine: Int,
        cursorOffset: Int,
        selectionStartLine: Int,
        selectionStartOffset: Int,
        selectionEndLine: Int,
        selectionEndOffset: Int,
        updatedAt: Long,
    ) {
        database.openTabDao().updateContentCursorSelection(
            tabId = tabId,
            kind = kind,
            cursorLine = cursorLine,
            cursorOffset = cursorOffset,
            selectionStartLine = selectionStartLine,
            selectionStartOffset = selectionStartOffset,
            selectionEndLine = selectionEndLine,
            selectionEndOffset = selectionEndOffset,
            updatedAt = updatedAt,
        )
    }

    override suspend fun getSidePanelSnapshot(): EditorSessionSidePanelSnapshot {
        val dao = database.sidePanelStateDao()
        val state = dao.getSingletonState()
        return EditorSessionSidePanelSnapshot(
            state = state?.let { persisted ->
                EditorSessionSidePanelStateSnapshot(
                    firstVisibleItemIndex = persisted.firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = persisted.firstVisibleItemScrollOffset,
                    horizontalScrollOffset = persisted.horizontalScrollOffset,
                    selectedNodeType = persisted.selectedNodeType,
                    selectedNodeKey = persisted.selectedNodeKey,
                    updatedAt = persisted.updatedAt,
                )
            },
            expandedPaths = dao.getExpandedPaths().map { it.fullPath },
        )
    }

    override suspend fun replaceSidePanelState(request: EditorSessionSidePanelPersistRequest) {
        database.sidePanelStateDao().replaceState(
            state = SidePanelStateEntity(
                firstVisibleItemIndex = request.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = request.firstVisibleItemScrollOffset,
                horizontalScrollOffset = request.horizontalScrollOffset,
                selectedNodeType = request.selectedNodeType,
                selectedNodeKey = request.selectedNodeKey,
                updatedAt = request.updatedAt,
            ),
            expandedPaths = request.expandedPaths.map { fullPath ->
                SidePanelExpandedPathEntity(
                    fullPath = fullPath,
                    updatedAt = request.updatedAt,
                )
            },
        )
    }

    override fun close() {
        EditorSessionDatabase.close(databaseDir)
    }
}
