package io.github.dexclub.core.app.workspace

import io.github.dexclub.core.api.workspace.GcResult
import io.github.dexclub.core.api.workspace.InspectResult
import io.github.dexclub.core.api.workspace.TargetSummary
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceRef
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.api.workspace.WorkspaceStatus

data class InitializeWorkspaceUseCaseRequest(
    val input: String,
)

data class InitializeWorkspaceUseCaseResult(
    val workspace: WorkspaceContext,
)

class InitializeWorkspaceUseCase(
    private val workspaceService: WorkspaceService,
) {
    fun execute(request: InitializeWorkspaceUseCaseRequest): InitializeWorkspaceUseCaseResult =
        InitializeWorkspaceUseCaseResult(
            workspace = workspaceService.initialize(request.input),
        )
}

data class SwitchWorkspaceTargetUseCaseRequest(
    val ref: WorkspaceRef,
    val input: String,
)

data class SwitchWorkspaceTargetUseCaseResult(
    val ref: WorkspaceRef,
)

class SwitchWorkspaceTargetUseCase(
    private val workspaceService: WorkspaceService,
) {
    fun execute(request: SwitchWorkspaceTargetUseCaseRequest): SwitchWorkspaceTargetUseCaseResult =
        SwitchWorkspaceTargetUseCaseResult(
            ref = workspaceService.switchTarget(request.ref, request.input),
        )
}

data class LoadWorkspaceStatusUseCaseRequest(
    val ref: WorkspaceRef,
)

data class LoadWorkspaceStatusUseCaseResult(
    val status: WorkspaceStatus,
)

class LoadWorkspaceStatusUseCase(
    private val workspaceService: WorkspaceService,
) {
    fun execute(request: LoadWorkspaceStatusUseCaseRequest): LoadWorkspaceStatusUseCaseResult =
        LoadWorkspaceStatusUseCaseResult(
            status = workspaceService.loadStatus(request.ref),
        )
}

data class ListWorkspaceTargetsUseCaseRequest(
    val ref: WorkspaceRef,
)

data class ListWorkspaceTargetsUseCaseResult(
    val targets: List<TargetSummary>,
)

class ListWorkspaceTargetsUseCase(
    private val workspaceService: WorkspaceService,
) {
    fun execute(request: ListWorkspaceTargetsUseCaseRequest): ListWorkspaceTargetsUseCaseResult =
        ListWorkspaceTargetsUseCaseResult(
            targets = workspaceService.listTargets(request.ref),
        )
}

data class GcWorkspaceUseCaseRequest(
    val workspace: WorkspaceContext,
)

data class GcWorkspaceUseCaseResult(
    val result: GcResult,
)

class GcWorkspaceUseCase(
    private val workspaceService: WorkspaceService,
) {
    fun execute(request: GcWorkspaceUseCaseRequest): GcWorkspaceUseCaseResult =
        GcWorkspaceUseCaseResult(
            result = workspaceService.gc(request.workspace),
        )
}

data class RefreshWorkspaceUseCaseRequest(
    val workspace: WorkspaceContext,
)

data class RefreshWorkspaceUseCaseResult(
    val result: InspectResult,
)

class RefreshWorkspaceUseCase(
    private val workspaceService: WorkspaceService,
) {
    fun execute(request: RefreshWorkspaceUseCaseRequest): RefreshWorkspaceUseCaseResult =
        RefreshWorkspaceUseCaseResult(
            result = workspaceService.refresh(request.workspace),
        )
}
