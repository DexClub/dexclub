package io.github.dexclub.app.scene.workspace

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class WorkspaceLayoutMode {
    Compact,
    Medium,
    Expanded,
}

internal val WorkspaceLayoutMode.isCompact: Boolean
    get() = this == WorkspaceLayoutMode.Compact

internal fun resolveWorkspaceLayoutMode(maxWidth: Dp): WorkspaceLayoutMode {
    return when {
        maxWidth < 720.dp -> WorkspaceLayoutMode.Compact
        maxWidth < 1100.dp -> WorkspaceLayoutMode.Medium
        else -> WorkspaceLayoutMode.Expanded
    }
}
