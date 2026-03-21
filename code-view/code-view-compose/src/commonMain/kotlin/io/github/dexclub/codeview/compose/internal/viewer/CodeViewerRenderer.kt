package io.github.dexclub.codeview.compose.internal.viewer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import io.github.dexclub.codeview.compose.internal.editor.CodeEditorComposingOverlay
import io.github.dexclub.codeview.compose.internal.editor.normalizedEnd
import io.github.dexclub.codeview.compose.internal.editor.normalizedStart
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection

internal fun DrawScope.drawCodeViewerContent(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    lineHeightPx: Float,
    selection: LineSelection?,
    searchHighlight: LineSelection?,
    cursor: Cursor?,
    composingOverlay: CodeEditorComposingOverlay?,
    cursorAlpha: Float,
    visibleLineRange: IntRange,
) {
    val selectionColor = Color(0x334096FF)
    val searchHighlightColor = Color(0x40F4D03F)
    val cursorColor = Color(0xFF1F2328)

    for (lineIndex in visibleLineRange) {
        val line = layoutSnapshot.lineAt(lineIndex)
        val lineTop = lineIndex * lineHeightPx

        drawSelectionRange(
            lineIndex = lineIndex,
            lineLength = line.length,
            selection = searchHighlight,
            color = searchHighlightColor,
            lineLayoutCache = lineLayoutCache,
            lineTop = lineTop,
            lineHeightPx = lineHeightPx,
            extendMultilineToContentRight = false,
            verticalInsetPx = 1.dp.toPx(),
            cornerRadiusPx = 4.dp.toPx(),
        )
        drawSelectionRange(
            lineIndex = lineIndex,
            lineLength = line.length,
            selection = selection,
            color = selectionColor,
            lineLayoutCache = lineLayoutCache,
            lineTop = lineTop,
            lineHeightPx = lineHeightPx,
            extendMultilineToContentRight = true,
            verticalInsetPx = 0f,
            cornerRadiusPx = 0f,
        )

        drawCodeLineText(
            layoutSnapshot = layoutSnapshot,
            lineIndex = lineIndex,
            lineLayoutCache = lineLayoutCache,
            lineTop = lineTop,
            lineHeightPx = lineHeightPx,
            composingOverlay = composingOverlay,
            overlayColor = cursorColor,
        )

        val inlineComposing = resolveInlineComposingOverlay(
            layoutSnapshot = layoutSnapshot,
            lineIndex = lineIndex,
            composingOverlay = composingOverlay,
        )
        if (inlineComposing != null) {
            val lineLayout = lineLayoutCache.layout(lineIndex)
            val overlayLayout = lineLayoutCache.plainTextLayout(inlineComposing.overlayText)
            val contentTop = lineContentTopPx(
                lineTop = lineTop,
                lineHeightPx = lineHeightPx,
                layout = lineLayout,
            )
            val contentBottom = lineContentBottomPx(
                lineTop = lineTop,
                lineHeightPx = lineHeightPx,
                layout = lineLayout,
            )
            val anchorStartX = lineLayoutCache.columnX(lineIndex, inlineComposing.startColumn)
            val caretX = anchorStartX + overlayLayout.getCursorRect(inlineComposing.caretOffset).left
            val roundedCaretX = kotlin.math.round(caretX * density) / density
            drawLine(
                color = cursorColor.copy(alpha = cursorColor.alpha * cursorAlpha),
                start = Offset(roundedCaretX, contentTop + 1.dp.toPx()),
                end = Offset(roundedCaretX, contentBottom - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        } else if (cursor != null && cursor.line == lineIndex) {
            val layout = lineLayoutCache.layout(lineIndex)
            val contentTop = lineContentTopPx(
                lineTop = lineTop,
                lineHeightPx = lineHeightPx,
                layout = layout,
            )
            val contentBottom = lineContentBottomPx(
                lineTop = lineTop,
                lineHeightPx = lineHeightPx,
                layout = layout,
            )
            val rawCursorX = lineLayoutCache.columnX(lineIndex, cursor.offset)
            val cursorX = kotlin.math.round(rawCursorX * density) / density
            drawLine(
                color = cursorColor.copy(alpha = cursorColor.alpha * cursorAlpha),
                start = Offset(cursorX, contentTop + 1.dp.toPx()),
                end = Offset(cursorX, contentBottom - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawSelectionRange(
    lineIndex: Int,
    lineLength: Int,
    selection: LineSelection?,
    color: Color,
    lineLayoutCache: CodeLineTextLayoutCache,
    lineTop: Float,
    lineHeightPx: Float,
    extendMultilineToContentRight: Boolean,
    verticalInsetPx: Float,
    cornerRadiusPx: Float,
) {
    if (selection == null) return
    if (selection.isCollapsed) return
    if (lineIndex < selection.startLine || lineIndex > selection.endLine) return

    val startColumn = when {
        lineIndex == selection.startLine -> selection.startOffset
        else -> 0
    }.coerceIn(0, lineLength)
    val endColumn = when {
        lineIndex == selection.endLine -> selection.endOffset
        else -> lineLength
    }.coerceIn(startColumn, lineLength)

    val left = lineLayoutCache.columnX(lineIndex, startColumn)
    val right = when {
        extendMultilineToContentRight && lineIndex < selection.endLine -> size.width
        else -> lineLayoutCache.columnX(lineIndex, endColumn)
    }
    val width = (right - left).coerceAtLeast(0f)
    if (width <= 0f) return
    val layout = lineLayoutCache.layout(lineIndex)
    val contentTop = lineContentTopPx(
        lineTop = lineTop,
        lineHeightPx = lineHeightPx,
        layout = layout,
    )
    val contentBottom = lineContentBottomPx(
        lineTop = lineTop,
        lineHeightPx = lineHeightPx,
        layout = layout,
    )
    val lineBottom = lineTop + lineHeightPx
    val isMultilineSelection = selection.startLine != selection.endLine
    val shouldFillWholeLineHeight = extendMultilineToContentRight && isMultilineSelection
    val paddingPx = when {
        verticalInsetPx > 0f -> verticalInsetPx
        else -> 1.5.dp.toPx()
    }
    val rectTop = when {
        shouldFillWholeLineHeight -> lineTop
        else -> maxOf(lineTop, contentTop - paddingPx)
    }
    val rectBottom = when {
        shouldFillWholeLineHeight -> lineBottom
        else -> minOf(lineBottom, contentBottom + paddingPx)
    }
    val height = (rectBottom - rectTop).coerceAtLeast(1f)
    drawRoundRect(
        color = color,
        topLeft = Offset(left, rectTop),
        size = Size(width, height),
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
    )
}

private fun DrawScope.drawCodeLineText(
    layoutSnapshot: CodeLayoutSnapshot,
    lineIndex: Int,
    lineLayoutCache: CodeLineTextLayoutCache,
    lineTop: Float,
    lineHeightPx: Float,
    composingOverlay: CodeEditorComposingOverlay?,
    overlayColor: Color,
) {
    val layout = lineLayoutCache.layout(lineIndex)
    val textTop = lineContentTopPx(
        lineTop = lineTop,
        lineHeightPx = lineHeightPx,
        layout = layout,
    )
    val inlineComposing = resolveInlineComposingOverlay(
        layoutSnapshot = layoutSnapshot,
        lineIndex = lineIndex,
        composingOverlay = composingOverlay,
    )

    if (inlineComposing == null) {
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                x = 0f,
                y = textTop,
            ),
        )
        return
    }

    val anchorStartX = lineLayoutCache.columnX(lineIndex, inlineComposing.startColumn)
    val anchorEndX = lineLayoutCache.columnX(lineIndex, inlineComposing.endColumn)
    val replacedWidthPx = (anchorEndX - anchorStartX).coerceAtLeast(0f)
    val overlayLayout = lineLayoutCache.plainTextLayout(inlineComposing.overlayText)
    val overlayWidthPx = overlayLayout.size.width.toFloat()
    val suffixShiftPx = overlayWidthPx - replacedWidthPx

    clipRect(
        left = 0f,
        right = anchorStartX,
        top = lineTop,
        bottom = lineTop + lineHeightPx,
    ) {
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(0f, textTop),
        )
    }

    drawText(
        textLayoutResult = overlayLayout,
        topLeft = Offset(anchorStartX, textTop),
    )

    clipRect(
        left = anchorStartX + overlayWidthPx,
        right = size.width,
        top = lineTop,
        bottom = lineTop + lineHeightPx,
    ) {
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(suffixShiftPx, textTop),
        )
    }

    val underlineStart = inlineComposing.compositionStart
    val underlineEnd = inlineComposing.compositionEnd
    if (underlineStart == underlineEnd) return

    val underlineY = textTop + overlayLayout.getLineBaseline(0) + 2.dp.toPx()
    val underlineStartX = anchorStartX + overlayLayout.getCursorRect(underlineStart).left
    val underlineEndX = anchorStartX + overlayLayout.getCursorRect(underlineEnd).left
    drawDashedUnderline(
        color = overlayColor,
        startX = underlineStartX,
        endX = underlineEndX,
        y = underlineY,
        strokeWidth = 1.5f.dp.toPx(),
    )
}

private data class InlineComposingOverlay(
    val startColumn: Int,
    val endColumn: Int,
    val overlayText: String,
    val caretOffset: Int,
    val compositionStart: Int,
    val compositionEnd: Int,
)

private fun resolveInlineComposingOverlay(
    layoutSnapshot: CodeLayoutSnapshot,
    lineIndex: Int,
    composingOverlay: CodeEditorComposingOverlay?,
): InlineComposingOverlay? {
    val overlay = composingOverlay ?: return null
    val composition = overlay.imeFieldValue.composition ?: return null
    val overlayText = overlay.imeFieldValue.text
    if (overlayText.isEmpty()) return null
    if (overlayText.contains('\n') || overlayText.contains('\r')) return null

    val startOffset = overlay.anchorSelection.normalizedStart
    val endOffset = overlay.anchorSelection.normalizedEnd
    val startPosition = layoutSnapshot.offsetToPosition(startOffset)
    val endPosition = layoutSnapshot.offsetToPosition(endOffset)
    if (startPosition.lineIndex != lineIndex || endPosition.lineIndex != lineIndex) return null

    return InlineComposingOverlay(
        startColumn = startPosition.columnIndex,
        endColumn = endPosition.columnIndex,
        overlayText = overlayText,
        caretOffset = overlay.imeFieldValue.selection.end.coerceIn(0, overlayText.length),
        compositionStart = composition.start.coerceIn(0, overlayText.length),
        compositionEnd = composition.end.coerceIn(0, overlayText.length),
    )
}

private fun lineContentTopPx(
    lineTop: Float,
    lineHeightPx: Float,
    layout: TextLayoutResult,
): Float {
    return lineTop + ((lineHeightPx - layout.size.height.toFloat()) / 2f).coerceAtLeast(0f)
}

private fun lineContentBottomPx(
    lineTop: Float,
    lineHeightPx: Float,
    layout: TextLayoutResult,
): Float {
    return lineContentTopPx(
        lineTop = lineTop,
        lineHeightPx = lineHeightPx,
        layout = layout,
    ) + layout.size.height.toFloat()
}

private fun DrawScope.drawDashedUnderline(
    color: Color,
    startX: Float,
    endX: Float,
    y: Float,
    strokeWidth: Float,
) {
    val totalWidth = (endX - startX).coerceAtLeast(0f)
    if (totalWidth <= 0f) return

    val dashWidthPx = 4.dp.toPx()
    val gapWidthPx = 4.dp.toPx()
    var currentX = startX

    while (currentX < endX) {
        val segmentEndX = minOf(currentX + dashWidthPx, endX)
        drawLine(
            color = color,
            start = Offset(currentX, y),
            end = Offset(segmentEndX, y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        currentX = segmentEndX + gapWidthPx
    }
}
