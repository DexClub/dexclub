package io.github.dexclub.cli

import io.github.dexclub.core.app.contract.WorkspaceContext
import io.github.dexclub.core.app.contract.WorkspaceRef
import io.github.dexclub.core.app.session.WorkspaceRuntime

internal class CliTargetWorkspaceRuntime(
    private val workdirResolver: WorkdirResolver,
    private val workspaceRuntime: WorkspaceRuntime,
) {
    fun resolveWorkspaceRef(workdir: String?): WorkspaceRef =
        workdirResolver.resolve(workdir)

    fun openWorkspace(workdir: String?): WorkspaceContext =
        workspaceRuntime.open(resolveWorkspaceRef(workdir).workdir)
}

