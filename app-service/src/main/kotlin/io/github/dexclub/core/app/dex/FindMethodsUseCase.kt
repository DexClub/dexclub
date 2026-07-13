package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.app.session.TargetSessionService
import io.github.dexclub.core.app.support.applyWindowSlice

data class FindMethodsUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val classNameContains: String? = null,
    val methodNameContains: String? = null,
    val descriptorContains: String? = null,
    val offset: Int? = null,
    val limit: Int? = null,
)

data class FindMethodsUseCaseResult(
    val session: io.github.dexclub.core.app.session.TargetSession?,
    val workspace: io.github.dexclub.core.api.workspace.WorkspaceContext,
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
    val items: List<MethodHit>,
)

class FindMethodsUseCase(
    private val workspaceService: io.github.dexclub.core.api.workspace.WorkspaceService,
    private val dexService: DexAnalysisService,
    private val sessionService: TargetSessionService,
) {
    private val support = DexUseCaseSupport(workspaceService, sessionService)

    fun execute(request: FindMethodsUseCaseRequest): FindMethodsUseCaseResult {
        val executionContext = support.resolveExecutionContext(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        val baseHits = dexService.findMethods(
            workspace = executionContext.workspace,
            request = buildFindMethodsRequest(
                classNameContains = request.classNameContains,
                methodNameContains = request.methodNameContains,
            ),
        )
        val normalizedDescriptor = request.descriptorContains?.trim()?.ifEmpty { null }
        val filtered = if (normalizedDescriptor == null) {
            baseHits
        } else {
            baseHits.filter { it.descriptor.contains(normalizedDescriptor, ignoreCase = false) }
        }.sortedWith(
            compareBy<MethodHit>(
                { it.className },
                { it.methodName },
                { it.descriptor },
                { it.sourcePath.orEmpty() },
                { it.sourceEntry.orEmpty() },
            ),
        )
        val slice = applyWindowSlice(
            items = filtered,
            offset = request.offset,
            limit = request.limit,
        )
        return FindMethodsUseCaseResult(
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
