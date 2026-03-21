package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal fun TextFieldValue.replaceSelection(replacement: String): TextFieldValue {
    return replaceRange(
        range = selection,
        replacement = replacement,
    )
}

internal fun TextFieldValue.replaceRange(
    range: TextRange,
    replacement: String,
): TextFieldValue {
    val selectionStart = range.normalizedStart
    val selectionEnd = range.normalizedEnd
    val nextText = text.replaceRange(selectionStart, selectionEnd, replacement)
    val nextCursor = selectionStart + replacement.length
    return TextFieldValue(
        text = nextText,
        selection = TextRange(nextCursor),
    )
}

internal fun TextFieldValue.deleteBackward(): TextFieldValue {
    if (!selection.collapsed) {
        return replaceSelection("")
    }
    val cursorOffset = normalizedCaretOffset()
    if (cursorOffset <= 0) return copy(composition = null)
    val nextText = text.removeRange(cursorOffset - 1, cursorOffset)
    return TextFieldValue(
        text = nextText,
        selection = TextRange(cursorOffset - 1),
    )
}

internal fun TextFieldValue.deleteForward(): TextFieldValue {
    if (!selection.collapsed) {
        return replaceSelection("")
    }
    val cursorOffset = normalizedCaretOffset()
    if (cursorOffset >= text.length) return copy(composition = null)
    val nextText = text.removeRange(cursorOffset, cursorOffset + 1)
    return TextFieldValue(
        text = nextText,
        selection = TextRange(cursorOffset),
    )
}

internal fun TextFieldValue.moveCaretTo(
    targetOffset: Int,
    extendSelection: Boolean,
): TextFieldValue {
    val safeOffset = targetOffset.coerceIn(0, text.length)
    val nextSelection = if (extendSelection) {
        TextRange(selection.start, safeOffset)
    } else {
        TextRange(safeOffset)
    }
    return copy(
        selection = nextSelection,
        composition = null,
    )
}

internal fun TextFieldValue.selectedText(): String {
    val selectionStart = selection.normalizedStart
    val selectionEnd = selection.normalizedEnd
    if (selectionStart == selectionEnd) return ""
    return text.substring(selectionStart, selectionEnd)
}

internal fun TextFieldValue.normalizedCaretOffset(): Int {
    return selection.end.coerceIn(0, text.length)
}

internal val TextRange.normalizedStart: Int
    get() = minOf(start, end)

internal val TextRange.normalizedEnd: Int
    get() = maxOf(start, end)
