package io.github.dexclub.database.editorsession.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "open_tabs",
    indices = [
        Index(value = ["targetType", "targetKey"], unique = true),
    ],
)
data class OpenTabEntity(
    @PrimaryKey
    val tabId: String,
    val targetType: String,
    val targetKey: String,
    val title: String,
    val subtitle: String,
    var layoutMode: String = LAYOUT_SINGLE,
    var activePaneIndex: Int = 0,
    var activeKind: String = KIND_SMALI,
    var createdAt: Long = 0L,
    var lastViewedAt: Long = 0L,
    var pinned: Boolean = false,
) {
    companion object {
        const val TARGET_TYPE_CLASS = "class"
        const val TARGET_TYPE_FILE = "file"

        const val KIND_SMALI = "smali"
        const val KIND_JAVA = "java"

        const val LAYOUT_SINGLE = "single"
        const val LAYOUT_SPLIT_HORIZONTAL = "split_horizontal"
        const val LAYOUT_SPLIT_VERTICAL = "split_vertical"
    }
}

