package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerCanvasMetrics
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerViewportSnapshot
import io.github.dexclub.codeview.core.text.Cursor
import kotlin.math.roundToInt

internal fun inputAnchorModifier(
    density: androidx.compose.ui.unit.Density,
    layoutSnapshot: CodeLayoutSnapshot,
    canvasMetrics: CodeViewerCanvasMetrics,
    lineLayoutCache: CodeLineTextLayoutCache,
    viewportSnapshot: CodeViewerViewportSnapshot,
    fieldSelection: androidx.compose.ui.text.TextRange,
    composingOverlay: CodeEditorComposingOverlay?,
): Modifier {
    val widthPx = canvasMetrics.charWidthPx.coerceAtLeast(with(density) { 2.dp.toPx() })
    val heightPx = maxOf(
        canvasMetrics.lineHeightPx + with(density) { 6.dp.toPx() },
        with(density) { 12.dp.toPx() },
    )
    val anchorPlacement = resolveInputAnchorPlacement(
        layoutSnapshot = layoutSnapshot,
        lineLayoutCache = lineLayoutCache,
        fieldSelection = fieldSelection,
        composingOverlay = composingOverlay,
    )
    val lineLayout = lineLayoutCache.layout(anchorPlacement.lineIndex)
    val lineTopPx = anchorPlacement.lineIndex * canvasMetrics.lineHeightPx - viewportSnapshot.verticalScrollPx
    val contentTopPx = lineTopPx + (
        (canvasMetrics.lineHeightPx - lineLayout.size.height.toFloat()) / 2f
    ).coerceAtLeast(0f)
    val rawXPx = anchorPlacement.xPx - viewportSnapshot.horizontalScrollPx
    val rawYPx = contentTopPx
    val xPx = rawXPx.coerceIn(0f, (viewportSnapshot.viewportWidthPx - widthPx).coerceAtLeast(0f))
    val yPx = rawYPx.coerceIn(0f, (viewportSnapshot.viewportHeightPx - heightPx).coerceAtLeast(0f))

    return Modifier
        .offset {
            IntOffset(
                x = xPx.roundToInt(),
                y = yPx.roundToInt(),
            )
        }
        .requiredSize(
            width = with(density) { widthPx.toDp() },
            height = with(density) { heightPx.toDp() },
        )
}

private data class InputAnchorPlacement(
    val lineIndex: Int,
    val xPx: Float,
)

private fun resolveInputAnchorPlacement(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    fieldSelection: androidx.compose.ui.text.TextRange,
    composingOverlay: CodeEditorComposingOverlay?,
): InputAnchorPlacement {
    val overlay = composingOverlay
    if (overlay != null) {
        val anchorOffset = overlay.anchorSelection.normalizedStart
        val anchorPosition = layoutSnapshot.offsetToPosition(anchorOffset)
        val overlayLayout = lineLayoutCache.plainTextLayout(overlay.imeFieldValue.text)
        val caretOffset = overlay.imeFieldValue.selection.end.coerceIn(0, overlay.imeFieldValue.text.length)
        return InputAnchorPlacement(
            lineIndex = anchorPosition.lineIndex,
            xPx = lineLayoutCache.columnX(anchorPosition.lineIndex, anchorPosition.columnIndex) +
                    overlayLayout.getCursorRect(caretOffset).left,
        )
    }

    val fallbackCursor = layoutSnapshot.cursorFromSelection(fieldSelection.toCodeSelection()) ?: Cursor(0, 0)
    return InputAnchorPlacement(
        lineIndex = fallbackCursor.line,
        xPx = lineLayoutCache.columnX(fallbackCursor.line, fallbackCursor.offset),
    )
}
