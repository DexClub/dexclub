package io.github.dexclub.core.workspace

interface WorkspaceRepository {
    suspend fun getAll(): List<WorkspaceRecord>

    suspend fun getById(id: Long): WorkspaceRecord?

    suspend fun insert(record: WorkspaceRecord): WorkspaceRecord

    suspend fun updateLastOpenedAt(id: Long, lastOpenedAt: Long)

    suspend fun deleteById(id: Long)

    fun close()
}
