package io.github.dexclub.core.app.resource

import io.github.dexclub.core.api.resource.DecodeXmlRequest
import io.github.dexclub.core.api.resource.DecodedXmlResult
import io.github.dexclub.core.api.resource.ResourceService
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService
import io.github.dexclub.core.app.session.TargetWorkspaceResolver

data class DecodeXmlUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val path: String,
)

data class DecodeXmlUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val xml: DecodedXmlResult,
)

class DecodeXmlUseCase(
    workspaceService: io.github.dexclub.core.api.workspace.WorkspaceService,
    private val resourceService: ResourceService,
    sessionService: TargetSessionService,
) {
    private val workspaceResolver = TargetWorkspaceResolver(workspaceService, sessionService)

    fun execute(request: DecodeXmlUseCaseRequest): DecodeXmlUseCaseResult {
        val executionContext = workspaceResolver.resolve(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        val xml = resourceService.decodeXml(
            workspace = executionContext.workspace,
            request = DecodeXmlRequest(path = request.path),
        )
        return DecodeXmlUseCaseResult(
            session = executionContext.session,
            workspace = executionContext.workspace,
            xml = xml,
        )
    }
}
