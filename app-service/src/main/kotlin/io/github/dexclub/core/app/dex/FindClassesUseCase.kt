package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.FindClassesRequest
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService
import io.github.dexclub.core.app.support.applyWindowSlice

data class FindClassesUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val queryText: String,
    val offset: Int? = null,
    val limit: Int? = null,
)

data class FindClassesUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
    val items: List<ClassHit>,
)

class FindClassesUseCase(
    workspaceService: WorkspaceService,
    private val dexService: DexAnalysisService,
    sessionService: TargetSessionService,
) {
    private val support = DexUseCaseSupport(workspaceService, sessionService)

    fun execute(request: FindClassesUseCaseRequest): FindClassesUseCaseResult {
        val executionContext = support.resolveExecutionContext(request.workspace, request.sessionId, request.workdir)
        val slice = applyWindowSlice(
            items = dexService.findClasses(
                workspace = executionContext.workspace,
                request = FindClassesRequest(queryText = request.queryText),
            ),
            offset = request.offset,
            limit = request.limit,
        )
        return FindClassesUseCaseResult(
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
