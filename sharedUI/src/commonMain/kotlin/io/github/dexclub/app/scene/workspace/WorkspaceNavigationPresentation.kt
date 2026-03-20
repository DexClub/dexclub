package io.github.dexclub.app.scene.workspace

import io.github.dexclub.app.model.OpenTabMode
import io.github.dexclub.app.model.OpenTabUiModel
import io.github.dexclub.core.navigation.NavigateRequestContext
import io.github.dexclub.lang.SemanticNode

internal data class WorkspaceNavigationRevealPlan(
    val paneIndex: Int,
    val activeKind: String,
    val revealKind: String?,
)

internal fun OpenTabUiModel.resolveNavigationTargetKinds(
    defaultKind: String,
): List<String> {
    return if (mode == OpenTabMode.MIXED) {
        requiredKinds
    } else {
        listOf(defaultKind)
    }
}

internal fun OpenTabUiModel.resolveNavigationRevealPlan(
    preferredKind: String,
    fallbackPaneIndex: Int,
    revealAllKindsInMixedMode: Boolean,
): WorkspaceNavigationRevealPlan {
    val paneIndex = panes
        .firstOrNull { pane -> pane.kind == preferredKind }
        ?.paneIndex
        ?: fallbackPaneIndex

    return WorkspaceNavigationRevealPlan(
        paneIndex = paneIndex,
        activeKind = preferredKind,
        revealKind = if (mode == OpenTabMode.MIXED && revealAllKindsInMixedMode) {
            null
        } else {
            preferredKind
        },
    )
}

internal fun SemanticNode.hasNavigationIdentity(): Boolean {
    return name.isNotEmpty() || owner.isNotEmpty() || descriptor.isNotEmpty()
}

internal fun NavigateRequestContext.navigationMissDebugMessage(): String {
    return "节点命中失败: lang=${semanticNode.lang}, kind=${semanticNode.kind}, " +
            "name=${semanticNode.name}, owner=${semanticNode.owner}, descriptor=${semanticNode.descriptor}"
}
