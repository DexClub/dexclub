package io.github.dexclub.core.app.session

import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService
import java.time.Duration
import java.time.Instant
import java.util.LinkedHashMap
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class TargetSession(
    val sessionId: String,
    val workspace: WorkspaceContext,
    val createdAt: String,
    val lastAccessedAt: Instant,
)

data class SessionStoreSnapshot(
    val now: Instant,
    val idleTimeout: Duration?,
    val maxSessions: Int,
    val maxHandlesPerSession: Int,
    val sessionCount: Int,
    val methodHandleCount: Int,
    val classHandleCount: Int,
    val sessions: List<TargetSession>,
)

interface SourceBackedHandleRef {
    val sessionId: String
    val descriptor: String
    val sourcePath: String?
    val sourceEntry: String?
}

data class MethodHandleRef(
    override val sessionId: String,
    override val descriptor: String,
    override val sourcePath: String? = null,
    override val sourceEntry: String? = null,
) : SourceBackedHandleRef

data class ClassHandleRef(
    override val sessionId: String,
    override val descriptor: String,
    override val sourcePath: String? = null,
    override val sourceEntry: String? = null,
) : SourceBackedHandleRef

class TargetSessionService(
    workspaceService: WorkspaceService? = null,
    private val idleTimeout: Duration? = Duration.ofMinutes(10),
    private val maxSessions: Int = 5,
    private val maxHandlesPerSession: Int = 1_000,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    private val lock = ReentrantLock()
    private val targetSessions = LinkedHashMap<String, TargetSession>(16, 0.75f, true)
    private val handles = HashMap<String, SourceBackedHandleRef>()
    private val handlesByRef = HashMap<SourceBackedHandleRef, String>()
    private val sessionHandles = HashMap<String, SessionHandleState>()
    private val removedTargetSessions = ArrayDeque<TargetSession>()

    @Volatile
    private var workspaceService: WorkspaceService? = workspaceService

    private var methodHandleCount = 0
    private var classHandleCount = 0

    init {
        require(idleTimeout == null || !idleTimeout.isNegative) { "idleTimeout must be null or non-negative" }
        require(maxSessions > 0) { "maxSessions must be greater than 0" }
        require(maxHandlesPerSession > 0) { "maxHandlesPerSession must be greater than 0" }
    }

    fun attachWorkspaceService(workspaceService: WorkspaceService) {
        this.workspaceService = workspaceService
    }

    fun openTargetSession(input: String): TargetSession =
        openTargetSession(requireWorkspaceService().initialize(input))

    fun openTargetSession(workspace: WorkspaceContext): TargetSession =
        lock.withLock {
            pruneExpiredSessionsLocked()
            val now = nowProvider()
            val session = TargetSession(
                sessionId = UUID.randomUUID().toString(),
                workspace = workspace,
                createdAt = now.toString(),
                lastAccessedAt = now,
            )
            targetSessions[session.sessionId] = session
            sessionHandles.putIfAbsent(session.sessionId, SessionHandleState())
            evictOverflowSessionsLocked()
            session
        }

    fun getTargetSession(sessionId: String): TargetSession? =
        lock.withLock {
            pruneExpiredSessionsLocked()
            touchTargetSessionLocked(sessionId)
        }

    fun listTargetSessions(): List<TargetSession> =
        lock.withLock {
            pruneExpiredSessionsLocked()
            targetSessions.values
                .toList()
                .sortedByDescending { it.createdAt }
        }

    fun closeTargetSession(sessionId: String): TargetSession? =
        lock.withLock {
            pruneExpiredSessionsLocked()
            removeTargetSessionLocked(sessionId)
        }

    fun refreshTargetSession(sessionId: String): TargetSession? {
        val session = getTargetSession(sessionId) ?: return null
        val refreshed = requireWorkspaceService().refresh(session.workspace)
        val refreshedWorkspace = session.workspace.copy(
            activeTarget = refreshed.target,
            snapshot = refreshed.snapshot,
        )
        return refreshTargetSession(sessionId, refreshedWorkspace)
    }

    fun refreshTargetSession(sessionId: String, workspace: WorkspaceContext): TargetSession? =
        lock.withLock {
            pruneExpiredSessionsLocked()
            val session = targetSessions[sessionId] ?: return null
            clearSessionHandlesLocked(sessionId)
            val refreshed = session.copy(
                workspace = workspace,
                lastAccessedAt = nowProvider(),
            )
            targetSessions[sessionId] = refreshed
            refreshed
        }

    fun putMethodHandle(sessionId: String, descriptor: String, sourcePath: String?, sourceEntry: String?): String =
        lock.withLock {
            putHandleLocked(
                MethodHandleRef(
                    sessionId = sessionId,
                    descriptor = descriptor,
                    sourcePath = sourcePath,
                    sourceEntry = sourceEntry,
                ),
            )
        }

    fun getMethodHandle(sessionId: String, handle: String): MethodHandleRef? =
        lock.withLock {
            touchHandleLocked(sessionId, handle)
            (handles[handle] as? MethodHandleRef)
                ?.takeIf { it.sessionId == sessionId }
        }

    fun putClassHandle(sessionId: String, descriptor: String, sourcePath: String?, sourceEntry: String?): String =
        lock.withLock {
            putHandleLocked(
                ClassHandleRef(
                    sessionId = sessionId,
                    descriptor = descriptor,
                    sourcePath = sourcePath,
                    sourceEntry = sourceEntry,
                ),
            )
        }

    fun getClassHandle(sessionId: String, handle: String): ClassHandleRef? =
        lock.withLock {
            touchHandleLocked(sessionId, handle)
            (handles[handle] as? ClassHandleRef)
                ?.takeIf { it.sessionId == sessionId }
        }

    fun pruneExpiredSessions() {
        lock.withLock {
            pruneExpiredSessionsLocked()
        }
    }

    fun drainRemovedTargetSessions(): List<TargetSession> =
        lock.withLock {
            if (removedTargetSessions.isEmpty()) {
                emptyList()
            } else {
                removedTargetSessions.toList().also { removedTargetSessions.clear() }
            }
        }

    fun snapshot(): SessionStoreSnapshot =
        lock.withLock {
            pruneExpiredSessionsLocked()
            SessionStoreSnapshot(
                now = nowProvider(),
                idleTimeout = idleTimeout,
                maxSessions = maxSessions,
                maxHandlesPerSession = maxHandlesPerSession,
                sessionCount = targetSessions.size,
                methodHandleCount = methodHandleCount,
                classHandleCount = classHandleCount,
                sessions = targetSessions.values.toList().sortedByDescending { it.createdAt },
            )
        }

    private fun requireWorkspaceService(): WorkspaceService =
        requireNotNull(workspaceService) {
            "workspaceService has not been attached"
        }

    private fun pruneExpiredSessionsLocked() {
        val timeout = idleTimeout ?: return
        if (targetSessions.isEmpty()) return
        val cutoff = nowProvider().minus(timeout)
        val iterator = targetSessions.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.lastAccessedAt.isBefore(cutoff)) {
                iterator.remove()
                clearSessionHandlesLocked(entry.key)
                removedTargetSessions += entry.value
            }
        }
    }

    private fun touchTargetSessionLocked(sessionId: String): TargetSession? {
        val now = nowProvider()
        val session = targetSessions[sessionId] ?: return null
        return session.copy(lastAccessedAt = now).also { touched ->
            targetSessions[sessionId] = touched
        }
    }

    private fun putHandleLocked(ref: SourceBackedHandleRef): String {
        require(targetSessions.containsKey(ref.sessionId)) { "session_id not found: ${ref.sessionId}" }

        handlesByRef[ref]?.let { existingHandle ->
            if (handles[existingHandle] != null) {
                touchHandleLocked(ref.sessionId, existingHandle)
                return existingHandle
            }
            handlesByRef.remove(ref)
        }

        val handle = UUID.randomUUID().toString()
        registerHandleLocked(handle, ref)
        evictOverflowHandlesLocked(ref.sessionId)
        return handle
    }

    private fun evictOverflowSessionsLocked() {
        while (targetSessions.size > maxSessions) {
            val eldest = targetSessions.entries.iterator().next()
            targetSessions.remove(eldest.key)
            clearSessionHandlesLocked(eldest.key)
            removedTargetSessions += eldest.value
        }
    }

    private fun removeTargetSessionLocked(sessionId: String): TargetSession? =
        targetSessions.remove(sessionId)?.also { session ->
            clearSessionHandlesLocked(sessionId)
            sessionHandles.remove(sessionId)
            removedTargetSessions += session
        }

    private fun clearSessionHandlesLocked(sessionId: String) {
        val state = sessionHandles[sessionId] ?: return
        if (state.handles.isEmpty()) return
        state.handles.keys.toList().forEach(::removeHandleLocked)
    }

    private fun touchHandleLocked(sessionId: String, handle: String) {
        sessionHandles[sessionId]?.handles?.get(handle)
    }

    private fun registerHandleLocked(handle: String, ref: SourceBackedHandleRef) {
        handles[handle] = ref
        handlesByRef[ref] = handle
        sessionHandles.getOrPut(ref.sessionId) { SessionHandleState() }.handles[handle] = Unit
        when (ref) {
            is MethodHandleRef -> methodHandleCount += 1
            is ClassHandleRef -> classHandleCount += 1
        }
    }

    private fun evictOverflowHandlesLocked(sessionId: String) {
        val state = sessionHandles[sessionId] ?: return
        while (state.handles.size > maxHandlesPerSession) {
            val eldestHandle = state.handles.entries.iterator().next().key
            removeHandleLocked(eldestHandle)
        }
    }

    private fun removeHandleLocked(handle: String) {
        val ref = handles.remove(handle) ?: return
        handlesByRef.remove(ref)
        sessionHandles[ref.sessionId]?.handles?.remove(handle)
        when (ref) {
            is MethodHandleRef -> methodHandleCount -= 1
            is ClassHandleRef -> classHandleCount -= 1
        }
    }

    private class SessionHandleState {
        val handles = LinkedHashMap<String, Unit>(16, 0.75f, true)
    }
}
