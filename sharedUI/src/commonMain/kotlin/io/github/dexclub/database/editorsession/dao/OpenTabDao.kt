package io.github.dexclub.database.editorsession.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.dexclub.database.editorsession.entities.OpenTabContentEntity
import io.github.dexclub.database.editorsession.entities.OpenTabEntity
import io.github.dexclub.database.editorsession.entities.OpenTabKindPriorityEntity
import io.github.dexclub.database.editorsession.entities.OpenTabPaneEntity

@Dao
interface OpenTabDao {
    @Query("SELECT * FROM open_tabs ORDER BY createdAt ASC")
    suspend fun getAllTabs(): List<OpenTabEntity>

    @Query("SELECT * FROM open_tabs WHERE tabId = :tabId LIMIT 1")
    suspend fun getTabById(tabId: String): OpenTabEntity?

    @Query("SELECT * FROM open_tabs WHERE targetType = :targetType AND targetKey = :targetKey LIMIT 1")
    suspend fun getTabByTarget(targetType: String, targetKey: String): OpenTabEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(entity: OpenTabEntity)

    @Update
    suspend fun updateTab(entity: OpenTabEntity)

    @Delete
    suspend fun deleteTab(entity: OpenTabEntity)

    @Query("DELETE FROM open_tabs WHERE tabId = :tabId")
    suspend fun deleteTabById(tabId: String)

    @Query(
        """
            UPDATE open_tabs
            SET lastViewedAt = MAX(lastViewedAt, :lastViewedAt)
            WHERE tabId = :tabId
        """
    )
    suspend fun updateTabLastViewedAt(tabId: String, lastViewedAt: Long)

    @Query("DELETE FROM open_tabs WHERE tabId IN (:tabIds)")
    suspend fun deleteTabsByIds(tabIds: List<String>)

    @Query("SELECT * FROM open_tab_contents WHERE tabId = :tabId")
    suspend fun getContentsByTabId(tabId: String): List<OpenTabContentEntity>

    @Query("SELECT * FROM open_tab_contents WHERE tabId = :tabId AND kind = :kind LIMIT 1")
    suspend fun getContent(tabId: String, kind: String): OpenTabContentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContent(entity: OpenTabContentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContents(entities: List<OpenTabContentEntity>)

    @Update
    suspend fun updateContent(entity: OpenTabContentEntity)

    @Query("DELETE FROM open_tab_contents WHERE tabId = :tabId")
    suspend fun deleteContentsByTabId(tabId: String)

    @Query(
        """
            UPDATE open_tab_contents
            SET scrollOffsetY = :offsetY,
                scrollOffsetX = :offsetX,
                updatedAt = :updatedAt
            WHERE tabId = :tabId AND kind = :kind
        """
    )
    suspend fun updateContentScrollOffset(
        tabId: String,
        kind: String,
        offsetY: Int,
        offsetX: Int,
        updatedAt: Long,
    )

    @Query(
        """
            UPDATE open_tab_contents
            SET cursorLine = :cursorLine,
                cursorOffset = :cursorOffset,
                selectionStartLine = :selectionStartLine,
                selectionStartOffset = :selectionStartOffset,
                selectionEndLine = :selectionEndLine,
                selectionEndOffset = :selectionEndOffset,
                updatedAt = :updatedAt
            WHERE tabId = :tabId AND kind = :kind
        """
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

    @Query("SELECT * FROM open_tab_panes WHERE tabId = :tabId ORDER BY paneIndex ASC")
    suspend fun getPanesByTabId(tabId: String): List<OpenTabPaneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPanes(entities: List<OpenTabPaneEntity>)

    @Query("DELETE FROM open_tab_panes WHERE tabId = :tabId")
    suspend fun deletePanesByTabId(tabId: String)

    @Query("SELECT * FROM open_tab_kind_priority WHERE tabId = :tabId ORDER BY priority ASC")
    suspend fun getKindPrioritiesByTabId(tabId: String): List<OpenTabKindPriorityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKindPriorities(entities: List<OpenTabKindPriorityEntity>)

    @Query("DELETE FROM open_tab_kind_priority WHERE tabId = :tabId")
    suspend fun deleteKindPrioritiesByTabId(tabId: String)

    @Transaction
    suspend fun insertTabWithInitialState(
        tab: OpenTabEntity,
        contents: List<OpenTabContentEntity>,
        panes: List<OpenTabPaneEntity>,
        kindPriorities: List<OpenTabKindPriorityEntity>,
    ) {
        insertTab(tab)
        if (contents.isNotEmpty()) {
            insertContents(contents)
        }
        if (panes.isNotEmpty()) {
            insertPanes(panes)
        }
        if (kindPriorities.isNotEmpty()) {
            insertKindPriorities(kindPriorities)
        }
    }

    @Transaction
    suspend fun replacePanesAndUpdateTab(
        tab: OpenTabEntity,
        panes: List<OpenTabPaneEntity>,
    ) {
        deletePanesByTabId(tab.tabId)
        if (panes.isNotEmpty()) {
            insertPanes(panes)
        }
        updateTab(tab)
    }

    @Transaction
    suspend fun replaceKindPriorities(
        tabId: String,
        priorities: List<OpenTabKindPriorityEntity>,
    ) {
        deleteKindPrioritiesByTabId(tabId)
        if (priorities.isNotEmpty()) {
            insertKindPriorities(priorities)
        }
    }
}
