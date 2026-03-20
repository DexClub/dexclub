package io.github.dexclub.data.classindex

import io.github.dexclub.core.workspace.WorkspaceClassIndexRepository
import io.github.dexclub.core.workspace.WorkspaceIndexedClassRecord
import io.github.dexclub.database.classindex.ClassIndexDatabase

class RoomWorkspaceClassIndexRepository(
    private val databaseDir: String,
) : WorkspaceClassIndexRepository {
    private val database: ClassIndexDatabase by lazy {
        ClassIndexDatabase.open(databaseDir)
    }

    override suspend fun count(): Int {
        return database.classesDao().count()
    }

    override suspend fun clear() {
        database.classesDao().clear()
    }

    override suspend fun insertAll(records: List<WorkspaceIndexedClassRecord>) {
        database.classesDao().insertAll(records.map(WorkspaceIndexedClassRecord::toEntity))
    }

    override suspend fun getAll(): List<WorkspaceIndexedClassRecord> {
        return database.classesDao().getAll().map { entity -> entity.toRecord() }
    }

    override suspend fun findByName(name: String): WorkspaceIndexedClassRecord? {
        return database.classesDao().findByName(name)?.toRecord()
    }

    override suspend fun findByNames(names: List<String>): List<WorkspaceIndexedClassRecord> {
        return database.classesDao().findByNames(names).map { entity -> entity.toRecord() }
    }

    override fun close() {
        ClassIndexDatabase.close(databaseDir)
    }
}
