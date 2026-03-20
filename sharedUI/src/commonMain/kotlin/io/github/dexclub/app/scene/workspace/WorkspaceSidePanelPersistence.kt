package io.github.dexclub.app.scene.workspace

import io.github.dexclub.core.editor.EditorSessionSidePanelPersistRequest
import io.github.dexclub.core.editor.EditorSessionSidePanelSnapshot

internal data class WorkspaceSidePanelStateSnapshot(
    val expandedPaths: Set<String>,
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val horizontalScrollOffset: Int,
    val selection: WorkspaceSideSelection?,
)

internal fun EditorSessionSidePanelSnapshot.toWorkspaceSidePanelStateSnapshot(): WorkspaceSidePanelStateSnapshot {
    val persistedState = state
    return WorkspaceSidePanelStateSnapshot(
        expandedPaths = expandedPaths.toSet(),
        firstVisibleItemIndex = persistedState?.firstVisibleItemIndex?.coerceAtLeast(0) ?: 0,
        firstVisibleItemScrollOffset = persistedState?.firstVisibleItemScrollOffset?.coerceAtLeast(0) ?: 0,
        horizontalScrollOffset = persistedState?.horizontalScrollOffset?.coerceAtLeast(0) ?: 0,
        selection = WorkspaceSideSelection.fromPersisted(
            type = persistedState?.selectedNodeType,
            key = persistedState?.selectedNodeKey,
        ),
    )
}

internal fun buildWorkspaceSidePanelPersistRequest(
    expandedPaths: Set<String>,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    horizontalScrollOffset: Int,
    selection: WorkspaceSideSelection?,
    updatedAt: Long,
): EditorSessionSidePanelPersistRequest {
    return EditorSessionSidePanelPersistRequest(
        firstVisibleItemIndex = firstVisibleItemIndex.coerceAtLeast(0),
        firstVisibleItemScrollOffset = firstVisibleItemScrollOffset.coerceAtLeast(0),
        horizontalScrollOffset = horizontalScrollOffset.coerceAtLeast(0),
        selectedNodeType = selection?.persistedType,
        selectedNodeKey = selection?.persistedKey,
        expandedPaths = expandedPaths.toList().sorted(),
        updatedAt = updatedAt,
    )
}
