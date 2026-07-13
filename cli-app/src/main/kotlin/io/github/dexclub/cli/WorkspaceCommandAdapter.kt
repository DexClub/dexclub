package io.github.dexclub.cli

import io.github.dexclub.core.app.contract.WorkspaceState
import io.github.dexclub.core.app.AppUseCases

internal class WorkspaceCommandAdapter(
    private val targetWorkspaceRuntime: CliTargetWorkspaceRuntime,
    private val appUseCases: AppUseCases,
) {
    fun initialize(request: CliRequest.Init): CommandResult {
        val context = appUseCases.workspace.initializeWorkspaceUseCase.execute(
            io.github.dexclub.core.app.workspace.InitializeWorkspaceUseCaseRequest(request.input),
        )
        val status = appUseCases.workspace.loadWorkspaceStatusUseCase.execute(
            io.github.dexclub.core.app.workspace.LoadWorkspaceStatusUseCaseRequest(
                targetWorkspaceRuntime.resolveWorkspaceRef(context.workspace.workdir),
            ),
        )
        return CommandResult(
            payload = RenderPayload.Status(StatusView.from(context.workspace.workdir, status.status)),
            outputFormat = request.outputFormat,
            exitCode = exitCodeForStatus(status.status.state),
        )
    }

    fun switchTarget(request: CliRequest.Switch): CommandResult {
        val workspaceRef = targetWorkspaceRuntime.resolveWorkspaceRef(null)
        val switchedRef = appUseCases.workspace.switchWorkspaceTargetUseCase.execute(
            io.github.dexclub.core.app.workspace.SwitchWorkspaceTargetUseCaseRequest(
                ref = workspaceRef,
                input = request.input,
            ),
        )
        val status = appUseCases.workspace.loadWorkspaceStatusUseCase.execute(
            io.github.dexclub.core.app.workspace.LoadWorkspaceStatusUseCaseRequest(switchedRef.ref),
        )
        return CommandResult(
            payload = RenderPayload.Status(StatusView.from(switchedRef.ref.workdir, status.status)),
            outputFormat = request.outputFormat,
            exitCode = exitCodeForStatus(status.status.state),
        )
    }

    fun loadStatus(request: CliRequest.Status): CommandResult {
        val workspaceRef = targetWorkspaceRuntime.resolveWorkspaceRef(request.workdir)
        val status = appUseCases.workspace.loadWorkspaceStatusUseCase.execute(
            io.github.dexclub.core.app.workspace.LoadWorkspaceStatusUseCaseRequest(workspaceRef),
        )
        return CommandResult(
            payload = RenderPayload.Status(StatusView.from(workspaceRef.workdir, status.status)),
            outputFormat = request.outputFormat,
            exitCode = exitCodeForStatus(status.status.state),
        )
    }

    fun listTargets(request: CliRequest.Targets): CommandResult {
        val workspaceRef = targetWorkspaceRuntime.resolveWorkspaceRef(request.workdir)
        val targets = appUseCases.workspace.listWorkspaceTargetsUseCase.execute(
            io.github.dexclub.core.app.workspace.ListWorkspaceTargetsUseCaseRequest(workspaceRef),
        )
        return CommandResult(
            payload = RenderPayload.Targets(targets.targets.map(TargetSummaryView::from)),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun gc(request: CliRequest.Gc): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.workspace.gcWorkspaceUseCase.execute(
            io.github.dexclub.core.app.workspace.GcWorkspaceUseCaseRequest(workspace),
        )
        return CommandResult(
            payload = RenderPayload.Gc(GcView.from(result.result)),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun refresh(request: CliRequest.Refresh): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.workspace.refreshWorkspaceUseCase.execute(
            io.github.dexclub.core.app.workspace.RefreshWorkspaceUseCaseRequest(workspace),
        )
        return CommandResult(
            payload = RenderPayload.Inspect(InspectView.from(result.result)),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    private fun exitCodeForStatus(state: WorkspaceState): Int =
        when (state) {
            WorkspaceState.Healthy -> 0
            WorkspaceState.Degraded,
            WorkspaceState.Broken,
            -> 2
        }
}

