package io.github.dexclub.core.app.resource

import io.github.dexclub.core.api.resource.ResourceService
import io.github.dexclub.core.api.resource.ResourceTableResult
import io.github.dexclub.core.api.workspace.WorkspaceContext

data class DumpResourceTableUseCaseRequest(
    val workspace: WorkspaceContext,
)

data class DumpResourceTableUseCaseResult(
    val result: ResourceTableResult,
)

class DumpResourceTableUseCase(
    private val resourceService: ResourceService,
) {
    fun execute(request: DumpResourceTableUseCaseRequest): DumpResourceTableUseCaseResult =
        DumpResourceTableUseCaseResult(
            result = resourceService.dumpResourceTable(request.workspace),
        )
}
