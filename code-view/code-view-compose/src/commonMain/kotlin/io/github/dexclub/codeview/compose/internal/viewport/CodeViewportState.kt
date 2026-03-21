package io.github.dexclub.codeview.compose.internal.viewport

import kotlin.math.ceil
import kotlin.math.max

import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.core.text.Cursor

internal data class CodeViewportState(
    val firstVisibleLine: Int = 0,
    val horizontalScrollPx: Float = 0f,
    val viewportWidthPx: Float = 0f,
    val viewportHeightPx: Float = 0f,
    val lineHeightPx: Float = 0f,
) {
    init {
        require(firstVisibleLine >= 0) { "firstVisibleLine 不能为负数: $firstVisibleLine" }
        require(horizontalScrollPx >= 0f) { "horizontalScrollPx 不能为负数: $horizontalScrollPx" }
        require(viewportWidthPx >= 0f) { "viewportWidthPx 不能为负数: $viewportWidthPx" }
        require(viewportHeightPx >= 0f) { "viewportHeightPx 不能为负数: $viewportHeightPx" }
        require(lineHeightPx >= 0f) { "lineHeightPx 不能为负数: $lineHeightPx" }
    }

    val visibleLineCount: Int
        get() {
            if (viewportHeightPx <= 0f || lineHeightPx <= 0f) return 0
            return max(1, ceil(viewportHeightPx / lineHeightPx).toInt())
        }


    fun lastVisibleLine(lineCount: Int): Int {
        require(lineCount > 0) { "lineCount 必须大于 0: $lineCount" }
        val safeFirstVisibleLine = firstVisibleLine.coerceIn(0, lineCount - 1)
        val safeVisibleLineCount = visibleLineCount.coerceAtLeast(1)
        return (safeFirstVisibleLine + safeVisibleLineCount - 1).coerceAtMost(lineCount - 1)
    }


    fun visibleLineRange(lineCount: Int): IntRange {
        require(lineCount > 0) { "lineCount 必须大于 0: $lineCount" }
        val safeFirstVisibleLine = firstVisibleLine.coerceIn(0, lineCount - 1)
        return safeFirstVisibleLine..lastVisibleLine(lineCount)
    }


    fun clamp(layout: CodeLayoutSnapshot): CodeViewportState {
        val maxFirstVisibleLine = (layout.lineCount - visibleLineCount.coerceAtLeast(1)).coerceAtLeast(0)
        return copy(
            firstVisibleLine = firstVisibleLine.coerceIn(0, maxFirstVisibleLine),
        )
    }


    fun clampHorizontalScroll(maxHorizontalScrollPx: Float): CodeViewportState {
        require(maxHorizontalScrollPx >= 0f) { "maxHorizontalScrollPx 不能为负数: $maxHorizontalScrollPx" }
        return copy(
            horizontalScrollPx = horizontalScrollPx.coerceIn(0f, maxHorizontalScrollPx),
        )
    }


    fun maxHorizontalScrollPx(
        layout: CodeLayoutSnapshot,
        charWidthPx: Float,
    ): Float {
        require(charWidthPx >= 0f) { "charWidthPx 不能为负数: $charWidthPx" }
        val contentWidthPx = layout.maxLineLength * charWidthPx
        return max(0f, contentWidthPx - viewportWidthPx)
    }


    fun revealCursor(
        layout: CodeLayoutSnapshot,
        cursor: Cursor?,
        charWidthPx: Float,
        cursorHorizontalPx: Float? = null,
        cursorWidthPx: Float = 1f,
        maxHorizontalScrollPx: Float? = null,
    ): CodeViewportState {
        val safeCursor = layout.clampCursor(cursor) ?: return clamp(layout)
        val safeViewport = clamp(layout)

        val safeVisibleLineCount = safeViewport.visibleLineCount.coerceAtLeast(1)
        val currentLastVisibleLine = safeViewport.lastVisibleLine(layout.lineCount)
        val nextFirstVisibleLine = when {
            safeCursor.line < safeViewport.firstVisibleLine -> safeCursor.line
            safeCursor.line > currentLastVisibleLine -> safeCursor.line - safeVisibleLineCount + 1
            else -> safeViewport.firstVisibleLine
        }.coerceAtLeast(0)

        val nextHorizontalScrollPx = revealHorizontalScroll(
            cursor = safeCursor,
            viewport = safeViewport,
            charWidthPx = charWidthPx,
            cursorHorizontalPx = cursorHorizontalPx,
            cursorWidthPx = cursorWidthPx,
        )

        return safeViewport.copy(
            firstVisibleLine = nextFirstVisibleLine,
            horizontalScrollPx = nextHorizontalScrollPx,
        ).clamp(layout).clampHorizontalScroll(
            maxHorizontalScrollPx = maxHorizontalScrollPx ?: safeViewport.maxHorizontalScrollPx(
                layout = layout,
                charWidthPx = charWidthPx,
            ),
        )
    }


    private fun revealHorizontalScroll(
        cursor: Cursor,
        viewport: CodeViewportState,
        charWidthPx: Float,
        cursorHorizontalPx: Float?,
        cursorWidthPx: Float,
    ): Float {
        require(charWidthPx >= 0f) { "charWidthPx 不能为负数: $charWidthPx" }
        if (viewport.viewportWidthPx <= 0f || charWidthPx <= 0f) {
            return viewport.horizontalScrollPx
        }

        val cursorStartX = cursorHorizontalPx ?: cursor.offset * charWidthPx
        val effectiveCursorWidthPx = when {
            cursorHorizontalPx == null -> charWidthPx.coerceAtLeast(1f)
            else -> cursorWidthPx.coerceAtLeast(1f)
        }
        val cursorEndX = cursorStartX + effectiveCursorWidthPx
        val visibleStartX = viewport.horizontalScrollPx
        val visibleEndX = viewport.horizontalScrollPx + viewport.viewportWidthPx

        return when {
            cursorStartX < visibleStartX -> cursorStartX
            cursorEndX > visibleEndX -> max(0f, cursorEndX - viewport.viewportWidthPx)
            else -> viewport.horizontalScrollPx
        }
    }
}
