package io.github.dexclub.core.app.resource

import io.github.dexclub.core.api.resource.ResourceEntryValueHit
import io.github.dexclub.core.api.resource.ResourceService
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService
import io.github.dexclub.core.app.session.TargetWorkspaceResolver
import io.github.dexclub.core.app.support.applyWindowSlice

data class FindResourceValuesUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val type: String,
    val value: String,
    val contains: Boolean = false,
    val ignoreCase: Boolean = false,
    val offset: Int? = null,
    val limit: Int? = null,
)

data class FindResourceValuesUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
    val items: List<ResourceEntryValueHit>,
)

class FindResourceValuesUseCase(
    workspaceService: io.github.dexclub.core.api.workspace.WorkspaceService,
    private val resourceService: ResourceService,
    sessionService: TargetSessionService,
) {
    private val workspaceResolver = TargetWorkspaceResolver(workspaceService, sessionService)

    fun execute(request: FindResourceValuesUseCaseRequest): FindResourceValuesUseCaseResult {
        require(request.type.isNotBlank()) { "type must not be blank" }
        require(request.value.isNotBlank()) { "value must not be blank" }
        val executionContext = workspaceResolver.resolve(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        val hits = resourceService.findResourceValues(
            workspace = executionContext.workspace,
            request = buildFindResourcesRequest(
                type = request.type.trim(),
                value = request.value,
                contains = request.contains,
                ignoreCase = request.ignoreCase,
            ),
        )
        val slice = applyWindowSlice(hits, request.offset, request.limit)
        return FindResourceValuesUseCaseResult(
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
