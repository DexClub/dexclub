package io.github.dexclub.core.workspace

data class WorkspaceRecord(
    val id: Long = 0,
    val name: String,
    val absolutePath: String,
    val displayPath: String,
    val dexsAbsolutePathList: List<String>,
    val validDexs: Int,
)
