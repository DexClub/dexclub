package io.github.dexclub.app.model

import io.github.dexclub.codeview.core.text.LineSelection
import io.github.dexclub.core.editor.EDITOR_SESSION_KIND_JAVA
import io.github.dexclub.core.editor.EDITOR_SESSION_KIND_SMALI
import io.github.dexclub.core.editor.EDITOR_SESSION_LAYOUT_SINGLE
import io.github.dexclub.core.editor.EDITOR_SESSION_LAYOUT_SPLIT_HORIZONTAL
import io.github.dexclub.core.editor.EDITOR_SESSION_LAYOUT_SPLIT_VERTICAL
import io.github.dexclub.core.editor.EDITOR_SESSION_TARGET_TYPE_CLASS
import io.github.dexclub.core.editor.EDITOR_SESSION_TARGET_TYPE_FILE
import io.github.dexclub.core.workspace.ClassVisualKind

const val OPEN_TAB_TARGET_TYPE_CLASS = EDITOR_SESSION_TARGET_TYPE_CLASS
const val OPEN_TAB_TARGET_TYPE_FILE = EDITOR_SESSION_TARGET_TYPE_FILE

const val OPEN_TAB_KIND_SMALI = EDITOR_SESSION_KIND_SMALI
const val OPEN_TAB_KIND_JAVA = EDITOR_SESSION_KIND_JAVA

const val OPEN_TAB_LAYOUT_SINGLE = EDITOR_SESSION_LAYOUT_SINGLE
const val OPEN_TAB_LAYOUT_SPLIT_HORIZONTAL = EDITOR_SESSION_LAYOUT_SPLIT_HORIZONTAL
const val OPEN_TAB_LAYOUT_SPLIT_VERTICAL = EDITOR_SESSION_LAYOUT_SPLIT_VERTICAL

enum class OpenTabMode {
    SMALI,
    JAVA,
    MIXED,
}

data class OpenTabPaneUiModel(
    val paneIndex: Int,
    val kind: String,
    val weight: Float = 1f,
)

data class OpenTabContentSnapshot(
    val kind: String,
    val codePath: String,
    val scrollOffsetY: Int = 0,
    val scrollOffsetX: Int = 0,
    val cursorLine: Int = -1,
    val cursorOffset: Int = -1,
    val selection: LineSelection? = null,
    val updatedAt: Long = 0L,
)

data class OpenTabUiModel(
    val tabId: String,
    val targetType: String,
    val targetKey: String,
    val title: String,
    val subtitle: String,
    val layoutMode: String = OPEN_TAB_LAYOUT_SINGLE,
    val activePaneIndex: Int = 0,
    val activeKind: String = OPEN_TAB_KIND_SMALI,
    val createdAt: Long = 0L,
    val lastViewedAt: Long = 0L,
    val pinned: Boolean = false,
    val contents: Map<String, OpenTabContentSnapshot>,
    val panes: List<OpenTabPaneUiModel>,
    val kindPriorities: Map<String, Int>,
    val classVisualKind: ClassVisualKind? = null,
) {
    val mode: OpenTabMode
        get() = when {
            layoutMode == OPEN_TAB_LAYOUT_SPLIT_HORIZONTAL || layoutMode == OPEN_TAB_LAYOUT_SPLIT_VERTICAL -> OpenTabMode.MIXED
            primaryKind == OPEN_TAB_KIND_JAVA -> OpenTabMode.JAVA
            else -> OpenTabMode.SMALI
        }

    private val activePane: OpenTabPaneUiModel?
        get() = panes.firstOrNull { it.paneIndex == activePaneIndex }

    val primaryKind: String
        get() = activePane?.kind ?: panes.firstOrNull()?.kind ?: activeKind

    val requiredKinds: List<String>
        get() = panes
            .sortedBy { it.paneIndex }
            .map { it.kind }
            .ifEmpty { listOf(activeKind) }
            .distinct()
}

