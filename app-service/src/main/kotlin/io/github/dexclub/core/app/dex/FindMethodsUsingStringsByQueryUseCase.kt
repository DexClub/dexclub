package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.FindMethodsUsingStringsRequest
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.shared.PageWindow
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService

data class FindMethodsUsingStringsByQueryUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val queryText: String,
    val window: PageWindow = PageWindow(),
)

data class FindMethodsUsingStringsByQueryUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val items: List<MethodHit>,
)

class FindMethodsUsingStringsByQueryUseCase(
    private val workspaceService: WorkspaceService,
    private val dexService: DexAnalysisService,
    private val sessionService: TargetSessionService,
) {
    private val support = DexUseCaseSupport(workspaceService, sessionService)

    fun execute(request: FindMethodsUsingStringsByQueryUseCaseRequest): FindMethodsUsingStringsByQueryUseCaseResult {
        val executionContext = support.resolveExecutionContext(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        return FindMethodsUsingStringsByQueryUseCaseResult(
            session = executionContext.session,
            workspace = executionContext.workspace,
            items = dexService.findMethodsUsingStrings(
                workspace = executionContext.workspace,
                request = FindMethodsUsingStringsRequest(
                    queryText = request.queryText,
                    window = request.window,
                ),
            ),
        )
    }
}
