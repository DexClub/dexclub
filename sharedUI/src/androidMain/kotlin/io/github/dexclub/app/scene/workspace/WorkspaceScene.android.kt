package io.github.dexclub.app.scene.workspace

import androidx.compose.runtime.Composable
import io.github.dexclub.app.navigation.WorkspaceRouteArgs

@Composable
actual fun WorkspaceScene(
    onBackPressed: () -> Unit,
    routeArgs: WorkspaceRouteArgs,
    model: WorkspaceSceneViewModel,
) {
    val requestExportWorkspaceLogs = rememberWorkspaceLogExportLauncher(
        initialDirectoryPath = routeArgs.absolutePath,
        onDirectoryPicked = model::exportWorkspaceLogs,
    )

    WorkspaceSceneContent(
        model = model,
        onRequestExportWorkspaceLogs = requestExportWorkspaceLogs,
        onBackPressed = onBackPressed,
        drawerGesturesEnabled = false,
    )
}

