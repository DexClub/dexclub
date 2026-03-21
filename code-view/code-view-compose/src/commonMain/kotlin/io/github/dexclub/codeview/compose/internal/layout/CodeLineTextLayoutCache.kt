package io.github.dexclub.codeview.compose.internal.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
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

    val maxLineWidthPx: Float by lazy(LazyThreadSafetyMode.NONE) {
        layoutSnapshot.lines.indices.maxOfOrNull(::lineWidthPx) ?: 0f
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

    private fun annotatedLine(lineIndex: Int): AnnotatedString {
        return lineTextCache.getOrPut(lineIndex) {
            val line = layoutSnapshot.lineAt(lineIndex)
            val tokens = layoutSnapshot.tokensForLine(lineIndex)
            if (tokens.isEmpty() || line.content.isEmpty()) {
                return@getOrPut AnnotatedString(line.content)
            }

            buildAnnotatedString {
                append(line.content)
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
}
