package io.github.dexclub.app.scene.workspace

import io.github.dexclub.app.navigation.WorkspaceRouteArgs

internal data class WorkspaceSceneContext(
    val absolutePath: String,
    val displayPath: String,
    val dexsAbsolutePathList: List<String>,
    val workspaceId: Long?,
    val workspaceName: String,
)

internal fun WorkspaceRouteArgs.toWorkspaceSceneContext(): WorkspaceSceneContext {
    return WorkspaceSceneContext(
        absolutePath = absolutePath,
        displayPath = displayPath,
        dexsAbsolutePathList = dexsAbsolutePathList,
        workspaceId = workspaceId.takeIf { it > 0L },
        workspaceName = workspaceName,
    )
}
