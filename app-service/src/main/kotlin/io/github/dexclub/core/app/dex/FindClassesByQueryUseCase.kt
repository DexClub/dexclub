package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.FindClassesRequest
import io.github.dexclub.core.api.shared.PageWindow
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService

data class FindClassesByQueryUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val queryText: String,
    val window: PageWindow = PageWindow(),
)

data class FindClassesByQueryUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val items: List<ClassHit>,
)

class FindClassesByQueryUseCase(
    private val workspaceService: WorkspaceService,
    private val dexService: DexAnalysisService,
    private val sessionService: TargetSessionService,
) {
    private val support = DexUseCaseSupport(workspaceService, sessionService)

    fun execute(request: FindClassesByQueryUseCaseRequest): FindClassesByQueryUseCaseResult {
        val executionContext = support.resolveExecutionContext(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        return FindClassesByQueryUseCaseResult(
            session = executionContext.session,
            workspace = executionContext.workspace,
            items = dexService.findClasses(
                workspace = executionContext.workspace,
                request = FindClassesRequest(
                    queryText = request.queryText,
                    window = request.window,
                ),
            ),
        )
    }
}
