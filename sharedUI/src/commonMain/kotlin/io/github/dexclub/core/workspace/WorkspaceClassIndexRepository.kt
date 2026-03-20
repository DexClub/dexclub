package io.github.dexclub.core.workspace

interface WorkspaceClassIndexRepository {
    suspend fun count(): Int

    suspend fun clear()

    suspend fun insertAll(records: List<WorkspaceIndexedClassRecord>)

    suspend fun getAll(): List<WorkspaceIndexedClassRecord>

    suspend fun findByName(name: String): WorkspaceIndexedClassRecord?

    suspend fun findByNames(names: List<String>): List<WorkspaceIndexedClassRecord>

    fun close()
}
