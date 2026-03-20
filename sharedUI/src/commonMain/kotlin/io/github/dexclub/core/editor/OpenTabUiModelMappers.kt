package io.github.dexclub.core.editor

import io.github.dexclub.codeview.core.text.LineSelection
import io.github.dexclub.app.model.OpenTabContentSnapshot
import io.github.dexclub.app.model.OpenTabPaneUiModel
import io.github.dexclub.app.model.OpenTabUiModel
import io.github.dexclub.core.workspace.ClassVisualKind

internal fun EditorSessionTabRecord.toUiModel(
    contents: Map<String, EditorSessionContentRecord>,
    panes: List<EditorSessionPaneRecord>,
    kindPriorities: Map<String, Int>,
    classVisualKind: ClassVisualKind?,
): OpenTabUiModel {
    return OpenTabUiModel(
        tabId = tabId,
        targetType = targetType,
        targetKey = targetKey,
        title = title,
        subtitle = subtitle,
        layoutMode = layoutMode,
        activePaneIndex = activePaneIndex,
        activeKind = activeKind,
        createdAt = createdAt,
        lastViewedAt = lastViewedAt,
        pinned = pinned,
        contents = contents.mapValues { (_, content) -> content.toSnapshot() },
        panes = panes.map(EditorSessionPaneRecord::toUiModel),
        kindPriorities = kindPriorities,
        classVisualKind = classVisualKind,
    )
}

internal fun OpenTabUiModel.toSessionTabRecord(): EditorSessionTabRecord {
    return EditorSessionTabRecord(
        tabId = tabId,
        targetType = targetType,
        targetKey = targetKey,
        title = title,
        subtitle = subtitle,
        layoutMode = layoutMode,
        activePaneIndex = activePaneIndex,
        activeKind = activeKind,
        createdAt = createdAt,
        lastViewedAt = lastViewedAt,
        pinned = pinned,
    )
}

internal fun EditorSessionContentRecord.toSnapshot(): OpenTabContentSnapshot {
    return OpenTabContentSnapshot(
        kind = kind,
        codePath = codePath,
        scrollOffsetY = scrollOffsetY,
        scrollOffsetX = scrollOffsetX,
        cursorLine = cursorLine,
        cursorOffset = cursorOffset,
        selection = selectionOrNull(),
        updatedAt = updatedAt,
    )
}

private fun EditorSessionPaneRecord.toUiModel(): OpenTabPaneUiModel {
    return OpenTabPaneUiModel(
        paneIndex = paneIndex,
        kind = kind,
        weight = weight,
    )
}

private fun EditorSessionContentRecord.selectionOrNull(): LineSelection? {
    if (selectionStartLine < 0 || selectionStartOffset < 0 || selectionEndLine < 0 || selectionEndOffset < 0) return null
    return LineSelection(
        startLine = selectionStartLine,
        startOffset = selectionStartOffset,
        endLine = selectionEndLine,
        endOffset = selectionEndOffset,
    )
}
