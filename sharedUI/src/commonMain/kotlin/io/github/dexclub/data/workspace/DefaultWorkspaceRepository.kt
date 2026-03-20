package io.github.dexclub.data.workspace

import io.github.dexclub.core.workspace.WorkspaceRecord
import io.github.dexclub.core.workspace.WorkspaceRepository
import io.github.xxfast.kstore.KStore

class DefaultWorkspaceRepository internal constructor(
    private val store: KStore<WorkspaceStoreSnapshot>,
) : WorkspaceRepository {
    constructor() : this(sharedStore)

    override suspend fun getAll(): List<WorkspaceRecord> {
        return loadSnapshot().workspaces.map { item -> item.toRecord() }
    }

    override suspend fun getById(id: Long): WorkspaceRecord? {
        return loadSnapshot().workspaces.firstOrNull { item -> item.id == id }?.toRecord()
    }

    override suspend fun insert(record: WorkspaceRecord): WorkspaceRecord {
        var insertedRecord: WorkspaceRecord? = null
        store.update { snapshot ->
            val currentSnapshot = snapshot ?: WorkspaceStoreSnapshot()
            val nextId = currentSnapshot.nextId.coerceAtLeast(1)
            val assignedId = record.id.takeIf { it > 0 } ?: nextId
            require(currentSnapshot.workspaces.none { item -> item.id == assignedId }) { "工作区 id 已存在: $assignedId" }

            val storedRecord = record.toStoredRecord(id = assignedId)
            insertedRecord = storedRecord.toRecord()
            currentSnapshot.copy(
                nextId = maxOf(nextId, assignedId + 1),
                workspaces = currentSnapshot.workspaces + storedRecord,
            )
        }
        return requireNotNull(insertedRecord) { "插入工作区失败: 未生成记录" }
    }

    override suspend fun deleteById(id: Long) {
        store.update { snapshot ->
            val currentSnapshot = snapshot ?: WorkspaceStoreSnapshot()
            currentSnapshot.copy(
                workspaces = currentSnapshot.workspaces.filterNot { item -> item.id == id },
            )
        }
    }

    override fun close() {
        store.close()
    }

    private suspend fun loadSnapshot(): WorkspaceStoreSnapshot {
        return store.get() ?: WorkspaceStoreSnapshot()
    }

    private fun WorkspaceStoreRecord.toRecord(): WorkspaceRecord {
        return WorkspaceRecord(
            id = id,
            name = name,
            absolutePath = absolutePath,
            displayPath = displayPath,
            dexsAbsolutePathList = dexsAbsolutePathList,
            validDexs = validDexs,
        )
    }

    private fun WorkspaceRecord.toStoredRecord(id: Long): WorkspaceStoreRecord {
        return WorkspaceStoreRecord(
            id = id,
            name = name,
            absolutePath = absolutePath,
            displayPath = displayPath,
            dexsAbsolutePathList = dexsAbsolutePathList,
            validDexs = validDexs,
        )
    }

    private companion object {
        private val sharedStore: KStore<WorkspaceStoreSnapshot> by lazy(::createWorkspaceStore)
    }
}
