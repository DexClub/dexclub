package io.github.dexclub.codeview.compose.internal.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import io.github.dexclub.codeview.compose.internal.viewer.buildCodeLineRenderSegments
import io.github.dexclub.codeview.compose.internal.viewer.CodeLineRenderSegment
import io.github.dexclub.codeview.core.token.CodeTokenKind

internal class CodeLineTextLayoutCache(
    private val textMeasurer: TextMeasurer,
    private val textStyle: TextStyle,
    private val layoutSnapshot: CodeLayoutSnapshot,
) {
    private val lineTextCache: MutableMap<Int, AnnotatedString> = mutableMapOf()
    private val lineLayoutCache: MutableMap<Int, TextLayoutResult> = mutableMapOf()
    private val lineWidthCache: MutableMap<Int, Float> = mutableMapOf()
    private val plainTextLayoutCache: MutableMap<String, TextLayoutResult> = mutableMapOf()
    private val lineRenderSegmentsCache: MutableMap<Int, List<CodeLineRenderSegment>> = mutableMapOf()
    private val plainTextRenderSegmentsCache: MutableMap<SegmentRenderKey, List<CodeLineRenderSegment>> = mutableMapOf()
    private val segmentLayoutCache: MutableMap<SegmentRenderKey, TextLayoutResult> = mutableMapOf()

    fun estimatedMaxLineWidthPx(fallbackCharWidthPx: Float): Float {
        return (layoutSnapshot.maxLineLength * fallbackCharWidthPx).coerceAtLeast(0f)
    }

    fun lineWidthPx(lineIndex: Int): Float {
        require(lineIndex in layoutSnapshot.lines.indices) { "lineIndex 超出范围: $lineIndex" }
        return lineWidthCache.getOrPut(lineIndex) {
            layout(lineIndex).size.width.toFloat()
        }
    }

    fun columnX(
        lineIndex: Int,
        column: Int,
    ): Float {
        val result = layout(lineIndex)
        val safeColumn = column.coerceIn(0, layoutSnapshot.lineLength(lineIndex))
        return result.getCursorRect(safeColumn).left
    }

    fun cursorWidthPx(
        lineIndex: Int,
        column: Int,
        fallbackCharWidthPx: Float,
    ): Float {
        val lineLength = layoutSnapshot.lineLength(lineIndex)
        if (column >= lineLength) {
            return fallbackCharWidthPx.coerceAtLeast(1f)
        }
        val currentX = columnX(lineIndex, column)
        val nextX = columnX(lineIndex, column + 1)
        return (nextX - currentX).coerceAtLeast(1f)
    }

    fun offsetForPosition(
        lineIndex: Int,
        xPx: Float,
        clampToLineEnd: Boolean,
    ): Int? {
        require(lineIndex in layoutSnapshot.lines.indices) { "lineIndex 超出范围: $lineIndex" }
        val lineLength = layoutSnapshot.lineLength(lineIndex)
        if (lineLength == 0) {
            return if (clampToLineEnd) 0 else null
        }

        val result = layout(lineIndex)
        val lineWidthPx = result.size.width.toFloat()
        if (!clampToLineEnd && xPx > lineWidthPx) {
            return null
        }

        val safeX = if (clampToLineEnd) {
            xPx.coerceIn(0f, lineWidthPx)
        } else {
            xPx.coerceAtLeast(0f)
        }
        return result
            .getOffsetForPosition(Offset(safeX, result.size.height / 2f))
            .coerceIn(0, lineLength)
    }

    fun layout(lineIndex: Int): TextLayoutResult {
        require(lineIndex in layoutSnapshot.lines.indices) { "lineIndex 超出范围: $lineIndex" }
        return lineLayoutCache.getOrPut(lineIndex) {
            textMeasurer.measure(
                text = annotatedLine(lineIndex),
                style = textStyle,
                softWrap = false,
            )
        }
    }

    fun plainTextLayout(text: String): TextLayoutResult {
        return plainTextLayoutCache.getOrPut(text) {
            textMeasurer.measure(
                text = AnnotatedString(text),
                style = textStyle,
                softWrap = false,
            )
        }
    }

    fun renderSegments(lineIndex: Int): List<CodeLineRenderSegment> {
        require(lineIndex in layoutSnapshot.lines.indices) { "lineIndex 超出范围: $lineIndex" }
        return lineRenderSegmentsCache.getOrPut(lineIndex) {
            val lineText = layoutSnapshot.lineText(lineIndex)
            if (lineText.isEmpty()) {
                return@getOrPut emptyList()
            }
            val tokens = layoutSnapshot.tokensForLine(lineIndex)
            val defaultColor = tokenColor(CodeTokenKind.PlainText)
            var tokenIndex = 0
            buildCodeLineRenderSegments(
                text = lineText,
                colorAtIndex = { column ->
                    while (tokenIndex < tokens.size && column >= tokens[tokenIndex].endColumn) {
                        tokenIndex += 1
                    }
                    val token = tokens.getOrNull(tokenIndex)
                    when {
                        token != null && column in token.startColumn until token.endColumn -> tokenColor(token.kind)
                        else -> defaultColor
                    }
                },
            )
        }
    }

    fun plainTextRenderSegments(
        text: String,
        color: Color,
    ): List<CodeLineRenderSegment> {
        val key = SegmentRenderKey(
            text = text,
            color = color,
        )
        return plainTextRenderSegmentsCache.getOrPut(key) {
            if (text.isEmpty()) {
                return@getOrPut emptyList()
            }
            buildCodeLineRenderSegments(
                text = text,
                colorAtIndex = { color },
            )
        }
    }

    fun segmentLayout(
        text: String,
        color: Color,
    ): TextLayoutResult {
        val key = SegmentRenderKey(
            text = text,
            color = color,
        )
        return segmentLayoutCache.getOrPut(key) {
            textMeasurer.measure(
                text = AnnotatedString(text),
                style = textStyle.copy(color = color),
                softWrap = false,
            )
        }
    }

    private fun annotatedLine(lineIndex: Int): AnnotatedString {
        return lineTextCache.getOrPut(lineIndex) {
            val lineText = layoutSnapshot.lineText(lineIndex)
            val tokens = layoutSnapshot.tokensForLine(lineIndex)
            if (tokens.isEmpty() || lineText.isEmpty()) {
                return@getOrPut AnnotatedString(lineText)
            }

            buildAnnotatedString {
                append(lineText)
                tokens.forEach { token ->
                    if (token.startColumn >= token.endColumn) return@forEach
                    addStyle(
                        style = SpanStyle(color = tokenColor(token.kind)),
                        start = token.startColumn,
                        end = token.endColumn,
                    )
                }
            }
        }
    }

    private fun tokenColor(kind: CodeTokenKind): Color {
        return when (kind) {
            CodeTokenKind.Keyword,
            CodeTokenKind.KeywordModifier,
            CodeTokenKind.KeywordType -> Color(0xFF7C3AED)

            CodeTokenKind.StringLiteral,
            CodeTokenKind.EscapeSequence,
            CodeTokenKind.Interpolation -> Color(0xFF0A7F5A)

            CodeTokenKind.NumberLiteral,
            CodeTokenKind.BooleanLiteral,
            CodeTokenKind.NullLiteral -> Color(0xFF0550AE)

            CodeTokenKind.Comment -> Color(0xFF6E7781)
            CodeTokenKind.TypeName,
            CodeTokenKind.Annotation -> Color(0xFFB35900)

            CodeTokenKind.FunctionName,
            CodeTokenKind.VariableName,
            CodeTokenKind.PropertyName,
            CodeTokenKind.ParameterName,
            CodeTokenKind.ConstantName,
            CodeTokenKind.LabelName,
            CodeTokenKind.Namespace,
            CodeTokenKind.Builtin -> Color(0xFF1F2328)

            CodeTokenKind.Operator,
            CodeTokenKind.Punctuation -> Color(0xFF57606A)

            CodeTokenKind.Invalid -> Color(0xFFCF222E)
            CodeTokenKind.PlainText -> Color(0xFF1F2328)
        }
    }

    private data class SegmentRenderKey(
        val text: String,
        val color: Color,
    )
}
