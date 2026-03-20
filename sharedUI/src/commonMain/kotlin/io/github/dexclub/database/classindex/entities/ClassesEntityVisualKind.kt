package io.github.dexclub.database.classindex.entities

import io.github.dexclub.core.workspace.ClassVisualKind
import io.github.dexclub.core.workspace.resolveClassVisualKind

fun ClassesEntity.visualKind(): ClassVisualKind {
    return resolveClassVisualKind(modifiers)
}
