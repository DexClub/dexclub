package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerCanvasMetrics
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerViewportSnapshot
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
    onHandleInteractionEnd: () -> Unit,
) {
    var activeRangeHandleSession by remember {
        mutableStateOf<ActiveRangeHandleSession?>(null)
    }

    if (!shouldShowSelectionHandles(selection, activeRangeHandleSession != null)) return

    val normalizedStart = selection.normalizedStart
    val normalizedEnd = selection.normalizedEnd
    val rangeHandleConfigs = remember(normalizedStart, normalizedEnd) {
        listOf(
            RangeSelectionHandleConfig(
                kind = SelectionHandleKind.Start,
                textOffset = normalizedStart,
                fixedOffset = normalizedEnd,
            ),
            RangeSelectionHandleConfig(
                kind = SelectionHandleKind.End,
                textOffset = normalizedEnd,
                fixedOffset = normalizedStart,
            ),
        )
    }

    rangeHandleConfigs.forEach { config ->
        SelectionHandle(
            kind = config.kind,
            placement = resolveSelectionHandlePlacement(
                kind = config.kind,
                density = density,
                layoutSnapshot = layoutSnapshot,
                lineLayoutCache = lineLayoutCache,
                canvasMetrics = canvasMetrics,
                viewportSnapshot = viewportSnapshot,
                textOffset = config.textOffset,
            ),
            onHandleInteractionStart = {
                activeRangeHandleSession = ActiveRangeHandleSession(
                    kind = config.kind,
                    fixedOffset = config.fixedOffset,
                )
                onHandleInteractionStart()
            },
            onHandleInteractionEnd = {
                activeRangeHandleSession = null
                onHandleInteractionEnd()
            },
            onDragViewportPosition = { viewportPosition ->
                val draggedTextOffset = resolveTextOffsetFromViewportPosition(
                    layoutSnapshot = layoutSnapshot,
                    lineLayoutCache = lineLayoutCache,
                    lineHeightPx = canvasMetrics.lineHeightPx,
                    viewportSnapshot = viewportSnapshot,
                    viewportPosition = viewportPosition,
                )
                val fixedOffset = activeRangeHandleSession
                    ?.takeIf { it.kind == config.kind }
                    ?.fixedOffset
                    ?: config.fixedOffset
                val nextSelection = resolveHandleDragSelection(
                    kind = config.kind,
                    draggedTextOffset = draggedTextOffset,
                    fixedTextOffset = fixedOffset,
                )
                if (selection != nextSelection) {
                    onSelectionChange(nextSelection)
                }
            },
        )
    }
}

@Composable
internal fun CodeEditorTouchCursorHandle(
    density: Density,
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    canvasMetrics: CodeViewerCanvasMetrics,
    viewportSnapshot: CodeViewerViewportSnapshot,
    selection: TextRange,
    onSelectionChange: (TextRange) -> Unit,
    onHandleInteractionStart: () -> Unit,
    onHandleInteractionEnd: () -> Unit,
) {
    if (!shouldShowCursorHandle(selection)) return

    SelectionHandle(
        kind = SelectionHandleKind.Cursor,
        placement = resolveSelectionHandlePlacement(
            kind = SelectionHandleKind.Cursor,
            density = density,
            layoutSnapshot = layoutSnapshot,
            lineLayoutCache = lineLayoutCache,
            canvasMetrics = canvasMetrics,
            viewportSnapshot = viewportSnapshot,
            textOffset = selection.end,
        ),
        onHandleInteractionStart = onHandleInteractionStart,
        onHandleInteractionEnd = onHandleInteractionEnd,
        onDragViewportPosition = { viewportPosition ->
            val nextSelection = resolveCursorHandleSelection(
                draggedTextOffset = resolveTextOffsetFromViewportPosition(
                    layoutSnapshot = layoutSnapshot,
                    lineLayoutCache = lineLayoutCache,
                    lineHeightPx = canvasMetrics.lineHeightPx,
                    viewportSnapshot = viewportSnapshot,
                    viewportPosition = viewportPosition,
                )
            )
            if (selection != nextSelection) {
                onSelectionChange(nextSelection)
            }
        },
    )
}

@Composable
private fun SelectionHandle(
    kind: SelectionHandleKind,
    placement: SelectionHandlePlacement,
    onHandleInteractionStart: () -> Unit,
    onHandleInteractionEnd: () -> Unit,
    onDragViewportPosition: (Offset) -> Unit,
) {
    val latestPlacement = rememberUpdatedState(placement)
    val latestOnHandleInteractionStart = rememberUpdatedState(onHandleInteractionStart)
    val latestOnHandleInteractionEnd = rememberUpdatedState(onHandleInteractionEnd)
    val latestOnDragViewportPosition = rememberUpdatedState(onDragViewportPosition)

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
            .pointerInput(kind) {
                var dragViewportPosition: Offset? = null
                detectDragGestures(
                    onDragStart = { startPosition ->
                        latestOnHandleInteractionStart.value()
                        val currentPlacement = latestPlacement.value
                        dragViewportPosition = Offset(
                            x = currentPlacement.touchLeftPx + startPosition.x,
                            y = currentPlacement.touchTopPx + startPosition.y,
                        )
                    },
                    onDragEnd = {
                        dragViewportPosition = null
                        latestOnHandleInteractionEnd.value()
                    },
                    onDragCancel = {
                        dragViewportPosition = null
                        latestOnHandleInteractionEnd.value()
                    },
                    onDrag = { change, dragAmount ->
                        val currentPosition = dragViewportPosition ?: Offset(
                            x = latestPlacement.value.touchLeftPx + change.position.x,
                            y = latestPlacement.value.touchTopPx + change.position.y,
                        )
                        val nextPosition = currentPosition + dragAmount
                        dragViewportPosition = nextPosition
                        latestOnDragViewportPosition.value(nextPosition)
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
                    width = placement.visualWidthDp,
                    height = placement.visualHeightDp,
                ),
        ) {
            drawHandleIcon(kind)
        }
    }
}

internal fun resolveHandleDragSelection(
    kind: SelectionHandleKind,
    draggedTextOffset: Int,
    fixedTextOffset: Int,
): TextRange {
    val safeDraggedTextOffset = draggedTextOffset.coerceAtLeast(0)
    val safeFixedTextOffset = fixedTextOffset.coerceAtLeast(0)
    return when (kind) {
        SelectionHandleKind.Start -> TextRange(safeDraggedTextOffset, safeFixedTextOffset)
        SelectionHandleKind.End -> TextRange(safeFixedTextOffset, safeDraggedTextOffset)
        SelectionHandleKind.Cursor -> error("Cursor 手柄不应走范围选区拖拽逻辑")
    }
}

internal fun shouldShowSelectionHandles(
    selection: TextRange,
    hasActiveRangeHandleSession: Boolean,
): Boolean {
    return !selection.collapsed || hasActiveRangeHandleSession
}

internal fun resolveCursorHandleSelection(
    draggedTextOffset: Int,
): TextRange {
    return TextRange(draggedTextOffset.coerceAtLeast(0))
}

internal fun shouldShowCursorHandle(
    selection: TextRange,
): Boolean {
    return selection.collapsed
}

private data class ActiveRangeHandleSession(
    val kind: SelectionHandleKind,
    val fixedOffset: Int,
)

private data class RangeSelectionHandleConfig(
    val kind: SelectionHandleKind,
    val textOffset: Int,
    val fixedOffset: Int,
)

private fun resolveTextOffsetFromViewportPosition(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    lineHeightPx: Float,
    viewportSnapshot: CodeViewerViewportSnapshot,
    viewportPosition: Offset,
): Int {
    return resolveEditorTextOffset(
        layoutSnapshot = layoutSnapshot,
        lineLayoutCache = lineLayoutCache,
        lineHeightPx = lineHeightPx,
        position = viewportPosition.toContentPosition(viewportSnapshot),
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
