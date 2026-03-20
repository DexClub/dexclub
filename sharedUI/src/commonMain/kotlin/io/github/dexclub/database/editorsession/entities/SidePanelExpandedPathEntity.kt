package io.github.dexclub.database.editorsession.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "side_panel_expanded_paths")
data class SidePanelExpandedPathEntity(
    @PrimaryKey
    val fullPath: String,
    val updatedAt: Long = 0L,
)
