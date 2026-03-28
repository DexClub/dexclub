package io.github.dexclub.core.workspace

import io.github.dexclub.node.ClassTreeNode

enum class WorkspaceIndexLoadMode {
    Loaded,
    Rebuilt,
}

data class WorkspaceIndexState(
    val classTreeRoot: ClassTreeNode,
    val classCount: Int,
    val mode: WorkspaceIndexLoadMode,
)

class WorkspaceIndexService(
    private val classIndexRepository: WorkspaceClassIndexRepository,
) {
    suspend fun countIndexedClasses(): Int {
        return classIndexRepository.count()
    }

    suspend fun loadClassTree(
        onProgress: (String) -> Unit = {},
    ): WorkspaceIndexState {
        onProgress("结构整理中..")
        val classes = classIndexRepository.getAll()
        return WorkspaceIndexState(
            classTreeRoot = ClassTreeNode.parse(classes.map(WorkspaceIndexedClassRecord::toClassTreeClassItem)),
            classCount = classes.size,
            mode = WorkspaceIndexLoadMode.Loaded,
        )
    }

    suspend fun rebuildClassTree(
        indexedClasses: Sequence<WorkspaceIndexClassEntry>,
        onProgress: (String) -> Unit = {},
    ): WorkspaceIndexState {
        classIndexRepository.clear()

        val classes = mutableListOf<WorkspaceIndexedClassRecord>()
        for (indexedClass in indexedClasses) {
            onProgress("载入Class: ${indexedClass.signature}")
            classes += indexedClass.toWorkspaceIndexedClassRecord()
            if (classes.size >= INSERT_BATCH_SIZE) {
                classIndexRepository.insertAll(classes)
                classes.clear()
            }
        }

        if (classes.isNotEmpty()) {
            classIndexRepository.insertAll(classes)
        }

        onProgress("结构整理中..")
        val storedClasses = classIndexRepository.getAll()
        return WorkspaceIndexState(
            classTreeRoot = ClassTreeNode.parse(storedClasses.map(WorkspaceIndexedClassRecord::toClassTreeClassItem)),
            classCount = storedClasses.size,
            mode = WorkspaceIndexLoadMode.Rebuilt,
        )
    }

    suspend fun findByName(name: String): WorkspaceIndexedClassRecord? {
        return classIndexRepository.findByName(name)
    }

    suspend fun findByNames(names: List<String>): List<WorkspaceIndexedClassRecord> {
        return classIndexRepository.findByNames(names)
    }

    fun close() {
        classIndexRepository.close()
    }

    companion object {
        private const val INSERT_BATCH_SIZE = 500
    }
}
