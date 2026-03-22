package io.github.dexclub.codeview.compose

import androidx.compose.ui.text.TextRange

internal data class AndroidEditingSnapshot(
    val text: String,
    val selection: TextRange,
    val composition: TextRange?,
    val insertedImeRange: TextRange?,
)

internal fun clampTextRange(
    range: TextRange,
    textLength: Int,
): TextRange {
    return TextRange(
        start = range.start.coerceIn(0, textLength),
        end = range.end.coerceIn(0, textLength),
    )
}

internal fun resolveInsertedTextSelection(
    textLength: Int,
    newCursorPosition: Int,
): TextRange {
    val cursorOffset = if (newCursorPosition > 0) {
        textLength + newCursorPosition - 1
    } else {
        newCursorPosition
    }.coerceIn(0, textLength)
    return TextRange(cursorOffset)
}

internal fun effectiveOffsetToDocumentOffset(
    offset: Int,
    insertedImeRange: TextRange?,
): Int {
    if (insertedImeRange == null) return offset
    return when {
        offset <= insertedImeRange.start -> offset
        offset >= insertedImeRange.end -> offset - insertedImeRange.length
        else -> insertedImeRange.start
    }
}
