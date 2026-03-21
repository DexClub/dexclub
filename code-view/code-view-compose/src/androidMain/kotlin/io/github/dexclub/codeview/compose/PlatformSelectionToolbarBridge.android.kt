package io.github.dexclub.codeview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar

@Composable
internal actual fun rememberPlatformSelectionToolbarBridge(): PlatformSelectionToolbarBridge {
    val textToolbar = LocalTextToolbar.current
    return remember(textToolbar) {
        AndroidPlatformSelectionToolbarBridge(textToolbar)
    }
}

private class AndroidPlatformSelectionToolbarBridge(
    private val textToolbar: TextToolbar,
) : PlatformSelectionToolbarBridge {
    override val usePlatformSelectionToolbar: Boolean = true

    override fun showSelectionToolbar(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        textToolbar.showMenu(
            rect = rect,
            onCopyRequested = onCopyRequested,
            onPasteRequested = null,
            onCutRequested = null,
            onSelectAllRequested = onSelectAllRequested,
        )
    }

    override fun hideSelectionToolbar() {
        textToolbar.hide()
    }
}
