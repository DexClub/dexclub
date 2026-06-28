package io.github.dexclub.app.scene.home

import io.github.dexclub.app.model.WorkspaceSummary

data class HomeUiState(
    val loading: Boolean = false,
    val loadingMessage: String = "",
    val newWorkspaceDialog: Boolean = false,
    val workspaceItems: List<WorkspaceSummary> = emptyList(),
    val selectedWorkspaceItem: WorkspaceSummary? = null,
    val deleteConfirmDialog: Boolean = false,
    val selectedTab: HomeTab = HomeTab.Projects,
    val projectCacheDir: String = "",
    val defaultProjectCacheDir: String = "",
)

enum class HomeTab {
    Projects,
    Customize,
}
