package io.github.dexclub.app.scene.workspace

import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.core.navigation.NavigateRequestContext
import io.github.dexclub.lang.SemanticNodeCodec

internal fun toNavigateRequestContext(
    annotationHit: CodeAnnotationHit,
    tabId: String,
    paneIndex: Int,
    activeKind: String,
): NavigateRequestContext? {
    val semanticNode = SemanticNodeCodec.decode(annotationHit.annotation.payload) ?: return null
    return NavigateRequestContext(
        annotationHit = annotationHit,
        semanticNode = semanticNode,
        tabId = tabId,
        paneIndex = paneIndex,
        activeKind = activeKind,
    )
}
