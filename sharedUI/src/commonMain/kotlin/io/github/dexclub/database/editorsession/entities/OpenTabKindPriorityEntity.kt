package io.github.dexclub.database.editorsession.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "open_tab_kind_priority",
    primaryKeys = ["tabId", "kind"],
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
data class OpenTabKindPriorityEntity(
    val tabId: String,
    val kind: String,
    var priority: Int,
)

