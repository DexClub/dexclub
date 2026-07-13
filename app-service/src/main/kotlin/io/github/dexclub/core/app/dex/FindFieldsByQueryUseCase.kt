package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.FieldHit
import io.github.dexclub.core.api.dex.FindFieldsRequest
import io.github.dexclub.core.api.shared.PageWindow
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService

data class FindFieldsByQueryUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val queryText: String,
    val window: PageWindow = PageWindow(),
)

data class FindFieldsByQueryUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val items: List<FieldHit>,
)

class FindFieldsByQueryUseCase(
    private val workspaceService: WorkspaceService,
    private val dexService: DexAnalysisService,
    private val sessionService: TargetSessionService,
) {
    private val support = DexUseCaseSupport(workspaceService, sessionService)

    fun execute(request: FindFieldsByQueryUseCaseRequest): FindFieldsByQueryUseCaseResult {
        val executionContext = support.resolveExecutionContext(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        return FindFieldsByQueryUseCaseResult(
            session = executionContext.session,
            workspace = executionContext.workspace,
            items = dexService.findFields(
                workspace = executionContext.workspace,
                request = FindFieldsRequest(
                    queryText = request.queryText,
                    window = request.window,
                ),
            ),
        )
    }
}
