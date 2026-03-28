package io.github.dexclub.app.scene.workspace

import io.github.dexclub.core.DexEngine
import io.github.dexclub.core.workspace.WorkspaceIndexClassEntry

internal fun DexEngine.workspaceIndexEntries(): Sequence<WorkspaceIndexClassEntry> {
    return indexedClasses().map { indexedClass ->
        WorkspaceIndexClassEntry(
            dexAbsolutePath = indexedClass.dexAbsolutePath,
            signature = indexedClass.signature,
            modifiers = indexedClass.modifiers,
        )
    }
}
