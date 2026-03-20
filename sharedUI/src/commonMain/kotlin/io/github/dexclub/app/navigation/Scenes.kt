package io.github.dexclub.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class Scenes : NavKey {

    @Serializable
    data object Home : Scenes()

    @Serializable
    data class Workspace(val args: WorkspaceRouteArgs) : Scenes()
}
