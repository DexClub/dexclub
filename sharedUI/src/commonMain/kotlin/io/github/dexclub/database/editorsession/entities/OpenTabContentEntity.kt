package io.github.dexclub.database.editorsession.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "open_tab_contents",
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
data class OpenTabContentEntity(
    val tabId: String,
    val kind: String,
    val codePath: String,
    var scrollOffsetY: Int = 0,
    var scrollOffsetX: Int = 0,
    var cursorLine: Int = -1,
    var cursorOffset: Int = -1,
    var selectionStartLine: Int = -1,
    var selectionStartOffset: Int = -1,
    var selectionEndLine: Int = -1,
    var selectionEndOffset: Int = -1,
    var updatedAt: Long = 0L,
)

