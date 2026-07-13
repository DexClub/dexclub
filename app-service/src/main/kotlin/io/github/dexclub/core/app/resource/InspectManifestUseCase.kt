package io.github.dexclub.core.app.resource

import io.github.dexclub.core.api.resource.ManifestInspectionResult
import io.github.dexclub.core.api.resource.ManifestInspectionSection
import io.github.dexclub.core.api.resource.ResourceService
import io.github.dexclub.core.api.resource.InspectManifestRequest
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService
import io.github.dexclub.core.app.session.TargetWorkspaceResolver

data class InspectManifestUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val includes: Set<ManifestInspectionSection>,
    val includeText: Boolean = false,
)

data class InspectManifestUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val manifest: ManifestInspectionResult,
)

class InspectManifestUseCase(
    workspaceService: io.github.dexclub.core.api.workspace.WorkspaceService,
    private val resourceService: ResourceService,
    sessionService: TargetSessionService,
) {
    private val workspaceResolver = TargetWorkspaceResolver(workspaceService, sessionService)

    fun execute(request: InspectManifestUseCaseRequest): InspectManifestUseCaseResult {
        val executionContext = workspaceResolver.resolve(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        val manifest = resourceService.inspectManifest(
            workspace = executionContext.workspace,
            request = InspectManifestRequest(
                includes = request.includes,
                includeText = request.includeText,
            ),
        )
        return InspectManifestUseCaseResult(
            session = executionContext.session,
            workspace = executionContext.workspace,
            manifest = manifest,
        )
    }
}
