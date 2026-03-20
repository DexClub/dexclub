package io.github.dexclub.app.navigation

import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceRouteArgs(
    val workspaceId: Long,
    val workspaceName: String,
    val absolutePath: String,
    val displayPath: String,
    val dexsAbsolutePathList: List<String>,
)
