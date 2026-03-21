package io.github.dexclub.app.scene.workspace

import io.github.dexclub.codeview.core.text.LineSelection

internal fun extractSelectedText(
    text: String,
    selection: LineSelection?,
): String {
    val lines = splitCodeViewLines(text)
    if (lines.isEmpty()) return ""
    val safeSelection = selection?.let {
        clampSelection(
            selection = it,
            lineCount = lines.size,
            lineLengthProvider = { lineIndex -> lines[lineIndex].length },
        )
    } ?: return ""
    if (safeSelection.isCollapsed) return ""

    val normalized = safeSelection.normalized()
    if (normalized.startLine == normalized.endLine) {
        return lines[normalized.startLine].substring(
            startIndex = normalized.startOffset,
            endIndex = normalized.endOffset,
        )
    }

    return buildString {
        append(lines[normalized.startLine].substring(normalized.startOffset))
        append('\n')
        for (lineIndex in (normalized.startLine + 1) until normalized.endLine) {
            append(lines[lineIndex])
            append('\n')
        }
        append(lines[normalized.endLine].substring(0, normalized.endOffset))
    }
}

internal fun resolveSelectAllSelection(
    text: String,
): LineSelection? {
    val lines = splitCodeViewLines(text)
    if (lines.isEmpty()) return null
    val lastLineIndex = lines.lastIndex
    return LineSelection(
        startLine = 0,
        startOffset = 0,
        endLine = lastLineIndex,
        endOffset = lines[lastLineIndex].length,
    )
}

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

private fun splitCodeViewLines(
    text: String,
): List<String> {
    if (text.isEmpty()) return listOf("")

    val lines = mutableListOf<String>()
    var lineStart = 0
    var index = 0
    while (index < text.length) {
        when (text[index]) {
            '\n' -> {
                lines += text.substring(lineStart, index)
                index += 1
                lineStart = index
            }

            '\r' -> {
                lines += text.substring(lineStart, index)
                index += if (index + 1 < text.length && text[index + 1] == '\n') 2 else 1
                lineStart = index
            }

            else -> {
                index += 1
            }
        }
    }
    lines += text.substring(lineStart)
    return lines
}
