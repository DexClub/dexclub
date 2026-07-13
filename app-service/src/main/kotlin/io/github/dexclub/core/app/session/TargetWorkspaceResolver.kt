package io.github.dexclub.core.app.session

import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceRef
import io.github.dexclub.core.api.workspace.WorkspaceService

data class TargetExecutionContext(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
)

class TargetWorkspaceResolver(
    private val workspaceService: WorkspaceService,
    private val sessionService: TargetSessionService,
) {
    private val workspaceRuntime = WorkspaceRuntime(workspaceService)

    fun resolve(
        workspace: WorkspaceContext? = null,
        sessionId: String? = null,
        workdir: String? = null,
    ): TargetExecutionContext {
        workspace?.let { return TargetExecutionContext(session = null, workspace = it) }

        val normalizedSessionId = sessionId?.trim()?.ifEmpty { null }
        if (normalizedSessionId != null) {
            val session = sessionService.getTargetSession(normalizedSessionId)
                ?: throw NoSuchElementException("session_id not found: $normalizedSessionId")
            return TargetExecutionContext(session = session, workspace = session.workspace)
        }

        val normalizedWorkdir = workdir?.trim()?.ifEmpty { null }
            ?: throw IllegalArgumentException("session_id or workdir is required")
        val openedWorkspace = workspaceRuntime.open(normalizedWorkdir)
        return TargetExecutionContext(session = null, workspace = openedWorkspace)
    }
}
