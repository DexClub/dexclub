package io.github.dexclub.app.scene.workspace

import androidx.compose.runtime.Composable
import io.github.dexclub.app.navigation.WorkspaceRouteArgs

@Composable
actual fun WorkspaceScene(
    onBackPressed: () -> Unit,
    onRequestNavigateHome: () -> Unit,
    onRequestCreateWorkspace: () -> Unit,
    onRequestOpenWorkspace: () -> Unit,
    routeArgs: WorkspaceRouteArgs,
) {
    val model = rememberWorkspaceSceneViewModel(routeArgs)
    val requestExportWorkspaceLogs = rememberWorkspaceLogExportLauncher(
        initialDirectoryPath = routeArgs.absolutePath,
        onDirectoryPicked = model::exportWorkspaceLogs,
    )

    WorkspaceSceneContent(
        model = model,
        onRequestExportWorkspaceLogs = requestExportWorkspaceLogs,
        onBackPressed = onBackPressed,
        onRequestNavigateHome = onRequestNavigateHome,
        onRequestCreateWorkspace = onRequestCreateWorkspace,
        onRequestOpenWorkspace = onRequestOpenWorkspace,
        drawerGesturesEnabled = false,
    )
}

