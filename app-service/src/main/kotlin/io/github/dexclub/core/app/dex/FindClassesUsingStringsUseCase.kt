package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.app.session.TargetSessionService
import io.github.dexclub.core.app.support.applyWindowSlice

data class FindClassesUsingStringsUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val containsAnyStrings: List<String>,
    val containsAllStrings: List<String>,
    val offset: Int? = null,
    val limit: Int? = null,
)

data class FindClassesUsingStringsUseCaseResult(
    val session: io.github.dexclub.core.app.session.TargetSession?,
    val workspace: io.github.dexclub.core.api.workspace.WorkspaceContext,
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
    val items: List<ClassHit>,
)

class FindClassesUsingStringsUseCase(
    private val workspaceService: io.github.dexclub.core.api.workspace.WorkspaceService,
    private val dexService: DexAnalysisService,
    private val sessionService: TargetSessionService,
) {
    private val support = DexUseCaseSupport(workspaceService, sessionService)

    fun execute(request: FindClassesUsingStringsUseCaseRequest): FindClassesUsingStringsUseCaseResult {
        if (request.containsAnyStrings.isEmpty() && request.containsAllStrings.isEmpty()) {
            throw IllegalArgumentException("At least one non-blank string filter is required")
        }
        val executionContext = support.resolveExecutionContext(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        val anyHits = if (request.containsAnyStrings.isNotEmpty()) {
            dexService.findClassesUsingStrings(
                workspace = executionContext.workspace,
                request = buildFindClassesUsingStringsRequest(
                    strings = request.containsAnyStrings,
                    requireAll = false,
                ),
            )
        } else {
            emptyList()
        }

        val allHits = if (request.containsAllStrings.isNotEmpty()) {
            dexService.findClassesUsingStrings(
                workspace = executionContext.workspace,
                request = buildFindClassesUsingStringsRequest(
                    strings = request.containsAllStrings,
                    requireAll = true,
                ),
            )
        } else {
            emptyList()
        }

        val combined = when {
            request.containsAnyStrings.isNotEmpty() && request.containsAllStrings.isNotEmpty() ->
                anyHits.intersect(allHits.toSet()).toList()
            request.containsAnyStrings.isNotEmpty() -> anyHits
            else -> allHits
        }.sortedWith(
            compareBy<ClassHit>(
                { it.className },
                { it.sourcePath.orEmpty() },
                { it.sourceEntry.orEmpty() },
            ),
        )
        val slice = applyWindowSlice(
            items = combined,
            offset = request.offset,
            limit = request.limit,
        )
        return FindClassesUsingStringsUseCaseResult(
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
