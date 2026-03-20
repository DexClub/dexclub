package io.github.dexclub.data.editorsession

import io.github.dexclub.core.editor.EditorSessionContentRecord
import io.github.dexclub.core.editor.EditorSessionKindPriorityRecord
import io.github.dexclub.core.editor.EditorSessionPaneRecord
import io.github.dexclub.core.editor.EditorSessionTabRecord
import io.github.dexclub.database.editorsession.entities.OpenTabContentEntity
import io.github.dexclub.database.editorsession.entities.OpenTabEntity
import io.github.dexclub.database.editorsession.entities.OpenTabKindPriorityEntity
import io.github.dexclub.database.editorsession.entities.OpenTabPaneEntity

internal fun OpenTabEntity.toRecord(): EditorSessionTabRecord {
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

internal fun EditorSessionTabRecord.toEntity(): OpenTabEntity {
    return OpenTabEntity(
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

internal fun OpenTabContentEntity.toRecord(): EditorSessionContentRecord {
    return EditorSessionContentRecord(
        tabId = tabId,
        kind = kind,
        codePath = codePath,
        scrollOffsetY = scrollOffsetY,
        scrollOffsetX = scrollOffsetX,
        cursorLine = cursorLine,
        cursorOffset = cursorOffset,
        selectionStartLine = selectionStartLine,
        selectionStartOffset = selectionStartOffset,
        selectionEndLine = selectionEndLine,
        selectionEndOffset = selectionEndOffset,
        updatedAt = updatedAt,
    )
}

internal fun EditorSessionContentRecord.toEntity(): OpenTabContentEntity {
    return OpenTabContentEntity(
        tabId = tabId,
        kind = kind,
        codePath = codePath,
        scrollOffsetY = scrollOffsetY,
        scrollOffsetX = scrollOffsetX,
        cursorLine = cursorLine,
        cursorOffset = cursorOffset,
        selectionStartLine = selectionStartLine,
        selectionStartOffset = selectionStartOffset,
        selectionEndLine = selectionEndLine,
        selectionEndOffset = selectionEndOffset,
        updatedAt = updatedAt,
    )
}

internal fun OpenTabPaneEntity.toRecord(): EditorSessionPaneRecord {
    return EditorSessionPaneRecord(
        tabId = tabId,
        paneIndex = paneIndex,
        kind = kind,
        weight = weight,
    )
}

internal fun EditorSessionPaneRecord.toEntity(): OpenTabPaneEntity {
    return OpenTabPaneEntity(
        tabId = tabId,
        paneIndex = paneIndex,
        kind = kind,
        weight = weight,
    )
}

internal fun OpenTabKindPriorityEntity.toRecord(): EditorSessionKindPriorityRecord {
    return EditorSessionKindPriorityRecord(
        tabId = tabId,
        kind = kind,
        priority = priority,
    )
}

internal fun EditorSessionKindPriorityRecord.toEntity(): OpenTabKindPriorityEntity {
    return OpenTabKindPriorityEntity(
        tabId = tabId,
        kind = kind,
        priority = priority,
    )
}
