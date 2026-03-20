package io.github.dexclub.database.editorsession.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "open_tab_panes",
    primaryKeys = ["tabId", "paneIndex"],
    foreignKeys = [
        ForeignKey(
            entity = OpenTabEntity::class,
            parentColumns = ["tabId"],
            childColumns = ["tabId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["tabId"]),
    ],
)
data class OpenTabPaneEntity(
    val tabId: String,
    val paneIndex: Int,
    val kind: String,
    var weight: Float = 1f,
)

