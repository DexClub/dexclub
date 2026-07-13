package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.FindClassesUsingStringsRequest
import io.github.dexclub.core.api.shared.PageWindow
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService

data class FindClassesUsingStringsByQueryUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val queryText: String,
    val window: PageWindow = PageWindow(),
)

data class FindClassesUsingStringsByQueryUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val items: List<ClassHit>,
)

class FindClassesUsingStringsByQueryUseCase(
    private val workspaceService: WorkspaceService,
    private val dexService: DexAnalysisService,
    private val sessionService: TargetSessionService,
) {
    private val support = DexUseCaseSupport(workspaceService, sessionService)

    fun execute(request: FindClassesUsingStringsByQueryUseCaseRequest): FindClassesUsingStringsByQueryUseCaseResult {
        val executionContext = support.resolveExecutionContext(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        return FindClassesUsingStringsByQueryUseCaseResult(
            session = executionContext.session,
            workspace = executionContext.workspace,
            items = dexService.findClassesUsingStrings(
                workspace = executionContext.workspace,
                request = FindClassesUsingStringsRequest(
                    queryText = request.queryText,
                    window = request.window,
                ),
            ),
        )
    }
}
