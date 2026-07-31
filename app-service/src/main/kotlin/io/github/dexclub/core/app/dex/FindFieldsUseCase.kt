package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.FieldHit
import io.github.dexclub.core.api.dex.FindFieldsRequest
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService
import io.github.dexclub.core.app.support.applyWindowSlice

data class FindFieldsUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val queryText: String,
    val offset: Int? = null,
    val limit: Int? = null,
)

data class FindFieldsUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
    val items: List<FieldHit>,
)

class FindFieldsUseCase(
    workspaceService: WorkspaceService,
    private val dexService: DexAnalysisService,
    sessionService: TargetSessionService,
) {
    private val support = DexUseCaseSupport(workspaceService, sessionService)

    fun execute(request: FindFieldsUseCaseRequest): FindFieldsUseCaseResult {
        val executionContext = support.resolveExecutionContext(request.workspace, request.sessionId, request.workdir)
        val slice = applyWindowSlice(
            items = dexService.findFields(
                workspace = executionContext.workspace,
                request = FindFieldsRequest(queryText = request.queryText),
            ),
            offset = request.offset,
            limit = request.limit,
        )
        return FindFieldsUseCaseResult(
            session = executionContext.session,
            workspace = executionContext.workspace,
            total = slice.total,
            offset = slice.offset,
            limit = slice.limit,
            hasMore = slice.hasMore,
            items = slice.items,
        )
    }
}
