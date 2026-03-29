package io.github.dexclub.codeview.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect

internal interface PlatformSelectionToolbarBridge {
    val usePlatformSelectionToolbar: Boolean

    fun showSelectionToolbar(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    )

    fun hideSelectionToolbar()
}

@Composable
internal expect fun rememberPlatformSelectionToolbarBridge(): PlatformSelectionToolbarBridge
