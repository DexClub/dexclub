package io.github.dexclub.database.editorsession.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.dexclub.database.editorsession.entities.SidePanelExpandedPathEntity
import io.github.dexclub.database.editorsession.entities.SidePanelStateEntity

@Dao
interface SidePanelStateDao {
    @Query("SELECT * FROM side_panel_state WHERE stateId = 0 LIMIT 1")
    suspend fun getSingletonState(): SidePanelStateEntity?

    @Query("SELECT * FROM side_panel_expanded_paths ORDER BY fullPath ASC")
    suspend fun getExpandedPaths(): List<SidePanelExpandedPathEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: SidePanelStateEntity)

    @Query("DELETE FROM side_panel_expanded_paths")
    suspend fun clearExpandedPaths()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpandedPaths(paths: List<SidePanelExpandedPathEntity>)

    @Transaction
    suspend fun replaceState(
        state: SidePanelStateEntity,
        expandedPaths: List<SidePanelExpandedPathEntity>,
    ) {
        upsertState(state)
        clearExpandedPaths()
        if (expandedPaths.isNotEmpty()) {
            insertExpandedPaths(expandedPaths)
        }
    }
}
