package io.github.dexclub.data.workspace

import io.github.xxfast.kstore.KStore

internal const val WORKSPACES_FILE_NAME = "workspaces.json"

internal expect fun createWorkspaceStore(): KStore<WorkspaceStoreSnapshot>
