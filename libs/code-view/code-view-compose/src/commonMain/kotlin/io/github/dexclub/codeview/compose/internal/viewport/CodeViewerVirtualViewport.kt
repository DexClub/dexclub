package io.github.dexclub.codeview.compose.internal.viewport

import kotlin.math.max

import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot

internal data class CodeViewerVirtualVerticalLayout(
    val totalContentHeightPx: Float,
    val maxVerticalScrollPx: Float,
)

internal fun resolveCodeViewerViewportState(
    layoutSnapshot: CodeLayoutSnapshot,
    verticalScrollPx: Float,
    horizontalScrollPx: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    lineHeightPx: Float,
    maxHorizontalScrollPx: Float,
    extraBottomLines: Int,
): CodeViewportState {
    return CodeViewportState(
        firstVisibleLine = resolveFirstVisibleLine(
            verticalScrollPx = verticalScrollPx,
            lineHeightPx = lineHeightPx,
        ),
        horizontalScrollPx = horizontalScrollPx,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        lineHeightPx = lineHeightPx,
    ).clamp(
        layout = layoutSnapshot,
        extraBottomLines = extraBottomLines,
    ).clampHorizontalScroll(
        maxHorizontalScrollPx = maxHorizontalScrollPx,
    )
}

internal fun resolveCodeViewerVerticalLayout(
    lineCount: Int,
    lineHeightPx: Float,
    viewportHeightPx: Float,
    scrollPastEnd: Int,
): CodeViewerVirtualVerticalLayout {
    val totalContentHeightPx = max(
        viewportHeightPx.coerceAtLeast(0f),
        lineCount.coerceAtLeast(0) * lineHeightPx.coerceAtLeast(0f) +
            resolveScrollPastEndReservedHeightPx(
                scrollPastEnd = scrollPastEnd,
                lineHeightPx = lineHeightPx,
            ),
    )
    return CodeViewerVirtualVerticalLayout(
        totalContentHeightPx = totalContentHeightPx,
        maxVerticalScrollPx = max(0f, totalContentHeightPx - viewportHeightPx.coerceAtLeast(0f)),
    )
}

internal fun resolveScrollPastEndReservedHeightPx(
    scrollPastEnd: Int,
    lineHeightPx: Float,
): Float {
    if (scrollPastEnd <= 0 || lineHeightPx <= 0f) return 0f
    return scrollPastEnd * lineHeightPx
}

private fun resolveFirstVisibleLine(
    verticalScrollPx: Float,
    lineHeightPx: Float,
): Int {
    if (verticalScrollPx <= 0f || lineHeightPx <= 0f) return 0
    return (verticalScrollPx / lineHeightPx).toInt().coerceAtLeast(0)
}
