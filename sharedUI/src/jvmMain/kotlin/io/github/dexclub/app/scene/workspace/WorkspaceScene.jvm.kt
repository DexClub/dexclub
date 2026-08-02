package io.github.dexclub.app.scene.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import io.github.dexclub.app.LocalComposeWindow
import io.github.dexclub.app.navigation.WorkspaceRouteArgs
import java.awt.Cursor
import java.awt.Frame

@Composable
actual fun WorkspaceScene(
    onBackPressed: () -> Unit,
    onRequestNavigateHome: () -> Unit,
    onRequestCreateWorkspace: () -> Unit,
    onRequestOpenWorkspace: () -> Unit,
    routeArgs: WorkspaceRouteArgs,
) {
    val model = rememberWorkspaceSceneViewModel(routeArgs)
    val window = LocalComposeWindow.current
    val requestExportWorkspaceLogs = rememberWorkspaceLogExportLauncher(
        initialDirectoryPath = routeArgs.absolutePath,
        onDirectoryPicked = model::exportWorkspaceLogs,
    )

    LaunchedEffect(Unit) {
        window.extendedState = Frame.MAXIMIZED_BOTH
    }

    WorkspaceSceneContent(
        model = model,
        onRequestExportWorkspaceLogs = requestExportWorkspaceLogs,
        onBackPressed = onBackPressed,
        onRequestNavigateHome = onRequestNavigateHome,
        onRequestCreateWorkspace = onRequestCreateWorkspace,
        onRequestOpenWorkspace = onRequestOpenWorkspace,
        dragHandleModifier = Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))),
    )
}

