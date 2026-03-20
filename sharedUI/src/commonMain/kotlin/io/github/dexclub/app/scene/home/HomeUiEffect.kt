package io.github.dexclub.app.scene.home

import io.github.dexclub.app.navigation.WorkspaceRouteArgs

sealed interface HomeUiEffect {
    data class ShowMessage(val message: String) : HomeUiEffect

    data class EnterWorkspace(val routeArgs: WorkspaceRouteArgs) : HomeUiEffect
}
