package io.github.dexclub.core.app.session

import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceRef
import io.github.dexclub.core.api.workspace.WorkspaceService

class WorkspaceRuntime(
    private val workspaceService: WorkspaceService,
) {
    fun open(workdir: String): WorkspaceContext {
        val normalizedWorkdir = workdir.trim().ifEmpty { throw IllegalArgumentException("workdir is required") }
        return workspaceService.open(WorkspaceRef(normalizedWorkdir))
    }
}
