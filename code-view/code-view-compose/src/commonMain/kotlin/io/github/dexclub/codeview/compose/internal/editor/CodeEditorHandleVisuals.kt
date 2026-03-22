package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerCanvasMetrics
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerViewportSnapshot

internal enum class SelectionHandleKind {
    Start,
    End,
    Cursor,
}

internal data class SelectionHandlePlacement(
    val touchLeftPx: Float,
    val touchTopPx: Float,
    val visualLeftPx: Float,
    val visualTopPx: Float,
    val visualWidthDp: Dp,
    val visualHeightDp: Dp,
)

private data class HandleVisualMetrics(
    val widthDp: Dp,
    val heightDp: Dp,
    val anchorOffsetPx: Float,
)

internal fun resolveSelectionHandlePlacement(
    kind: SelectionHandleKind,
    density: Density,
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    canvasMetrics: CodeViewerCanvasMetrics,
    viewportSnapshot: CodeViewerViewportSnapshot,
    textOffset: Int,
): SelectionHandlePlacement {
    val cursor = layoutSnapshot.offsetToCursor(textOffset)
    val visualMetrics = resolveHandleVisualMetrics(
        kind = kind,
        density = density,
    )
    val touchWidthPx = with(density) { SELECTION_HANDLE_TOUCH_WIDTH_DP.dp.toPx() }
    val visualWidthPx = with(density) { visualMetrics.widthDp.toPx() }
    val horizontalPaddingPx = (touchWidthPx - visualWidthPx) / 2f
    val xPx = lineLayoutCache.columnX(cursor.line, cursor.offset) - viewportSnapshot.horizontalScrollPx
    val yPx = (cursor.line + 1) * canvasMetrics.lineHeightPx - viewportSnapshot.verticalScrollPx
    val visualLeftPx = xPx - visualMetrics.anchorOffsetPx
    return SelectionHandlePlacement(
        touchLeftPx = visualLeftPx - horizontalPaddingPx,
        touchTopPx = yPx - with(density) { SELECTION_HANDLE_TOUCH_TOP_INSET_DP.dp.toPx() },
        visualLeftPx = horizontalPaddingPx,
        visualTopPx = with(density) { SELECTION_HANDLE_TOUCH_TOP_INSET_DP.dp.toPx() },
        visualWidthDp = visualMetrics.widthDp,
        visualHeightDp = visualMetrics.heightDp,
    )
}

private fun resolveHandleVisualMetrics(
    kind: SelectionHandleKind,
    density: Density,
): HandleVisualMetrics {
    val widthDp = when (kind) {
        SelectionHandleKind.Start,
        SelectionHandleKind.End -> SELECTION_HANDLE_VISUAL_SIZE_DP.dp
        SelectionHandleKind.Cursor -> CURSOR_HANDLE_WIDTH_DP
    }
    val heightDp = when (kind) {
        SelectionHandleKind.Start,
        SelectionHandleKind.End -> SELECTION_HANDLE_VISUAL_SIZE_DP.dp
        SelectionHandleKind.Cursor -> CURSOR_HANDLE_HEIGHT_DP
    }
    val widthPx = with(density) { widthDp.toPx() }
    val anchorOffsetPx = when (kind) {
        SelectionHandleKind.Start -> widthPx
        SelectionHandleKind.End -> 0f
        SelectionHandleKind.Cursor -> widthPx / 2f
    }
    return HandleVisualMetrics(
        widthDp = widthDp,
        heightDp = heightDp,
        anchorOffsetPx = anchorOffsetPx,
    )
}

internal fun DrawScope.drawHandleIcon(
    kind: SelectionHandleKind,
) {
    val radius = size.width / 2f
    when (kind) {
        SelectionHandleKind.Start -> {
            scale(
                scaleX = -1f,
                scaleY = 1f,
                pivot = center,
            ) {
                drawSelectionHandleBase(radius)
            }
        }

        SelectionHandleKind.End -> {
            drawSelectionHandleBase(radius)
        }

        // 对齐 Compose foundation AndroidCursorHandle：基础 handle 旋转 45 度。
        SelectionHandleKind.Cursor -> {
            drawCursorHandleBase(radius)
        }
    }
}

private fun DrawScope.drawSelectionHandleBase(
    radius: Float,
) {
    drawRect(
        color = SELECTION_HANDLE_COLOR,
        topLeft = Offset.Zero,
        size = Size(radius, radius),
    )
    drawCircle(
        color = SELECTION_HANDLE_COLOR,
        radius = radius,
        center = Offset(radius, radius),
    )
}

private fun DrawScope.drawCursorHandleBase(
    radius: Float,
) {
    withTransform({
        translate(left = radius)
        rotate(
            degrees = CURSOR_HANDLE_ROTATION_DEGREES,
            pivot = Offset.Zero,
        )
    }) {
        drawSelectionHandleBase(radius)
    }
}

private val SELECTION_HANDLE_COLOR: Color = Color(0xFF1A73E8)
internal const val SELECTION_HANDLE_TOUCH_WIDTH_DP: Int = 36
internal const val SELECTION_HANDLE_TOUCH_HEIGHT_DP: Int = 44
private const val SELECTION_HANDLE_TOUCH_TOP_INSET_DP: Int = 4
private const val SELECTION_HANDLE_VISUAL_SIZE_DP: Int = 22
private const val CURSOR_HANDLE_ROTATION_DEGREES: Float = 45f
private const val CURSOR_HANDLE_SQRT_2: Float = 1.41421356f
private val CURSOR_HANDLE_HEIGHT_DP: Dp = 25.dp
private val CURSOR_HANDLE_WIDTH_DP: Dp = CURSOR_HANDLE_HEIGHT_DP * 2f / (1f + CURSOR_HANDLE_SQRT_2)
