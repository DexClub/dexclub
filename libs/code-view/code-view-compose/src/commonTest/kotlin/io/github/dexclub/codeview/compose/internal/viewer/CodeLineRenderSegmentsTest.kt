package io.github.dexclub.codeview.compose.internal.viewer

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeLineRenderSegmentsTest {
    private val plainColor = Color(0xFF1F2328)
    private val keywordColor = Color(0xFF7C3AED)

    @Test
    fun resolveCodeViewerTextMetricGroup_distinguishesLatinAndCjk() {
        assertEquals(
            expected = CodeViewerTextMetricGroup.EditorDefault,
            actual = resolveCodeViewerTextMetricGroup('a'),
        )
        assertEquals(
            expected = CodeViewerTextMetricGroup.EastAsianWide,
            actual = resolveCodeViewerTextMetricGroup('中'),
        )
        assertEquals(
            expected = CodeViewerTextMetricGroup.EastAsianWide,
            actual = resolveCodeViewerTextMetricGroup('Ａ'),
        )
    }

    @Test
    fun buildCodeLineRenderSegments_splitsWhenMetricGroupChanges() {
        val segments = buildCodeLineRenderSegments(
            text = "ab中c",
            colorAtIndex = { plainColor },
        )

        assertEquals(
            expected = listOf("ab", "中", "c"),
            actual = segments.map(CodeLineRenderSegment::text),
        )
        assertEquals(
            expected = listOf(0, 2, 3),
            actual = segments.map(CodeLineRenderSegment::startColumn),
        )
    }

    @Test
    fun buildCodeLineRenderSegments_splitsWhenColorChangesInsideSameMetricGroup() {
        val segments = buildCodeLineRenderSegments(
            text = "abcd",
            colorAtIndex = { index ->
                if (index < 2) plainColor else keywordColor
            },
        )

        assertEquals(
            expected = listOf("ab", "cd"),
            actual = segments.map(CodeLineRenderSegment::text),
        )
        assertEquals(
            expected = listOf(plainColor, keywordColor),
            actual = segments.map(CodeLineRenderSegment::color),
        )
    }

    @Test
    fun sliceCodeLineRenderSegments_keepsRelativeColumnsAndText() {
        val segments = listOf(
            CodeLineRenderSegment(
                startColumn = 0,
                endColumn = 2,
                text = "ab",
                color = plainColor,
            ),
            CodeLineRenderSegment(
                startColumn = 2,
                endColumn = 3,
                text = "中",
                color = plainColor,
            ),
            CodeLineRenderSegment(
                startColumn = 3,
                endColumn = 5,
                text = "cd",
                color = keywordColor,
            ),
        )

        val sliced = sliceCodeLineRenderSegments(
            segments = segments,
            startColumn = 1,
            endColumn = 4,
        )

        assertEquals(
            expected = listOf("b", "中", "c"),
            actual = sliced.map(CodeLineRenderSegment::text),
        )
        assertEquals(
            expected = listOf(1, 2, 3),
            actual = sliced.map(CodeLineRenderSegment::startColumn),
        )
        assertEquals(
            expected = listOf(2, 3, 4),
            actual = sliced.map(CodeLineRenderSegment::endColumn),
        )
    }
}
