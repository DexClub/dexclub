package io.github.dexclub.core.app.resource

import io.github.dexclub.core.api.resource.ResourceService
import io.github.dexclub.core.api.resource.ResourceValue
import io.github.dexclub.core.api.resource.ResolveResourceRequest
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService
import io.github.dexclub.core.app.session.TargetWorkspaceResolver

data class GetResourceValueUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val resourceId: String? = null,
    val type: String? = null,
    val name: String? = null,
)

data class GetResourceValueUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val resource: ResourceValue,
)

class GetResourceValueUseCase(
    workspaceService: io.github.dexclub.core.api.workspace.WorkspaceService,
    private val resourceService: ResourceService,
    sessionService: TargetSessionService,
) {
    private val workspaceResolver = TargetWorkspaceResolver(workspaceService, sessionService)

    fun execute(request: GetResourceValueUseCaseRequest): GetResourceValueUseCaseResult {
        val executionContext = workspaceResolver.resolve(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        val resource = resourceService.getResourceValue(
            workspace = executionContext.workspace,
            request = ResolveResourceRequest(
                resourceId = request.resourceId,
                type = request.type,
                name = request.name,
            ),
        )
        return GetResourceValueUseCaseResult(
            session = executionContext.session,
            workspace = executionContext.workspace,
            resource = resource,
        )
    }
}
