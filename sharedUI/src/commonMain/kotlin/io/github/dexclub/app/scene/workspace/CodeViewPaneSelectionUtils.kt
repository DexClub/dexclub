package io.github.dexclub.app.scene.workspace

import io.github.dexclub.codeview.core.text.LineSelection

internal fun clampSelection(
    selection: LineSelection,
    lineCount: Int,
    lineLengthProvider: (Int) -> Int,
): LineSelection? {
    if (lineCount == 0) return null
    val normalized = selection.normalized()
    val maxLineIndex = lineCount - 1
    val startLine = normalized.startLine.coerceIn(0, maxLineIndex)
    val endLine = normalized.endLine.coerceIn(startLine, maxLineIndex)
    val startOffset = normalized.startOffset.coerceIn(0, lineLengthProvider(startLine))
    val endOffset = normalized.endOffset.coerceIn(0, lineLengthProvider(endLine))
    return LineSelection(
        startLine = startLine,
        startOffset = startOffset,
        endLine = endLine,
        endOffset = endOffset,
    )
}
