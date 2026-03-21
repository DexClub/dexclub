package io.github.dexclub.codeview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect

@Composable
internal actual fun rememberPlatformSelectionToolbarBridge(): PlatformSelectionToolbarBridge {
    return remember { JvmPlatformSelectionToolbarBridge }
}

private object JvmPlatformSelectionToolbarBridge : PlatformSelectionToolbarBridge {
    override val usePlatformSelectionToolbar: Boolean = false

    override fun showSelectionToolbar(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
    }

    override fun hideSelectionToolbar() {
    }
}
