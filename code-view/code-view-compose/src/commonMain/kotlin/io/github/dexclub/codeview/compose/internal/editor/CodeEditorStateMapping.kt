package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.text.TextRange
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.core.text.CodeSelection
import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection

internal fun resolveExternalSelection(
    layoutSnapshot: CodeLayoutSnapshot,
    selection: LineSelection?,
    cursor: Cursor?,
): CodeSelection? {
    val safeSelection = layoutSnapshot.clampSelection(selection)
    val safeCursor = layoutSnapshot.clampCursor(cursor)

    if (safeSelection == null) {
        return safeCursor?.let { safe -> CodeSelection.collapsed(layoutSnapshot.cursorToOffset(safe)) }
    }

    val normalizedSelection = safeSelection.normalized()
    val startOffset = layoutSnapshot.positionToOffset(
        normalizedSelection.startLine,
        normalizedSelection.startOffset,
    )
    val endOffset = layoutSnapshot.positionToOffset(
        normalizedSelection.endLine,
        normalizedSelection.endOffset,
    )
    val cursorOffset = safeCursor?.let(layoutSnapshot::cursorToOffset)

    return when {
        cursorOffset == null -> CodeSelection(
            anchorOffset = startOffset,
            caretOffset = endOffset,
        )

        cursorOffset == startOffset && startOffset != endOffset -> CodeSelection(
            anchorOffset = endOffset,
            caretOffset = startOffset,
        )

        else -> CodeSelection(
            anchorOffset = startOffset,
            caretOffset = endOffset,
        )
    }
}

internal fun TextRange.toCodeSelection(): CodeSelection =
    CodeSelection(
        anchorOffset = start,
        caretOffset = end,
    )

internal fun CodeSelection.toTextRange(): TextRange = TextRange(
    start = anchorOffset,
    end = caretOffset,
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

internal fun clampTextRangeOrNull(
    range: TextRange?,
    textLength: Int,
): TextRange? = range?.let {
    clampTextRange(
        range = it,
        textLength = textLength,
    )
}
