package io.github.dexclub.core.app.resource

import io.github.dexclub.core.api.resource.ResourceEntry
import io.github.dexclub.core.api.resource.ResourceResolution
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
            .sortedWith(resourceEntryListOrder)
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

private val resourceEntryListOrder =
    compareBy<ResourceEntry>(
        { it.type.orEmpty() },
        { listOrderResolutionRank(it.resolution) },
        { -listOrderCompleteness(it) },
        { it.name.orEmpty() },
        { it.filePath.orEmpty() },
        { it.sourceEntry.orEmpty() },
        { it.resourceId.orEmpty() },
    )

private fun listOrderResolutionRank(resolution: ResourceResolution): Int =
    when (resolution) {
        ResourceResolution.TableBacked -> 0
        ResourceResolution.PathInferred -> 1
        ResourceResolution.TableValue -> 2
        ResourceResolution.Unresolved -> 3
        ResourceResolution.TableHole -> 4
    }

private fun listOrderCompleteness(entry: ResourceEntry): Int =
    listOf(entry.name, entry.filePath, entry.sourcePath, entry.sourceEntry, entry.resourceId)
        .count { !it.isNullOrBlank() }
