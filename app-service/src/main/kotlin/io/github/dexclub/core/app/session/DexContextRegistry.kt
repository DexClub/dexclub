package io.github.dexclub.core.app.session

import io.github.dexclub.core.api.workspace.WorkspaceContext
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Binds a shared target dex context to runtime sessions instead of splitting by dex file or limiting dex count.
 */
class DexContextRegistry(
    private val releaseDexContext: (WorkspaceContext) -> Unit,
) {
    private val lock = ReentrantLock()
    private val entries = HashMap<TargetContextScope, ContextEntry>()
    private val scopesBySessionId = HashMap<String, TargetContextScope>()

    fun retain(session: TargetSession) {
        lock.withLock {
            val scope = session.workspace.toTargetContextScope()
            val previousScope = scopesBySessionId.put(session.sessionId, scope)
            if (previousScope != null && previousScope != scope) {
                releaseSessionLocked(session.sessionId, previousScope)
            }
            entries.getOrPut(scope) { ContextEntry(session.workspace) }.sessionIds += session.sessionId
        }
    }

    fun release(session: TargetSession) {
        lock.withLock {
            val scope = scopesBySessionId.remove(session.sessionId) ?: return
            releaseSessionLocked(session.sessionId, scope)
        }
    }

    fun acquireForSession(session: TargetSession): DexContextLease =
        lock.withLock {
            val scope = scopesBySessionId[session.sessionId] ?: session.workspace.toTargetContextScope().also { key ->
                scopesBySessionId[session.sessionId] = key
                entries.getOrPut(key) { ContextEntry(session.workspace) }.sessionIds += session.sessionId
            }
            val entry = entries.getOrPut(scope) { ContextEntry(session.workspace) }
            entry.inFlightRequests += 1
            DexContextLease { releaseRequest(scope) }
        }

    fun acquireForWorkdir(workspace: WorkspaceContext): DexContextLease =
        lock.withLock {
            val scope = workspace.toTargetContextScope()
            val entry = entries.getOrPut(scope) { ContextEntry(workspace) }
            entry.inFlightRequests += 1
            DexContextLease { releaseRequest(scope) }
        }

    fun closeAll() {
        val workspaces = lock.withLock {
            entries.values.map(ContextEntry::workspace).also {
                entries.clear()
                scopesBySessionId.clear()
            }
        }
        workspaces.forEach(releaseDexContext)
    }

    private fun releaseRequest(scope: TargetContextScope) {
        lock.withLock {
            val entry = entries[scope] ?: return
            check(entry.inFlightRequests > 0) { "dex context request count underflow" }
            entry.inFlightRequests -= 1
            releaseEntryIfUnusedLocked(scope, entry)
        }
    }

    private fun releaseSessionLocked(sessionId: String, scope: TargetContextScope) {
        val entry = entries[scope] ?: return
        entry.sessionIds -= sessionId
        releaseEntryIfUnusedLocked(scope, entry)
    }

    private fun releaseEntryIfUnusedLocked(scope: TargetContextScope, entry: ContextEntry) {
        if (entry.sessionIds.isNotEmpty() || entry.inFlightRequests > 0) return
        entries.remove(scope, entry)
        releaseDexContext(entry.workspace)
    }

    private data class TargetContextScope(
        val workdir: String,
        val activeTargetId: String,
    )

    private data class ContextEntry(
        val workspace: WorkspaceContext,
        val sessionIds: MutableSet<String> = linkedSetOf(),
        var inFlightRequests: Int = 0,
    )

    private fun WorkspaceContext.toTargetContextScope() =
        TargetContextScope(workdir = workdir, activeTargetId = activeTargetId)
}

class DexContextLease(
    private val release: () -> Unit,
) : AutoCloseable {
    private var closed = false

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        release()
    }
}
