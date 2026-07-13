package io.github.dexclub.mcp

import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.DexContextRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class McpDexContextRegistryTest {
    @Test
    fun releasesContextOnlyAfterLastSessionForTheSameTargetCloses() {
        val workspace = fakeWorkspaceContext()
        val released = mutableListOf<io.github.dexclub.core.api.workspace.WorkspaceContext>()
        val registry = DexContextRegistry(released::add)
        val first = TargetSession("session-1", workspace, "2026-05-03T10:00:00Z", java.time.Instant.EPOCH)
        val second = TargetSession("session-2", workspace, "2026-05-03T10:00:00Z", java.time.Instant.EPOCH)

        registry.retain(first)
        registry.retain(second)

        registry.release(first)
        assertEquals(emptyList(), released)

        registry.release(second)
        assertEquals(listOf(workspace), released)
    }

    @Test
    fun waitsForInFlightSessionRequestBeforeReleasingContext() {
        val workspace = fakeWorkspaceContext()
        val released = mutableListOf<io.github.dexclub.core.api.workspace.WorkspaceContext>()
        val registry = DexContextRegistry(released::add)
        val session = TargetSession("session-1", workspace, "2026-05-03T10:00:00Z", java.time.Instant.EPOCH)

        registry.retain(session)
        val lease = registry.acquireForSession(session)
        registry.release(session)

        assertEquals(emptyList(), released)
        lease.close()
        assertEquals(listOf(workspace), released)
    }

    @Test
    fun releasesStatelessWorkdirContextWhenRequestFinishes() {
        val workspace = fakeWorkspaceContext()
        val released = mutableListOf<io.github.dexclub.core.api.workspace.WorkspaceContext>()
        val registry = DexContextRegistry(released::add)

        registry.acquireForWorkdir(workspace).close()

        assertEquals(listOf(workspace), released)
    }
}
