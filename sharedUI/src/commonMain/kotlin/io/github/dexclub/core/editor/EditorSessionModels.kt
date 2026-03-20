package io.github.dexclub.core.editor

const val EDITOR_SESSION_TARGET_TYPE_CLASS = "class"
const val EDITOR_SESSION_TARGET_TYPE_FILE = "file"

const val EDITOR_SESSION_KIND_SMALI = "smali"
const val EDITOR_SESSION_KIND_JAVA = "java"

const val EDITOR_SESSION_LAYOUT_SINGLE = "single"
const val EDITOR_SESSION_LAYOUT_SPLIT_HORIZONTAL = "split_horizontal"
const val EDITOR_SESSION_LAYOUT_SPLIT_VERTICAL = "split_vertical"

data class EditorSessionTabRecord(
    val tabId: String,
    val targetType: String,
    val targetKey: String,
    val title: String,
    val subtitle: String,
    val layoutMode: String = EDITOR_SESSION_LAYOUT_SINGLE,
    val activePaneIndex: Int = 0,
    val activeKind: String = EDITOR_SESSION_KIND_SMALI,
    val createdAt: Long = 0L,
    val lastViewedAt: Long = 0L,
    val pinned: Boolean = false,
)

data class EditorSessionContentRecord(
    val tabId: String,
    val kind: String,
    val codePath: String,
    val scrollOffsetY: Int = 0,
    val scrollOffsetX: Int = 0,
    val cursorLine: Int = -1,
    val cursorOffset: Int = -1,
    val selectionStartLine: Int = -1,
    val selectionStartOffset: Int = -1,
    val selectionEndLine: Int = -1,
    val selectionEndOffset: Int = -1,
    val updatedAt: Long = 0L,
)

data class EditorSessionPaneRecord(
    val tabId: String,
    val paneIndex: Int,
    val kind: String,
    val weight: Float = 1f,
)

data class EditorSessionKindPriorityRecord(
    val tabId: String,
    val kind: String,
    val priority: Int,
)
