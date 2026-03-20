package io.github.dexclub.data.classindex

import io.github.dexclub.core.workspace.WorkspaceIndexedClassRecord
import io.github.dexclub.database.classindex.entities.ClassesEntity
import io.github.dexclub.database.classindex.entities.visualKind

internal fun ClassesEntity.toRecord(): WorkspaceIndexedClassRecord {
    return WorkspaceIndexedClassRecord(
        className = name,
        displayName = displayName,
        signature = signature,
        dexAbsolutePath = dexAbsolutePath,
        modifiers = modifiers,
        classVisualKind = visualKind(),
    )
}

internal fun WorkspaceIndexedClassRecord.toEntity(): ClassesEntity {
    return ClassesEntity(
        name = className,
        signature = signature,
        dexAbsolutePath = dexAbsolutePath,
        modifiers = modifiers,
    )
}
