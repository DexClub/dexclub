package io.github.dexclub.mcp

import io.github.dexclub.core.app.session.TargetSessionService
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals
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
            sessionStore = TargetSessionService(nowProvider = { timestamps.removeAt(0) }),
        )

        val first = app.openTargetSession("first.apk")
        val second = app.openTargetSession("second.apk")

        val sessions = app.listTargetSessions()

        assertEquals(listOf(second.sessionId, first.sessionId), sessions.map { it.sessionId })
    }

    @Test
    fun closeTargetSessionRemovesSessionAndClearsHandles() {
        val store = TargetSessionService()
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
    fun refreshTargetSessionUpdatesCachedWorkspaceAndClearsHandles() {
        val initialWorkspace = fakeWorkspaceContext()
        val refreshedWorkspace = initialWorkspace.copy(
            snapshot = initialWorkspace.snapshot.copy(
                contentFingerprint = "content-2",
            ),
        )
        val workspaceService = FakeWorkspaceService(initialWorkspace).also {
            it.refreshedWorkspace = refreshedWorkspace
        }
        val app = createTestApp(workspace = initialWorkspace, workspaceService = workspaceService)
        val session = app.openTargetSession("sample.apk")
        val methodHandle = app.sessionStore.putMethodHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;->foo()V",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )
        val classHandle = app.sessionStore.putClassHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )

        val refreshed = app.refreshTargetSession(session.sessionId)

        assertNotNull(refreshed)
        assertEquals(listOf(initialWorkspace), workspaceService.refreshedWorkspaces)
        assertNotEquals(session.workspace.snapshot.contentFingerprint, refreshed.workspace.snapshot.contentFingerprint)
        assertEquals(refreshed.workspace, app.getTargetSession(session.sessionId)?.workspace)
        assertNull(app.sessionStore.getMethodHandle(session.sessionId, methodHandle))
        assertNull(app.sessionStore.getClassHandle(session.sessionId, classHandle))
    }

    @Test
    fun closingLastSessionForTargetReleasesDexContext() {
        val dexService = FakeDexAnalysisService()
        val app = createTestApp(dexService = dexService)
        val first = app.openTargetSession("sample.apk")
        val second = app.openTargetSession("sample.apk")

        app.closeTargetSession(first.sessionId)
        assertEquals(emptyList(), dexService.releasedDexContexts)

        app.closeTargetSession(second.sessionId)
        assertEquals(listOf(first.workspace), dexService.releasedDexContexts)
    }

    @Test
    fun expiredSessionsArePrunedWithTheirHandles() {
        val timestamps = mutableListOf(
            Instant.parse("2026-05-03T10:00:00Z"),
            Instant.parse("2026-05-03T10:00:00Z"),
            Instant.parse("2026-05-03T10:31:00Z"),
            Instant.parse("2026-05-03T10:31:00Z"),
        )
        val store = TargetSessionService(
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
        val store = TargetSessionService(
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
        val store = TargetSessionService(
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
        assertEquals(600L, snapshot.idleTimeoutSeconds)
        assertEquals(5, snapshot.maxSessions)
        assertEquals(1_000, snapshot.maxHandlesPerSession)
        assertEquals(1, snapshot.sessionCount)
        assertEquals(1, snapshot.methodHandleCount)
        assertEquals(1, snapshot.classHandleCount)
        assertEquals("2026-05-03T10:10:00Z", snapshot.sessions.single().expiresAt)
        assertEquals("2026-05-03T10:00:00Z", snapshot.sessions.single().lastAccessedAt)
    }

    @Test
    fun openingSessionEvictsLeastRecentlyUsedSessionWhenLimitExceeded() {
        val now = Instant.parse("2026-05-03T10:00:00Z")
        val store = TargetSessionService(
            maxSessions = 2,
            nowProvider = { now },
        )
        val first = store.openTargetSession(fakeWorkspaceContext())
        val second = store.openTargetSession(fakeWorkspaceContext())
        val secondHandle = store.putMethodHandle(
            sessionId = second.sessionId,
            descriptor = "Lsample/Test;->foo()V",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )

        assertNotNull(store.getTargetSession(first.sessionId))
        val third = store.openTargetSession(fakeWorkspaceContext())

        assertNotNull(store.getTargetSession(first.sessionId))
        assertNull(store.getTargetSession(second.sessionId))
        assertNotNull(store.getTargetSession(third.sessionId))
        assertNull(store.getMethodHandle(second.sessionId, secondHandle))
    }

    @Test
    fun repeatedHandleCreationReusesExistingHandle() {
        val store = TargetSessionService()
        val session = store.openTargetSession(fakeWorkspaceContext())

        val firstMethodHandle = store.putMethodHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;->foo()V",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )
        val secondMethodHandle = store.putMethodHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;->foo()V",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )
        val firstClassHandle = store.putClassHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )
        val secondClassHandle = store.putClassHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )

        assertEquals(firstMethodHandle, secondMethodHandle)
        assertEquals(firstClassHandle, secondClassHandle)
        assertEquals(1, store.snapshot().methodHandleCount)
        assertEquals(1, store.snapshot().classHandleCount)
    }

    @Test
    fun handleLimitEvictsLeastRecentlyUsedHandleAcrossHandleTypes() {
        val store = TargetSessionService(maxHandlesPerSession = 2)
        val session = store.openTargetSession(fakeWorkspaceContext())
        val firstMethodHandle = store.putMethodHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;->first()V",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )
        val classHandle = store.putClassHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )
        assertNotNull(store.getMethodHandle(session.sessionId, firstMethodHandle))

        val secondMethodHandle = store.putMethodHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;->second()V",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )

        assertNotNull(store.getMethodHandle(session.sessionId, firstMethodHandle))
        assertNull(store.getClassHandle(session.sessionId, classHandle))
        assertNotNull(store.getMethodHandle(session.sessionId, secondMethodHandle))
    }

    @Test
    fun handleCreationRejectsClosedSession() {
        val store = TargetSessionService()
        val session = store.openTargetSession(fakeWorkspaceContext())
        store.closeTargetSession(session.sessionId)

        val failure = assertFailsWith<IllegalArgumentException> {
            store.putMethodHandle(
                sessionId = session.sessionId,
                descriptor = "Lsample/Test;->foo()V",
                sourcePath = "sample.apk",
                sourceEntry = "classes.dex",
            )
        }

        assertEquals("session_id not found: ${session.sessionId}", failure.message)
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
