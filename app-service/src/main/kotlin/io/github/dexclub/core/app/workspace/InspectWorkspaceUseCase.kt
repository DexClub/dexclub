package io.github.dexclub.core.app.workspace

import io.github.dexclub.core.api.workspace.InspectResult
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService

data class InspectWorkspaceUseCaseRequest(
    val workspace: WorkspaceContext,
)

data class InspectWorkspaceUseCaseResult(
    val result: InspectResult,
)

class InspectWorkspaceUseCase(
    private val workspaceService: WorkspaceService,
) {
    fun execute(request: InspectWorkspaceUseCaseRequest): InspectWorkspaceUseCaseResult =
        InspectWorkspaceUseCaseResult(
            result = workspaceService.inspect(request.workspace),
        )
}
