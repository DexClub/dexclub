package io.github.dexclub.app.scene.workspace

import io.github.dexclub.core.navigation.NavigateRequestContext

data class NavigationRevealTarget(
    val tabId: String,
    val kind: String?,
    val token: Long,
)

data class NavigationRequest(
    val id: Long,
    val context: NavigateRequestContext,
)
