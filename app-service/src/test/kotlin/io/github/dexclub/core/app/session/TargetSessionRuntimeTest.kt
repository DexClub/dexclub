package io.github.dexclub.core.app.session

import io.github.dexclub.core.app.appServiceTestDir
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
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TargetSessionRuntimeTest {
    @Test
    fun closesSharedDexContextOnlyAfterLastSessionLeavesTarget() {
        val workspace = fakeWorkspaceContext()
        val released = mutableListOf<WorkspaceContext>()
        val runtime = TargetSessionRuntime(
            workspaceService = FakeWorkspaceService(workspace),
            sessionService = TargetSessionService(),
            dexContextRegistry = DexContextRegistry(released::add),
        )

        val first = runtime.openTargetSession("sample.apk")
        val second = runtime.openTargetSession("sample.apk")

        runtime.closeTargetSession(first.sessionId)
        assertEquals(emptyList(), released)

        runtime.closeTargetSession(second.sessionId)
        assertEquals(listOf(workspace), released)
    }

    @Test
    fun prunedSessionsAlsoReleaseTheirDexContext() {
        val workspace = fakeWorkspaceContext()
        val released = mutableListOf<WorkspaceContext>()
        val timestamps = mutableListOf(
            Instant.parse("2026-05-03T10:00:00Z"),
            Instant.parse("2026-05-03T10:00:00Z"),
            Instant.parse("2026-05-03T10:31:00Z"),
            Instant.parse("2026-05-03T10:31:00Z"),
        )
        val runtime = TargetSessionRuntime(
            workspaceService = FakeWorkspaceService(workspace),
            sessionService = TargetSessionService(
                idleTimeout = Duration.ofMinutes(30),
                nowProvider = { timestamps.removeAt(0) },
            ),
            dexContextRegistry = DexContextRegistry(released::add),
        )

        runtime.openTargetSession("sample.apk")
        runtime.listTargetSessions()
        runtime.snapshot()

        assertEquals(listOf(workspace), released)
    }

    @Test
    fun resolvesSessionExecutionContextAndReusesSessionDexLease() {
        val workspace = fakeWorkspaceContext()
        val released = mutableListOf<WorkspaceContext>()
        val runtime = TargetSessionRuntime(
            workspaceService = FakeWorkspaceService(workspace),
            sessionService = TargetSessionService(),
            dexContextRegistry = DexContextRegistry(released::add),
        )

        val session = runtime.openTargetSession("sample.apk")
        val context = runtime.resolveExecutionContext(sessionId = session.sessionId)
        val lease = runtime.acquireDexContextForExecutionContext(context)

        assertEquals(session.sessionId, context.session?.sessionId)
        assertEquals(session.workspace, context.session?.workspace)
        assertEquals(workspace, context.workspace)
        lease.close()
        assertEquals(emptyList(), released)

        runtime.closeTargetSession(session.sessionId)
        assertEquals(listOf(workspace), released)
    }

    @Test
    fun resolvesWorkdirExecutionContextAndAcquiresStatelessDexLease() {
        val workspace = fakeWorkspaceContext()
        val released = mutableListOf<WorkspaceContext>()
        val runtime = TargetSessionRuntime(
            workspaceService = FakeWorkspaceService(workspace),
            sessionService = TargetSessionService(),
            dexContextRegistry = DexContextRegistry(released::add),
        )

        val context = runtime.resolveExecutionContext(workdir = workspace.workdir)
        val lease = runtime.acquireDexContextForExecutionContext(context)

        assertNull(context.session)
        assertEquals(workspace, context.workspace)
        lease.close()
        assertEquals(listOf(workspace), released)
    }
}

private fun fakeWorkspaceContext(): WorkspaceContext =
    appServiceTestDir("workspace").let { workdir ->
        WorkspaceContext(
            workdir = workdir.toString(),
            dexclubDir = appServiceTestDir("workspace", ".dexclub").toString(),
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

private class FakeWorkspaceService(
    private val workspace: WorkspaceContext,
) : WorkspaceService {
    override fun initialize(input: String): WorkspaceContext = workspace

    override fun switchTarget(ref: WorkspaceRef, input: String): WorkspaceRef = ref

    override fun open(ref: WorkspaceRef): WorkspaceContext = workspace

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
