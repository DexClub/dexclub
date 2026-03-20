package io.github.dexclub.app.scene.workspace

data class WorkspaceHeaderUiState(
    val workspaceName: String = "",
    val displayPath: String = "",
    val searchDialogUiState: WorkspaceSearchDialogUiState = WorkspaceSearchDialogUiState(),
    val settingsUiState: WorkspaceSettingsUiState = WorkspaceSettingsUiState(),
)
