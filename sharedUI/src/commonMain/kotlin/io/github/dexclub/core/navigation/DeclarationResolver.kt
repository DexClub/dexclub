package io.github.dexclub.core.navigation

import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.lang.SemanticNode

data class NavigateRequestContext(
    val annotationHit: CodeAnnotationHit,
    val semanticNode: SemanticNode,
    val tabId: String,
    val paneIndex: Int,
    val activeKind: String,
)

data class JumpTarget(
    val targetClassName: String,
    val targetKind: String,
    val targetLine: Int = 0,
    val targetOffset: Int = 0,
    val reason: String = "",
)

sealed interface JumpResolveResult {
    data class Resolved(val target: JumpTarget) : JumpResolveResult
    data class NotFound(val reason: String) : JumpResolveResult
    data class Unsupported(val reason: String) : JumpResolveResult
    data class Error(val reason: String) : JumpResolveResult
}

data class ResolverEnv(
    val workspaceId: Long?,
    val workspaceName: String,
    val activeLines: List<String>,
    val sourceClassName: String,
)

interface DeclarationResolver {
    suspend fun resolve(
        context: NavigateRequestContext,
        env: ResolverEnv,
    ): JumpResolveResult
}
