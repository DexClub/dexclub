package io.github.dexclub.app.scene.workspace

import io.github.dexclub.node.ClassTreeNode

sealed interface WorkspaceSideSelection {
    val persistedType: String

    val persistedKey: String

    val stableKey: String
        get() = "$persistedType:$persistedKey"

    data class Package(val fullPath: String) : WorkspaceSideSelection {
        override val persistedType: String
            get() = TYPE_PACKAGE

        override val persistedKey: String
            get() = fullPath
    }

    data class Class(val className: String) : WorkspaceSideSelection {
        override val persistedType: String
            get() = TYPE_CLASS

        override val persistedKey: String
            get() = className
    }

    companion object {
        private const val TYPE_PACKAGE = "package"
        private const val TYPE_CLASS = "class"

        fun fromPersisted(type: String?, key: String?): WorkspaceSideSelection? {
            if (type.isNullOrBlank() || key.isNullOrBlank()) return null
            return when (type) {
                TYPE_PACKAGE -> Package(key)
                TYPE_CLASS -> Class(key)
                else -> null
            }
        }
    }
}

fun ClassTreeNode.toWorkspaceSideSelection(): WorkspaceSideSelection {
    return when (this) {
        is ClassTreeNode.PackageNode -> WorkspaceSideSelection.Package(fullPath)
        is ClassTreeNode.ClassNode -> WorkspaceSideSelection.Class(className)
    }
}

fun ClassTreeNode.workspaceSideStableKey(): String = toWorkspaceSideSelection().stableKey

fun WorkspaceSideSelection.matches(node: ClassTreeNode): Boolean {
    return this == node.toWorkspaceSideSelection()
}
