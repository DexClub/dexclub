package io.github.dexclub.mcp

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class McpSessionToolsTest {
    @Test
    fun openTargetSessionDelegatesToWorkspaceInitializeAndCachesSession() {
        val workspace = fakeWorkspaceContext()
        val workspaceService = FakeWorkspaceService(workspace)
        val app = createTestApp(workspace = workspace, workspaceService = workspaceService)

        val session = app.openTargetSession("sample.apk")

        assertEquals("sample.apk", workspaceService.initializedInput)
        assertEquals(workspace.workdir, session.workspace.workdir)
        val cached = app.getTargetSession(session.sessionId)
        assertNotNull(cached)
        assertEquals(session.sessionId, cached.sessionId)
        assertEquals(session.workspace, cached.workspace)
        assertEquals(session.createdAt, cached.createdAt)
    }

    @Test
    fun listTargetSessionsReturnsNewestFirst() {
        val timestamps = mutableListOf(
            Instant.parse("2026-05-03T10:00:00Z"),
            Instant.parse("2026-05-03T10:00:00Z"),
            Instant.parse("2026-05-03T10:00:01Z"),
            Instant.parse("2026-05-03T10:00:01Z"),
            Instant.parse("2026-05-03T10:00:02Z"),
        )
        val app = createTestApp(
            workspaceService = FakeWorkspaceService(fakeWorkspaceContext()),
            sessionStore = McpSessionStore(nowProvider = { timestamps.removeAt(0) }),
        )

        val first = app.openTargetSession("first.apk")
        val second = app.openTargetSession("second.apk")

        val sessions = app.listTargetSessions()

        assertEquals(listOf(second.sessionId, first.sessionId), sessions.map { it.sessionId })
    }

    @Test
    fun closeTargetSessionRemovesSessionAndClearsHandles() {
        val store = McpSessionStore()
        val session = store.openTargetSession(fakeWorkspaceContext())
        val methodHandle = store.putMethodHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;->foo()V",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )
        val classHandle = store.putClassHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )

        val closed = store.closeTargetSession(session.sessionId)

        assertNotNull(closed)
        assertEquals(session.sessionId, closed.sessionId)
        assertEquals(session.workspace, closed.workspace)
        assertNull(store.getTargetSession(session.sessionId))
        assertNull(store.getMethodHandle(session.sessionId, methodHandle))
        assertNull(store.getClassHandle(session.sessionId, classHandle))
    }

    @Test
    fun expiredSessionsArePrunedWithTheirHandles() {
        val timestamps = mutableListOf(
            Instant.parse("2026-05-03T10:00:00Z"),
            Instant.parse("2026-05-03T10:00:00Z"),
            Instant.parse("2026-05-03T10:31:00Z"),
            Instant.parse("2026-05-03T10:31:00Z"),
        )
        val store = McpSessionStore(
            idleTimeout = Duration.ofMinutes(30),
            nowProvider = { timestamps.removeAt(0) },
        )
        val session = store.openTargetSession(fakeWorkspaceContext())
        val methodHandle = store.putMethodHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;->foo()V",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )

        store.pruneExpiredSessions()

        assertNull(store.getTargetSession(session.sessionId))
        assertNull(store.getMethodHandle(session.sessionId, methodHandle))
    }

    @Test
    fun accessingSessionRefreshesLastAccessedAt() {
        val timestamps = mutableListOf(
            Instant.parse("2026-05-03T10:00:00Z"),
            Instant.parse("2026-05-03T10:10:00Z"),
            Instant.parse("2026-05-03T10:10:00Z"),
            Instant.parse("2026-05-03T10:35:00Z"),
            Instant.parse("2026-05-03T10:35:00Z"),
            Instant.parse("2026-05-03T10:35:00Z"),
        )
        val store = McpSessionStore(
            idleTimeout = Duration.ofMinutes(30),
            nowProvider = { timestamps.removeAt(0) },
        )
        val session = store.openTargetSession(fakeWorkspaceContext())

        assertNotNull(store.getTargetSession(session.sessionId))

        store.pruneExpiredSessions()

        assertNotNull(store.getTargetSession(session.sessionId))
    }

    @Test
    fun diagnoseTargetSessionsExposesRuntimeSnapshot() {
        val timestamps = mutableListOf(
            Instant.parse("2026-05-03T10:00:00Z"),
            Instant.parse("2026-05-03T10:00:00Z"),
            Instant.parse("2026-05-03T10:05:00Z"),
        )
        val store = McpSessionStore(
            idleTimeout = Duration.ofMinutes(30),
            nowProvider = { timestamps.removeAt(0) },
        )
        val session = store.openTargetSession(fakeWorkspaceContext())
        store.putMethodHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;->foo()V",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )
        store.putClassHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )

        val snapshot = store.snapshot().toView()

        assertEquals("2026-05-03T10:05:00Z", snapshot.now)
        assertEquals(1800L, snapshot.idleTimeoutSeconds)
        assertEquals(1, snapshot.sessionCount)
        assertEquals(1, snapshot.methodHandleCount)
        assertEquals(1, snapshot.classHandleCount)
        assertEquals("2026-05-03T10:30:00Z", snapshot.sessions.single().expiresAt)
        assertEquals("2026-05-03T10:00:00Z", snapshot.sessions.single().lastAccessedAt)
    }

    @Test
    fun staleSessionMessageExplainsHowToRecover() {
        val message = createTestApp().staleSessionMessage("dead-session")

        assertEquals(
            "session_id not found: dead-session. The MCP process may have restarted, the session may have expired, or the chat may have been restored. Reopen the target with open_target_session, or switch to workdir for stateless calls",
            message,
        )
    }
}
