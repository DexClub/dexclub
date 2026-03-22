package io.github.dexclub.codeview.compose.internal.viewer

import androidx.compose.ui.graphics.Color

internal data class CodeLineRenderSegment(
    val startColumn: Int,
    val endColumn: Int,
    val text: String,
    val color: Color,
)

internal enum class CodeViewerTextMetricGroup {
    EditorDefault,
    EastAsianWide,
}

internal fun resolveCodeViewerTextMetricGroup(char: Char): CodeViewerTextMetricGroup {
    val code = char.code
    return when {
        code in 0x2E80..0xA4CF -> CodeViewerTextMetricGroup.EastAsianWide
        code in 0xAC00..0xD7AF -> CodeViewerTextMetricGroup.EastAsianWide
        code in 0xF900..0xFAFF -> CodeViewerTextMetricGroup.EastAsianWide
        code in 0xFE10..0xFE6F -> CodeViewerTextMetricGroup.EastAsianWide
        code in 0xFF00..0xFFEF -> CodeViewerTextMetricGroup.EastAsianWide
        else -> CodeViewerTextMetricGroup.EditorDefault
    }
}

internal fun buildCodeLineRenderSegments(
    text: String,
    colorAtIndex: (Int) -> Color,
): List<CodeLineRenderSegment> {
    if (text.isEmpty()) return emptyList()

    val segments = mutableListOf<CodeLineRenderSegment>()
    var segmentStart = 0
    var currentColor = colorAtIndex(0)
    var currentMetricGroup = resolveCodeViewerTextMetricGroup(text[0])

    for (index in 1 until text.length) {
        val nextColor = colorAtIndex(index)
        val nextMetricGroup = resolveCodeViewerTextMetricGroup(text[index])
        if (nextColor == currentColor && nextMetricGroup == currentMetricGroup) {
            continue
        }
        segments += CodeLineRenderSegment(
            startColumn = segmentStart,
            endColumn = index,
            text = text.substring(segmentStart, index),
            color = currentColor,
        )
        segmentStart = index
        currentColor = nextColor
        currentMetricGroup = nextMetricGroup
    }

    segments += CodeLineRenderSegment(
        startColumn = segmentStart,
        endColumn = text.length,
        text = text.substring(segmentStart),
        color = currentColor,
    )
    return segments
}

internal fun sliceCodeLineRenderSegments(
    segments: List<CodeLineRenderSegment>,
    startColumn: Int,
    endColumn: Int,
): List<CodeLineRenderSegment> {
    if (startColumn >= endColumn) return emptyList()

    return buildList {
        segments.forEach { segment ->
            if (segment.endColumn <= startColumn || segment.startColumn >= endColumn) {
                return@forEach
            }

            val segmentSliceStart = maxOf(startColumn, segment.startColumn)
            val segmentSliceEnd = minOf(endColumn, segment.endColumn)
            add(
                CodeLineRenderSegment(
                    startColumn = segmentSliceStart,
                    endColumn = segmentSliceEnd,
                    text = segment.text.substring(
                        startIndex = segmentSliceStart - segment.startColumn,
                        endIndex = segmentSliceEnd - segment.startColumn,
                    ),
                    color = segment.color,
                )
            )
        }
    }
}
