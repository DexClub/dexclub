package io.github.dexclub.cli

import io.github.dexclub.core.app.AppUseCases
import io.github.dexclub.core.app.projection.toProjection

internal class InspectCommandAdapter(
    private val targetWorkspaceRuntime: CliTargetWorkspaceRuntime,
    private val appUseCases: AppUseCases,
) {
    fun inspect(request: CliRequest.Inspect): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.workspace.inspectWorkspaceUseCase.execute(
            io.github.dexclub.core.app.workspace.InspectWorkspaceUseCaseRequest(workspace),
        )
        return CommandResult(
            payload = RenderPayload.Inspect(InspectView.from(result.result)),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun inspectMethod(request: CliRequest.InspectMethod): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.dex.inspectMethodUseCase.execute(
            io.github.dexclub.core.app.dex.InspectMethodUseCaseRequest(
                workspace = workspace,
                descriptor = request.descriptor,
                includes = request.includes,
            ),
        )
        return CommandResult(
            payload = RenderPayload.MethodDetail(MethodDetailView.from(result.detail.toProjection())),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }
}
