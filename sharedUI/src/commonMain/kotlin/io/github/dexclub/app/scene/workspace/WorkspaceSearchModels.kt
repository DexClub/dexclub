package io.github.dexclub.app.scene.workspace

import io.github.dexclub.core.search.ClassSearchHit
import io.github.dexclub.core.search.StringSearchHit
import io.github.dexclub.core.workspace.ClassVisualKind

data class WorkspaceSearchResponse<T>(
    val items: List<T>,
)

data class WorkspaceClassSearchResult(
    val className: String,
    val classVisualKind: ClassVisualKind,
    val descriptor: String,
)

data class WorkspaceStringSearchResult(
    val className: String,
    val classVisualKind: ClassVisualKind,
    val methodDescriptor: String,
    val methodName: String,
    val methodDisplaySignature: String,
)

fun ClassSearchHit.toWorkspaceClassSearchResult(): WorkspaceClassSearchResult {
    return WorkspaceClassSearchResult(
        className = className,
        classVisualKind = classVisualKind,
        descriptor = descriptor,
    )
}

fun StringSearchHit.toWorkspaceStringSearchResult(): WorkspaceStringSearchResult {
    return WorkspaceStringSearchResult(
        className = className,
        classVisualKind = classVisualKind,
        methodDescriptor = methodDescriptor,
        methodName = methodName,
        methodDisplaySignature = methodDisplaySignature,
    )
}
