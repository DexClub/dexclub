package io.github.dexclub.app.scene.workspace

import io.github.dexclub.node.FlattenedNode

internal data class WorkspaceSidePanelScrollUiState(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0,
    val horizontalScrollOffset: Int = 0,
)

data class WorkspaceSidePanelUiState(
    val flattenedItems: List<FlattenedNode> = emptyList(),
    val expandedPaths: Set<String> = emptySet(),
    val selection: WorkspaceSideSelection? = null,
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0,
    val horizontalScrollOffset: Int = 0,
)

internal fun buildWorkspaceSidePanelUiState(
    flattenedItems: List<FlattenedNode>,
    expandedPaths: Set<String>,
    selection: WorkspaceSideSelection?,
    scrollUiState: WorkspaceSidePanelScrollUiState,
): WorkspaceSidePanelUiState {
    return WorkspaceSidePanelUiState(
        flattenedItems = flattenedItems,
        expandedPaths = expandedPaths,
        selection = selection,
        firstVisibleItemIndex = scrollUiState.firstVisibleItemIndex,
        firstVisibleItemScrollOffset = scrollUiState.firstVisibleItemScrollOffset,
        horizontalScrollOffset = scrollUiState.horizontalScrollOffset,
    )
}
