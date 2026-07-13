package io.github.dexclub.core.app.resource

import io.github.dexclub.core.api.resource.ResourceEntry
import io.github.dexclub.core.api.resource.ResourceService
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService
import io.github.dexclub.core.app.session.TargetWorkspaceResolver
import io.github.dexclub.core.app.support.applyWindowSlice

data class ListResourcesUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val type: String? = null,
    val offset: Int? = null,
    val limit: Int? = null,
)

data class ListResourcesUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
    val items: List<ResourceEntry>,
)

class ListResourcesUseCase(
    workspaceService: io.github.dexclub.core.api.workspace.WorkspaceService,
    private val resourceService: ResourceService,
    sessionService: TargetSessionService,
) {
    private val workspaceResolver = TargetWorkspaceResolver(workspaceService, sessionService)

    fun execute(request: ListResourcesUseCaseRequest): ListResourcesUseCaseResult {
        val executionContext = workspaceResolver.resolve(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        val normalizedType = request.type?.trim()?.ifEmpty { null }
        val filtered = resourceService.listResourceEntries(executionContext.workspace)
            .asSequence()
            .filter { normalizedType == null || it.type == normalizedType }
            .toList()
        val slice = applyWindowSlice(filtered, request.offset, request.limit)
        return ListResourcesUseCaseResult(
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
