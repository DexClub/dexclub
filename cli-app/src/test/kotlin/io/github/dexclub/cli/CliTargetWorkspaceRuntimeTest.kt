package io.github.dexclub.cli

import io.github.dexclub.core.api.shared.CacheState
import io.github.dexclub.core.api.shared.CapabilitySet
import io.github.dexclub.core.api.shared.InputType
import io.github.dexclub.core.api.shared.InventoryCounts
import io.github.dexclub.core.api.shared.WorkspaceIssue
import io.github.dexclub.core.api.shared.WorkspaceKind
import io.github.dexclub.core.api.shared.WorkspaceState
import io.github.dexclub.core.api.workspace.GcResult
import io.github.dexclub.core.api.workspace.InspectResult
import io.github.dexclub.core.api.workspace.TargetHandle
import io.github.dexclub.core.api.workspace.TargetSnapshotSummary
import io.github.dexclub.core.api.workspace.TargetSummary
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceRef
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.api.workspace.WorkspaceStatus
import io.github.dexclub.core.app.session.WorkspaceRuntime
import kotlin.test.Test
import kotlin.test.assertEquals

class CliTargetWorkspaceRuntimeTest {
    @Test
    fun openWorkspaceResolvesWorkdirAndDelegatesToWorkspaceRuntime() {
        val workspace = fakeWorkspaceContext()
        val workspaceService = RecordingWorkspaceServiceForRuntime(workspace)
        val runtime = CliTargetWorkspaceRuntime(
            workdirResolver = WorkdirResolver { "/cwd" },
            workspaceRuntime = WorkspaceRuntime(workspaceService),
        )

        val opened = runtime.openWorkspace("  /tmp/work  ")

        assertEquals(workspace, opened)
        assertEquals(listOf(WorkspaceRef("/tmp/work")), workspaceService.openedRefs)
    }

    @Test
    fun openWorkspaceFallsBackToCwdWhenWorkdirIsBlank() {
        val workspace = fakeWorkspaceContext()
        val workspaceService = RecordingWorkspaceServiceForRuntime(workspace)
        val runtime = CliTargetWorkspaceRuntime(
            workdirResolver = WorkdirResolver { "/cwd" },
            workspaceRuntime = WorkspaceRuntime(workspaceService),
        )

        val opened = runtime.openWorkspace("   ")

        assertEquals(workspace, opened)
        assertEquals(listOf(WorkspaceRef("/cwd")), workspaceService.openedRefs)
    }
}

private fun fakeWorkspaceContext(): WorkspaceContext =
    cliAppTestDir("workspace").let { workdir ->
        WorkspaceContext(
            workdir = workdir.toString(),
            dexclubDir = cliAppTestDir("workspace", ".dexclub").toString(),
        workspaceId = "ws-1",
        activeTargetId = "target-1",
        activeTarget = TargetHandle(
            targetId = "target-1",
            inputType = InputType.File,
            inputPath = "sample.apk",
        ),
        snapshot = TargetSnapshotSummary(
            kind = WorkspaceKind.Apk,
            inventoryFingerprint = "inv-1",
            contentFingerprint = "content-1",
            capabilities = CapabilitySet(
                inspect = true,
                findClass = true,
                findMethod = true,
                exportSmali = true,
            ),
            inventoryCounts = InventoryCounts(
                apkCount = 1,
                dexCount = 2,
                manifestCount = 1,
                arscCount = 1,
                binaryXmlCount = 3,
            ),
        ),
        )
    }

private class RecordingWorkspaceServiceForRuntime(
    private val workspace: WorkspaceContext,
) : WorkspaceService {
    val openedRefs = mutableListOf<WorkspaceRef>()

    override fun initialize(input: String): WorkspaceContext = workspace

    override fun switchTarget(ref: WorkspaceRef, input: String): WorkspaceRef = ref

    override fun open(ref: WorkspaceRef): WorkspaceContext {
        openedRefs += ref
        return workspace
    }

    override fun listTargets(ref: WorkspaceRef): List<TargetSummary> = emptyList()

    override fun loadStatus(ref: WorkspaceRef): WorkspaceStatus =
        WorkspaceStatus(
            workspaceId = workspace.workspaceId,
            activeTargetId = workspace.activeTargetId,
            state = WorkspaceState.Healthy,
            issues = emptyList<WorkspaceIssue>(),
            activeTarget = workspace.activeTarget,
            snapshot = workspace.snapshot,
            cacheState = CacheState.Present,
        )

    override fun gc(workspace: WorkspaceContext): GcResult =
        GcResult(
            workdir = workspace.workdir,
            targetId = workspace.activeTargetId,
            deletedFiles = 0,
            deletedBytes = 0,
        )

    override fun refresh(workspace: WorkspaceContext): InspectResult =
        InspectResult(
            target = workspace.activeTarget,
            snapshot = workspace.snapshot,
            classCount = null,
        )

    override fun inspect(workspace: WorkspaceContext): InspectResult =
        InspectResult(
            target = workspace.activeTarget,
            snapshot = workspace.snapshot,
            classCount = null,
        )
}
