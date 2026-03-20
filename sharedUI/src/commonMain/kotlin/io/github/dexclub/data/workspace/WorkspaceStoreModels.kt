package io.github.dexclub.data.workspace

import kotlinx.serialization.Serializable

@Serializable
internal data class WorkspaceStoreSnapshot(
    val nextId: Long = 1,
    val workspaces: List<WorkspaceStoreRecord> = emptyList(),
)


@Serializable
internal data class WorkspaceStoreRecord(
    val id: Long,
    val name: String,
    val absolutePath: String,
    val displayPath: String,
    val dexsAbsolutePathList: List<String>,
    val validDexs: Int,
)
