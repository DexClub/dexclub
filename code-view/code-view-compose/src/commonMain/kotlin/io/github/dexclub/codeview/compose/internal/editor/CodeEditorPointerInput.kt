package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache

internal fun Modifier.codeEditorDesktopPointerInput(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    lineHeightPx: Float,
    onFieldValueChange: (TextFieldValue) -> Unit,
    requestContentFocus: () -> Unit,
    requestImeFocus: () -> Unit,
    onInterruptInputAnchor: () -> Unit,
    onAnyPointerEditing: () -> Unit,
): Modifier {
    return pointerInput(layoutSnapshot.text, lineHeightPx) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            requestContentFocus()
            requestImeFocus()
            onInterruptInputAnchor()
            onAnyPointerEditing()

            val anchorOffset = resolveEditorTextOffset(
                layoutSnapshot = layoutSnapshot,
                lineLayoutCache = lineLayoutCache,
                lineHeightPx = lineHeightPx,
                position = down.position,
            )
            onFieldValueChange(
                TextFieldValue(
                    text = layoutSnapshot.text,
                    selection = TextRange(anchorOffset),
                )
            )

            drag(down.id) { change ->
                val targetOffset = resolveEditorTextOffset(
                    layoutSnapshot = layoutSnapshot,
                    lineLayoutCache = lineLayoutCache,
                    lineHeightPx = lineHeightPx,
                    position = change.position,
                )
                onFieldValueChange(
                    TextFieldValue(
                        text = layoutSnapshot.text,
                        selection = TextRange(anchorOffset, targetOffset),
                    )
                )
                change.consume()
            }
        }
    }
}

internal fun Modifier.codeEditorTouchPointerInput(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    lineHeightPx: Float,
    onFieldValueChange: (TextFieldValue) -> Unit,
    requestContentFocus: () -> Unit,
    requestImeFocus: () -> Unit,
    onInterruptInputAnchor: () -> Unit,
    onAnyPointerEditing: () -> Unit,
    onLongPressSelection: ((textOffset: Int, selection: TextRange, position: Offset) -> Unit)? = null,
): Modifier {
    return pointerInput(layoutSnapshot.text, lineHeightPx) {
        detectTapGestures(
            onTap = { position ->
                requestContentFocus()
                requestImeFocus()
                onInterruptInputAnchor()
                onAnyPointerEditing()

                val offset = resolveEditorTextOffset(
                    layoutSnapshot = layoutSnapshot,
                    lineLayoutCache = lineLayoutCache,
                    lineHeightPx = lineHeightPx,
                    position = position,
                )
                onFieldValueChange(
                    TextFieldValue(
                        text = layoutSnapshot.text,
                        selection = TextRange(offset),
                    )
                )
            },
            onLongPress = { position ->
                requestContentFocus()
                requestImeFocus()
                onInterruptInputAnchor()
                onAnyPointerEditing()

                val offset = resolveEditorTextOffset(
                    layoutSnapshot = layoutSnapshot,
                    lineLayoutCache = lineLayoutCache,
                    lineHeightPx = lineHeightPx,
                    position = position,
                )
                val selection = resolveLongPressSelectionRange(
                    text = layoutSnapshot.text,
                    rawOffset = offset,
                )
                onFieldValueChange(
                    TextFieldValue(
                        text = layoutSnapshot.text,
                        selection = selection,
                    )
                )
                if (!selection.collapsed) {
                    onLongPressSelection?.invoke(offset, selection, position)
                }
            },
        )
    }
}

internal fun resolveEditorTextOffset(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    lineHeightPx: Float,
    position: Offset,
): Int {
    if (lineHeightPx <= 0f) return 0
    val maxContentHeight = (layoutSnapshot.lineCount * lineHeightPx).coerceAtLeast(0f)
    val safeY = position.y.coerceIn(0f, maxContentHeight)
    val lineIndex = (safeY / lineHeightPx)
        .toInt()
        .coerceIn(0, layoutSnapshot.lineCount - 1)
    val lineOffset = lineLayoutCache.offsetForPosition(
        lineIndex = lineIndex,
        xPx = position.x,
        clampToLineEnd = true,
    ) ?: 0
    return layoutSnapshot.positionToOffset(lineIndex, lineOffset)
}

private fun resolveLongPressSelectionRange(
    text: String,
    rawOffset: Int,
): TextRange {
    if (text.isEmpty()) return TextRange.Zero

    val safeOffset = rawOffset.coerceIn(0, text.length)
    val anchorIndex = when {
        safeOffset >= text.length -> text.lastIndex
        text[safeOffset].isSelectionWordChar() || !text[safeOffset].isWhitespace() -> safeOffset
        safeOffset > 0 && text[safeOffset - 1].isSelectionWordChar() -> safeOffset - 1
        safeOffset > 0 && !text[safeOffset - 1].isWhitespace() -> safeOffset - 1
        else -> safeOffset
    }

    if (anchorIndex !in text.indices) {
        return TextRange(safeOffset)
    }

    val anchorChar = text[anchorIndex]
    if (anchorChar == '\n' || anchorChar == '\r' || anchorChar.isWhitespace()) {
        return TextRange(safeOffset)
    }

    val predicate: (Char) -> Boolean = when {
        anchorChar.isSelectionWordChar() -> { char -> char.isSelectionWordChar() }
        else -> { char -> !char.isWhitespace() && char != '\n' && char != '\r' && !char.isSelectionWordChar() }
    }

    var start = anchorIndex
    while (start > 0 && predicate(text[start - 1])) {
        start -= 1
    }

    var end = anchorIndex + 1
    while (end < text.length && predicate(text[end])) {
        end += 1
    }

    return TextRange(start, end)
}

private fun Char.isSelectionWordChar(): Boolean {
    return isLetterOrDigit() || this == '_' || this == '$'
}
