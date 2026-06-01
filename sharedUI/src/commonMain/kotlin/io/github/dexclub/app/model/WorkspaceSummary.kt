package io.github.dexclub.app.model

import io.github.dexclub.app.navigation.WorkspaceRouteArgs
import io.github.dexclub.core.workspace.WorkspaceRecord

data class WorkspaceSummary(
    val id: Long,
    val name: String,
    val absolutePath: String,
    val displayPath: String,
    val dexsAbsolutePathList: List<String>,
    val validDexs: Int,
    val lastOpenedAt: Long = 0L,
)


fun WorkspaceRecord.toWorkspaceSummary(): WorkspaceSummary {
    return WorkspaceSummary(
        id = id,
        name = name,
        absolutePath = absolutePath,
        displayPath = displayPath,
        dexsAbsolutePathList = dexsAbsolutePathList,
        validDexs = validDexs,
        lastOpenedAt = lastOpenedAt,
    )
}


fun WorkspaceSummary.toRouteArgs(): WorkspaceRouteArgs {
    return WorkspaceRouteArgs(
        workspaceId = id,
        workspaceName = name,
        absolutePath = absolutePath,
        displayPath = displayPath,
        dexsAbsolutePathList = dexsAbsolutePathList,
    )
}
