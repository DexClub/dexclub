package io.github.dexclub.app.scene.workspace

import io.github.dexclub.node.ClassTreeNode

internal data class WorkspaceHeaderCallbacks(
    val onRequestExportWorkspaceLogs: () -> Unit,
    val onResetSearchDialogState: () -> Unit,
    val onSearchDialogTabSelected: (WorkspaceSearchTab) -> Unit,
    val onSearchDialogQueryChange: (String) -> Unit,
    val onSearchDialogRequest: () -> Unit,
    val onOpenClassResult: (WorkspaceClassSearchResult) -> Unit,
    val onOpenStringResult: (WorkspaceStringSearchResult) -> Unit,
    val onSmaliUnicodeDecodeChange: (Boolean) -> Unit,
    val onJavaUnicodeDecodeChange: (Boolean) -> Unit,
    val onCodeScrollPastEndChange: (Int) -> Unit,
)

internal data class WorkspaceSidePanelCallbacks(
    val onNodeClick: (ClassTreeNode) -> Unit,
    val onScrollOffsetsChange: (Int, Int, Int) -> Unit,
)
