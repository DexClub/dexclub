package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.InspectMethodRequest
import io.github.dexclub.core.api.dex.MethodDetail
import io.github.dexclub.core.api.dex.MethodDetailSection
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService

data class InspectMethodUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val methodHandle: String? = null,
    val descriptor: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val includes: Set<MethodDetailSection>,
)

data class InspectMethodUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val detail: MethodDetail,
)

class InspectMethodUseCase(
    private val workspaceService: WorkspaceService,
    private val dexService: DexAnalysisService,
    private val sessionService: TargetSessionService,
) {
    private val support = DexUseCaseSupport(workspaceService, sessionService)

    fun execute(request: InspectMethodUseCaseRequest): InspectMethodUseCaseResult {
        val executionContext = support.resolveExecutionContext(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        val methodRef = support.resolveMethodReference(
            session = executionContext.session,
            methodHandle = request.methodHandle,
            descriptor = request.descriptor,
            sourcePath = request.sourcePath,
            sourceEntry = request.sourceEntry,
        )
        val descriptor = methodRef?.descriptor.orEmpty()
        require(descriptor.isNotEmpty()) { "method_handle or descriptor is required" }

        val detail = dexService.inspectMethod(
            workspace = executionContext.workspace,
            request = InspectMethodRequest(
                descriptor = descriptor,
                source = buildSourceLocator(
                    ref = methodRef,
                    sourcePath = request.sourcePath,
                    sourceEntry = request.sourceEntry,
                ),
                includes = request.includes,
            ),
        )
        return InspectMethodUseCaseResult(
            session = executionContext.session,
            workspace = executionContext.workspace,
            detail = detail,
        )
    }
}
