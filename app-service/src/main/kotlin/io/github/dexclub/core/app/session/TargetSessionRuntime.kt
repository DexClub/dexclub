package io.github.dexclub.core.app.session

import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService

class TargetSessionRuntime(
    workspaceService: WorkspaceService,
    val sessionService: TargetSessionService = TargetSessionService(workspaceService = workspaceService),
    private val dexContextRegistry: DexContextRegistry,
) : AutoCloseable {
    private val workspaceResolver = TargetWorkspaceResolver(workspaceService, sessionService)

    init {
        sessionService.attachWorkspaceService(workspaceService)
    }

    fun openTargetSession(input: String): TargetSession =
        sessionService.openTargetSession(input).also { session ->
            dexContextRegistry.retain(session)
            releaseRemovedTargetSessions()
        }

    fun listTargetSessions(): List<TargetSession> =
        sessionService.listTargetSessions().also { releaseRemovedTargetSessions() }

    fun getTargetSession(sessionId: String): TargetSession? =
        sessionService.getTargetSession(sessionId).also { releaseRemovedTargetSessions() }

    fun closeTargetSession(sessionId: String): TargetSession? =
        sessionService.closeTargetSession(sessionId).also { releaseRemovedTargetSessions() }

    fun refreshTargetSession(sessionId: String): TargetSession? =
        sessionService.refreshTargetSession(sessionId).also { releaseRemovedTargetSessions() }

    fun snapshot(): SessionStoreSnapshot =
        sessionService.snapshot().also { releaseRemovedTargetSessions() }

    fun resolveExecutionContext(
        sessionId: String? = null,
        workdir: String? = null,
    ): TargetExecutionContext =
        workspaceResolver.resolve(
            sessionId = sessionId,
            workdir = workdir,
        )

    fun acquireDexContextForExecutionContext(context: TargetExecutionContext): DexContextLease =
        context.session?.let(dexContextRegistry::acquireForSession)
            ?: dexContextRegistry.acquireForWorkdir(context.workspace)

    fun acquireDexContextForSession(session: TargetSession): DexContextLease =
        dexContextRegistry.acquireForSession(session)

    fun acquireDexContextForWorkspace(workspace: WorkspaceContext): DexContextLease =
        dexContextRegistry.acquireForWorkdir(workspace)

    override fun close() {
        dexContextRegistry.closeAll()
    }

    private fun releaseRemovedTargetSessions() {
        sessionService.drainRemovedTargetSessions().forEach(dexContextRegistry::release)
    }
}
