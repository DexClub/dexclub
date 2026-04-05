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
    layoutSnapshot: CodeLayoutSnapshot,
) {
    private var layoutSnapshot: CodeLayoutSnapshot = layoutSnapshot
    private val lineTextCache: MutableMap<Int, AnnotatedString> = mutableMapOf()
    private val lineLayoutCache: MutableMap<Int, TextLayoutResult> = mutableMapOf()
    private val lineWidthCache: MutableMap<Int, Float> = mutableMapOf()
    private val plainTextLayoutCache: MutableMap<String, TextLayoutResult> = mutableMapOf()
    private val lineRenderSegmentsCache: MutableMap<Int, List<CodeLineRenderSegment>> = mutableMapOf()
    private val plainTextRenderSegmentsCache: MutableMap<SegmentRenderKey, List<CodeLineRenderSegment>> = mutableMapOf()
    private val segmentLayoutCache: MutableMap<SegmentRenderKey, TextLayoutResult> = mutableMapOf()

    fun updateLayoutSnapshot(nextLayoutSnapshot: CodeLayoutSnapshot) {
        val previousLayoutSnapshot = layoutSnapshot
        if (previousLayoutSnapshot === nextLayoutSnapshot) return
        layoutSnapshot = nextLayoutSnapshot

        val sharedLineCount = minOf(previousLayoutSnapshot.lineCount, nextLayoutSnapshot.lineCount)
        for (lineIndex in 0 until sharedLineCount) {
            val previousLine = previousLayoutSnapshot.lineAt(lineIndex)
            val nextLine = nextLayoutSnapshot.lineAt(lineIndex)
            val textChanged = previousLine != nextLine ||
                previousLayoutSnapshot.lineText(lineIndex) != nextLayoutSnapshot.lineText(lineIndex)
            if (textChanged) {
                invalidateMeasuredLine(lineIndex)
                continue
            }

            if (previousLayoutSnapshot.tokensForLine(lineIndex) != nextLayoutSnapshot.tokensForLine(lineIndex)) {
                invalidateDecoratedLine(lineIndex)
            }
        }

        trimLineCaches(nextLayoutSnapshot.lineCount)
    }

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
            CodeTokenKind.KeywordType -> IdeaLightSemanticPalette.KeywordBlue

            CodeTokenKind.StringLiteral,
            CodeTokenKind.Interpolation -> IdeaLightSemanticPalette.StringGreen

            CodeTokenKind.EscapeSequence -> IdeaLightSemanticPalette.EscapeTeal

            CodeTokenKind.NumberLiteral,
            CodeTokenKind.BooleanLiteral,
            CodeTokenKind.NullLiteral -> IdeaLightSemanticPalette.NumberBlue

            CodeTokenKind.Comment -> IdeaLightSemanticPalette.CommentGray
            CodeTokenKind.TypeName -> IdeaLightSemanticPalette.PlainText

            CodeTokenKind.Builtin -> IdeaLightSemanticPalette.BuiltinBlue

            CodeTokenKind.Annotation -> IdeaLightSemanticPalette.AnnotationOlive

            CodeTokenKind.FunctionName,
            CodeTokenKind.VariableName,
            CodeTokenKind.PropertyName,
            CodeTokenKind.ParameterName,
            CodeTokenKind.ConstantName,
            CodeTokenKind.LabelName,
            CodeTokenKind.Namespace,
            CodeTokenKind.Operator,
            CodeTokenKind.Punctuation -> IdeaLightSemanticPalette.PlainText

            CodeTokenKind.Invalid -> IdeaLightSemanticPalette.InvalidRed
            CodeTokenKind.PlainText -> IdeaLightSemanticPalette.PlainText
        }
    }

    private fun invalidateMeasuredLine(lineIndex: Int) {
        lineTextCache.remove(lineIndex)
        lineLayoutCache.remove(lineIndex)
        lineWidthCache.remove(lineIndex)
        lineRenderSegmentsCache.remove(lineIndex)
    }

    private fun invalidateDecoratedLine(lineIndex: Int) {
        lineTextCache.remove(lineIndex)
        lineRenderSegmentsCache.remove(lineIndex)
    }

    private fun trimLineCaches(lineCount: Int) {
        lineTextCache.keys.removeAll { it >= lineCount }
        lineLayoutCache.keys.removeAll { it >= lineCount }
        lineWidthCache.keys.removeAll { it >= lineCount }
        lineRenderSegmentsCache.keys.removeAll { it >= lineCount }
    }

    private data class SegmentRenderKey(
        val text: String,
        val color: Color,
    )
}

private object IdeaLightSemanticPalette {
    val PlainText: Color = Color(0xFF080808)
    val KeywordBlue: Color = Color(0xFF0033B3)
    val BuiltinBlue: Color = Color(0xFF0033B3)
    val StringGreen: Color = Color(0xFF067D17)
    val EscapeTeal: Color = Color(0xFF0037A6)
    val NumberBlue: Color = Color(0xFF1750EB)
    val CommentGray: Color = Color(0xFF8C8C8C)
    val AnnotationOlive: Color = Color(0xFF9E880D)
    val InvalidRed: Color = Color(0xFFFF0000)
}
