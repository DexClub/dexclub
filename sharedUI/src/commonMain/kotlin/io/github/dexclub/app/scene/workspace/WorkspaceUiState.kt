package io.github.dexclub.app.scene.workspace

data class WorkspaceUiState(
    val loading: Boolean = false,
    val loadingMessage: String = "",
    val headerUiState: WorkspaceHeaderUiState = WorkspaceHeaderUiState(),
    val sidePanelUiState: WorkspaceSidePanelUiState = WorkspaceSidePanelUiState(),
    val codePanelUiState: WorkspaceCodePanelUiState = WorkspaceCodePanelUiState(),
)
