package io.github.dexclub.codeview.compose.internal.viewer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import io.github.dexclub.codeview.compose.CodeViewerInteractionOptions
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.codeview.core.annotation.CodeInteractionTrigger
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.core.document.DocumentRevision

internal fun Modifier.annotationInteractionModifier(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    documentKey: DocumentId,
    documentRevision: DocumentRevision,
    lineHeightPx: Float,
    interactionOptions: CodeViewerInteractionOptions,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)?,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)?,
    enablePrimaryClick: Boolean = true,
    enableLongPressContextMenu: Boolean = true,
    enableSecondaryClickContextMenu: Boolean = true,
): Modifier {
    var modifier = this

    if (enableSecondaryClickContextMenu) {
        modifier = modifier.pointerInput(
            layoutSnapshot,
            documentKey,
            documentRevision,
            interactionOptions.annotationTag,
        ) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type != PointerEventType.Press || !event.buttons.isSecondaryPressed) {
                        continue
                    }

                    val position = event.changes.firstOrNull()?.position ?: continue
                    val hit = buildAnnotationHit(
                        layoutSnapshot = layoutSnapshot,
                        lineLayoutCache = lineLayoutCache,
                        documentKey = documentKey,
                        documentRevision = documentRevision,
                        position = position,
                        lineHeightPx = lineHeightPx,
                        trigger = CodeInteractionTrigger.ContextMenu,
                    )
                    onContextMenu?.invoke(hit, position)
                }
            }
        }
    }

    if (enablePrimaryClick || enableLongPressContextMenu) {
        modifier = modifier.pointerInput(
            layoutSnapshot,
            documentKey,
            documentRevision,
            interactionOptions.annotationTag,
        ) {
            detectTapGestures(
                onTap = if (enablePrimaryClick) {
                    { offset ->
                        val hit = buildAnnotationHit(
                            layoutSnapshot = layoutSnapshot,
                            lineLayoutCache = lineLayoutCache,
                            documentKey = documentKey,
                            documentRevision = documentRevision,
                            position = offset,
                            lineHeightPx = lineHeightPx,
                            trigger = CodeInteractionTrigger.PrimaryClick,
                        )
                        if (hit != null) {
                            onAnnotationHit?.invoke(hit)
                        }
                    }
                } else {
                    null
                },
                onLongPress = if (enableLongPressContextMenu) {
                    { offset ->
                        val hit = buildAnnotationHit(
                            layoutSnapshot = layoutSnapshot,
                            lineLayoutCache = lineLayoutCache,
                            documentKey = documentKey,
                            documentRevision = documentRevision,
                            position = offset,
                            lineHeightPx = lineHeightPx,
                            trigger = CodeInteractionTrigger.ContextMenu,
                        )
                        onContextMenu?.invoke(hit, offset)
                    }
                } else {
                    null
                },
            )
        }
    }

    return modifier
}

private fun buildAnnotationHit(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    documentKey: DocumentId,
    documentRevision: DocumentRevision,
    position: Offset,
    lineHeightPx: Float,
    trigger: CodeInteractionTrigger,
): CodeAnnotationHit? {
    if (lineHeightPx <= 0f || position.x < 0f || position.y < 0f) return null
    val contentHeightPx = layoutSnapshot.lineCount * lineHeightPx
    if (position.y >= contentHeightPx) return null

    val lineIndex = (position.y / lineHeightPx).toInt().coerceIn(0, layoutSnapshot.lineCount - 1)
    val lineOffset = lineLayoutCache.offsetForPosition(
        lineIndex = lineIndex,
        xPx = position.x,
        clampToLineEnd = false,
    ) ?: return null
    val offset = layoutSnapshot.positionToOffset(lineIndex, lineOffset)
    val annotation = layoutSnapshot.findAnnotationAtOffset(offset) ?: return null

    return CodeAnnotationHit(
        annotation = annotation,
        range = annotation.range,
        trigger = trigger,
        documentId = documentKey,
        revision = documentRevision,
    )
}
