package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerCanvasMetrics
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerViewportSnapshot
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.roundToInt

@Composable
internal fun CodeEditorTouchSelectionHandles(
    density: Density,
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    canvasMetrics: CodeViewerCanvasMetrics,
    viewportSnapshot: CodeViewerViewportSnapshot,
    selection: TextRange,
    onSelectionChange: (TextRange) -> Unit,
    onHandleInteractionStart: () -> Unit,
) {
    if (selection.collapsed) return

    val normalizedStart = selection.normalizedStart
    val normalizedEnd = selection.normalizedEnd
    val startPlacement = resolveSelectionHandlePlacement(
        kind = SelectionHandleKind.Start,
        density = density,
        layoutSnapshot = layoutSnapshot,
        lineLayoutCache = lineLayoutCache,
        canvasMetrics = canvasMetrics,
        viewportSnapshot = viewportSnapshot,
        textOffset = normalizedStart,
    )
    val endPlacement = resolveSelectionHandlePlacement(
        kind = SelectionHandleKind.End,
        density = density,
        layoutSnapshot = layoutSnapshot,
        lineLayoutCache = lineLayoutCache,
        canvasMetrics = canvasMetrics,
        viewportSnapshot = viewportSnapshot,
        textOffset = normalizedEnd,
    )

    SelectionHandle(
        density = density,
        kind = SelectionHandleKind.Start,
        placement = startPlacement,
        onHandleInteractionStart = onHandleInteractionStart,
        onDragViewportPosition = { viewportPosition ->
            val textOffset = resolveEditorTextOffset(
                layoutSnapshot = layoutSnapshot,
                lineLayoutCache = lineLayoutCache,
                lineHeightPx = canvasMetrics.lineHeightPx,
                position = viewportPosition.toContentPosition(viewportSnapshot),
            )
            val nextStart = textOffset.coerceIn(0, normalizedEnd)
            val nextSelection = TextRange(normalizedEnd, nextStart)
            if (selection != nextSelection) {
                onSelectionChange(nextSelection)
            }
        },
    )

    SelectionHandle(
        density = density,
        kind = SelectionHandleKind.End,
        placement = endPlacement,
        onHandleInteractionStart = onHandleInteractionStart,
        onDragViewportPosition = { viewportPosition ->
            val textOffset = resolveEditorTextOffset(
                layoutSnapshot = layoutSnapshot,
                lineLayoutCache = lineLayoutCache,
                lineHeightPx = canvasMetrics.lineHeightPx,
                position = viewportPosition.toContentPosition(viewportSnapshot),
            )
            val nextEnd = textOffset.coerceAtLeast(normalizedStart)
            val nextSelection = TextRange(normalizedStart, nextEnd)
            if (selection != nextSelection) {
                onSelectionChange(nextSelection)
            }
        },
    )
}

@Composable
private fun SelectionHandle(
    density: Density,
    kind: SelectionHandleKind,
    placement: SelectionHandlePlacement,
    onHandleInteractionStart: () -> Unit,
    onDragViewportPosition: (Offset) -> Unit,
) {
    Box(
        contentAlignment = Alignment.TopStart,
        modifier = Modifier
            .offset {
                IntOffset(
                    x = placement.touchLeftPx.roundToInt(),
                    y = placement.touchTopPx.roundToInt(),
                )
            }
            .requiredSize(
                width = SELECTION_HANDLE_TOUCH_WIDTH_DP.dp,
                height = SELECTION_HANDLE_TOUCH_HEIGHT_DP.dp,
            )
            .pointerInput(placement.touchLeftPx, placement.touchTopPx) {
                detectDragGestures(
                    onDragStart = {
                        onHandleInteractionStart()
                    },
                    onDrag = { change, _ ->
                        onDragViewportPosition(
                            Offset(
                                x = placement.touchLeftPx + change.position.x,
                                y = placement.touchTopPx + change.position.y,
                            )
                        )
                        change.consume()
                    },
                )
            },
    ) {
        Canvas(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = placement.visualLeftPx.roundToInt(),
                        y = placement.visualTopPx.roundToInt(),
                    )
                }
                .requiredSize(
                    width = SELECTION_HANDLE_VISUAL_SIZE_DP.dp,
                    height = SELECTION_HANDLE_VISUAL_SIZE_DP.dp,
                ),
        ) {
            val radius = size.width / 2f
            if (kind == SelectionHandleKind.Start) {
                scale(
                    scaleX = -1f,
                    scaleY = 1f,
                    pivot = center,
                ) {
                    drawSelectionHandleBase(radius)
                }
            } else {
                drawSelectionHandleBase(radius)
            }
        }
    }
}

private enum class SelectionHandleKind {
    Start,
    End,
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSelectionHandleBase(
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

private data class SelectionHandlePlacement(
    val touchLeftPx: Float,
    val touchTopPx: Float,
    val visualLeftPx: Float,
    val visualTopPx: Float,
)

private fun resolveSelectionHandlePlacement(
    kind: SelectionHandleKind,
    density: Density,
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    canvasMetrics: CodeViewerCanvasMetrics,
    viewportSnapshot: CodeViewerViewportSnapshot,
    textOffset: Int,
): SelectionHandlePlacement {
    val cursor = layoutSnapshot.offsetToCursor(textOffset)
    val touchWidthPx = with(density) { SELECTION_HANDLE_TOUCH_WIDTH_DP.dp.toPx() }
    val visualSizePx = with(density) { SELECTION_HANDLE_VISUAL_SIZE_DP.dp.toPx() }
    val touchTopInsetPx = with(density) { SELECTION_HANDLE_TOUCH_TOP_INSET_DP.dp.toPx() }
    val horizontalPaddingPx = (touchWidthPx - visualSizePx) / 2f
    val xPx = lineLayoutCache.columnX(cursor.line, cursor.offset) - viewportSnapshot.horizontalScrollPx
    val yPx = (cursor.line + 1) * canvasMetrics.lineHeightPx - viewportSnapshot.verticalScrollPx
    val visualAnchorOffsetPx = when (kind) {
        SelectionHandleKind.Start -> visualSizePx
        SelectionHandleKind.End -> 0f
    }
    val visualLeftPx = xPx - visualAnchorOffsetPx
    return SelectionHandlePlacement(
        touchLeftPx = visualLeftPx - horizontalPaddingPx,
        touchTopPx = yPx - touchTopInsetPx,
        visualLeftPx = horizontalPaddingPx,
        visualTopPx = touchTopInsetPx,
    )
}

private fun Offset.toContentPosition(
    viewportSnapshot: CodeViewerViewportSnapshot,
): Offset {
    return Offset(
        x = x + viewportSnapshot.horizontalScrollPx,
        y = y + viewportSnapshot.verticalScrollPx,
    )
}

private val SELECTION_HANDLE_COLOR: Color = Color(0xFF1A73E8)
private const val SELECTION_HANDLE_TOUCH_WIDTH_DP: Int = 36
private const val SELECTION_HANDLE_TOUCH_HEIGHT_DP: Int = 44
private const val SELECTION_HANDLE_TOUCH_TOP_INSET_DP: Int = 4
private const val SELECTION_HANDLE_VISUAL_SIZE_DP: Int = 22
