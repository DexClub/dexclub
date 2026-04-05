package io.github.dexclub.codeview.compose.internal.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import io.github.dexclub.codeview.compose.CodeGutterOptions
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.core.text.Cursor

internal fun resolveLineNumberDigits(
    lineCount: Int,
    minDigits: Int,
): Int {
    require(lineCount > 0) { "lineCount 必须大于 0: $lineCount" }
    require(minDigits >= 1) { "minDigits 必须大于等于 1: $minDigits" }
    return maxOf(minDigits, lineCount.toString().length)
}

internal fun resolveCodeContentViewportWidthPx(
    viewportWidthPx: Float,
    contentLeftInsetPx: Float,
): Float {
    return (viewportWidthPx - contentLeftInsetPx).coerceAtLeast(0f)
}

internal fun resolveCodeGutterWidthPx(
    lineLayoutCache: CodeLineTextLayoutCache,
    lineCount: Int,
    gutterOptions: CodeGutterOptions,
    startPaddingPx: Float,
    endPaddingPx: Float,
): Float {
    val lineNumberOptions = gutterOptions.lineNumbers
    val sampleDigits = resolveLineNumberDigits(
        lineCount = lineCount,
        minDigits = lineNumberOptions.minDigits,
    )
    val sampleText = buildString(sampleDigits) {
        repeat(sampleDigits) { append('9') }
    }
    val sampleLayout = lineLayoutCache.segmentLayout(
        text = sampleText,
        color = lineNumberOptions.textColor,
    )
    return sampleLayout.size.width.toFloat() + startPaddingPx + endPaddingPx
}

internal fun DrawScope.drawCodeLineNumbers(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    visibleLineRange: IntRange,
    lineHeightPx: Float,
    verticalScrollPx: Float,
    baselinePx: Float,
    gutterWidthPx: Float,
    cursor: Cursor?,
    gutterOptions: CodeGutterOptions,
) {
    if (gutterWidthPx <= 0f) return

    val lineNumberOptions = gutterOptions.lineNumbers
    val startPaddingPx = lineNumberOptions.startPadding.toPx()
    val endPaddingPx = lineNumberOptions.endPadding.toPx()
    val dividerWidthPx = LINE_NUMBER_GUTTER_DIVIDER_WIDTH_DP.dp.toPx()
    val dividerX = gutterWidthPx - dividerWidthPx / 2f
    drawRect(
        color = gutterOptions.backgroundColor,
        topLeft = Offset.Zero,
        size = size.copy(width = gutterWidthPx),
    )
    drawLine(
        color = gutterOptions.dividerColor,
        start = Offset(dividerX, 0f),
        end = Offset(dividerX, size.height),
        strokeWidth = dividerWidthPx,
    )

    for (lineIndex in visibleLineRange) {
        val lineNumberText = (lineIndex + 1).toString()
        val color = if (cursor?.line == lineIndex) {
            lineNumberOptions.activeTextColor
        } else {
            lineNumberOptions.textColor
        }
        val lineNumberLayout = lineLayoutCache.segmentLayout(
            text = lineNumberText,
            color = color,
        )
        val lineTop = lineIndex * lineHeightPx - verticalScrollPx
        drawText(
            textLayoutResult = lineNumberLayout,
            topLeft = Offset(
                x = (dividerX - dividerWidthPx / 2f - endPaddingPx - lineNumberLayout.size.width.toFloat())
                    .coerceAtLeast(startPaddingPx),
                y = resolveSegmentTextTopPx(
                    lineTop = lineTop,
                    baselinePx = baselinePx,
                    layout = lineNumberLayout,
                ),
            ),
        )
    }
}

private const val LINE_NUMBER_GUTTER_DIVIDER_WIDTH_DP: Int = 1
