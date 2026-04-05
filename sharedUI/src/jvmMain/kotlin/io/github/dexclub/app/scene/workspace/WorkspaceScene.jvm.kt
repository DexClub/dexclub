package io.github.dexclub.app.scene.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import io.github.dexclub.app.LocalComposeWindow
import io.github.dexclub.app.navigation.WorkspaceRouteArgs
import java.awt.Cursor
import java.awt.Frame
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

@Composable
actual fun WorkspaceScene(
    onBackPressed: () -> Unit,
    routeArgs: WorkspaceRouteArgs,
    model: WorkspaceSceneViewModel,
) {
    val window = LocalComposeWindow.current
    val requestExportWorkspaceLogs = rememberWorkspaceLogExportLauncher(
        initialDirectoryPath = routeArgs.absolutePath,
        onDirectoryPicked = model::exportWorkspaceLogs,
    )

    LaunchedEffect(Unit) {
        window.extendedState = Frame.MAXIMIZED_BOTH
    }

    DisposableEffect(window, model) {
        val dispatcher = KeyEventDispatcher { event ->
            if (!window.isActive) {
                return@KeyEventDispatcher false
            }
            if (event.id != KeyEvent.KEY_PRESSED || event.keyCode != KeyEvent.VK_F) {
                return@KeyEventDispatcher false
            }
            if (!event.isControlDown && !event.isMetaDown) {
                return@KeyEventDispatcher false
            }
            model.requestInPageSearchForSelectedPane()
            true
        }
        val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        focusManager.addKeyEventDispatcher(dispatcher)
        onDispose {
            focusManager.removeKeyEventDispatcher(dispatcher)
        }
    }

    WorkspaceSceneContent(
        model = model,
        onRequestExportWorkspaceLogs = requestExportWorkspaceLogs,
        onBackPressed = onBackPressed,
        dragHandleModifier = Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))),
    )
}

