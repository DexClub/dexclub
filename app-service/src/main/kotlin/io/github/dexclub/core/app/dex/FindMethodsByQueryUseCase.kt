package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.FindMethodsRequest
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.shared.PageWindow
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService

data class FindMethodsByQueryUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val queryText: String,
    val window: PageWindow = PageWindow(),
)

data class FindMethodsByQueryUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val items: List<MethodHit>,
)

class FindMethodsByQueryUseCase(
    private val workspaceService: WorkspaceService,
    private val dexService: DexAnalysisService,
    private val sessionService: TargetSessionService,
) {
    private val support = DexUseCaseSupport(workspaceService, sessionService)

    fun execute(request: FindMethodsByQueryUseCaseRequest): FindMethodsByQueryUseCaseResult {
        val executionContext = support.resolveExecutionContext(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        return FindMethodsByQueryUseCaseResult(
            session = executionContext.session,
            workspace = executionContext.workspace,
            items = dexService.findMethods(
                workspace = executionContext.workspace,
                request = FindMethodsRequest(
                    queryText = request.queryText,
                    window = request.window,
                ),
            ),
        )
    }
}
