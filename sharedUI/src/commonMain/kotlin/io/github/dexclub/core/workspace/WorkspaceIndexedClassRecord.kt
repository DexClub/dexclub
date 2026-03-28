package io.github.dexclub.core.workspace

import io.github.dexclub.node.ClassTreeNode
import io.github.dexclub.utils.SignatureUtils

data class WorkspaceIndexedClassRecord(
    val className: String,
    val displayName: String,
    val signature: String,
    val dexAbsolutePath: String,
    val modifiers: Int,
    val classVisualKind: ClassVisualKind,
)

internal fun WorkspaceIndexClassEntry.toWorkspaceIndexedClassRecord(): WorkspaceIndexedClassRecord {
    val className = SignatureUtils.typeName(signature)
    return WorkspaceIndexedClassRecord(
        className = className,
        displayName = className.substringAfterLast('.'),
        signature = signature,
        dexAbsolutePath = dexAbsolutePath,
        modifiers = modifiers,
        classVisualKind = resolveClassVisualKind(modifiers),
    )
}

internal fun WorkspaceIndexedClassRecord.toClassTreeClassItem(): ClassTreeNode.ClassTreeClassItem {
    return ClassTreeNode.ClassTreeClassItem(
        className = className,
        displayName = displayName,
        classVisualKind = classVisualKind,
    )
}
