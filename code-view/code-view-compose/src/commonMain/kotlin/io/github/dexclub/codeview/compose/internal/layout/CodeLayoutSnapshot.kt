package io.github.dexclub.codeview.compose.internal.layout

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.core.text.CodeSelection
import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection
import io.github.dexclub.codeview.core.text.TextPosition

internal data class CodeLayoutSnapshot(
    val text: String,
    val lineStarts: IntArray,
    val lines: List<CodeLineLayout>,
    val maxLineLength: Int,
    val tokensByLine: List<List<CodeLineTokenSpan>>,
    val annotations: List<CodeAnnotation>,
) {
    init {
        require(lines.isNotEmpty()) { "lines 不能为空" }
        require(lineStarts.size == lines.size) {
            "lineStarts.size 必须与 lines.size 一致: ${lineStarts.size} != ${lines.size}"
        }
        require(tokensByLine.size == lines.size) {
            "tokensByLine.size 必须与 lines.size 一致: ${tokensByLine.size} != ${lines.size}"
        }
    }

    val lineCount: Int
        get() = lines.size


    fun lineAt(lineIndex: Int): CodeLineLayout {
        require(lineIndex in lines.indices) { "lineIndex 超出范围: $lineIndex" }
        return lines[lineIndex]
    }


    fun lineLength(lineIndex: Int): Int = lineAt(lineIndex).length


    fun tokensForLine(lineIndex: Int): List<CodeLineTokenSpan> = tokensByLine[lineAt(lineIndex).lineIndex]


    fun findAnnotationAtOffset(offset: Int): CodeAnnotation? {
        val safeOffset = offset.coerceIn(0, text.length)
        return annotations
            .filter { annotation -> annotation.range.contains(safeOffset) }
            .minByOrNull { annotation -> annotation.range.end - annotation.range.start }
    }


    fun clampCursor(cursor: Cursor?): Cursor? {
        if (cursor == null) return null
        val safeLine = cursor.line.coerceIn(0, lineCount - 1)
        val safeOffset = cursor.offset.coerceIn(0, lineLength(safeLine))
        return Cursor(
            line = safeLine,
            offset = safeOffset,
        )
    }


    fun clampSelection(selection: LineSelection?): LineSelection? {
        if (selection == null) return null
        val startLine = selection.startLine.coerceIn(0, lineCount - 1)
        val endLine = selection.endLine.coerceIn(0, lineCount - 1)
        return LineSelection(
            startLine = startLine,
            startOffset = selection.startOffset.coerceIn(0, lineLength(startLine)),
            endLine = endLine,
            endOffset = selection.endOffset.coerceIn(0, lineLength(endLine)),
        )
    }


    fun offsetToPosition(offset: Int): TextPosition {
        val safeOffset = offset.coerceIn(0, text.length)
        val lineIndex = findLineIndexForOffset(safeOffset)
        val line = lines[lineIndex]
        return TextPosition(
            lineIndex = lineIndex,
            columnIndex = (safeOffset - line.startOffset).coerceIn(0, line.length),
        )
    }


    fun offsetToCursor(offset: Int): Cursor {
        val position = offsetToPosition(offset)
        return Cursor(
            line = position.lineIndex,
            offset = position.columnIndex,
        )
    }


    fun positionToOffset(
        lineIndex: Int,
        columnIndex: Int,
    ): Int {
        val safeLineIndex = lineIndex.coerceIn(0, lineCount - 1)
        val line = lines[safeLineIndex]
        val safeColumnIndex = columnIndex.coerceIn(0, line.length)
        return line.startOffset + safeColumnIndex
    }


    fun cursorToOffset(cursor: Cursor): Int = positionToOffset(cursor.line, cursor.offset)


    fun lineSelectionToCodeSelection(selection: LineSelection?): CodeSelection? {
        val safeSelection = clampSelection(selection) ?: return null
        return CodeSelection(
            anchorOffset = positionToOffset(safeSelection.startLine, safeSelection.startOffset),
            caretOffset = positionToOffset(safeSelection.endLine, safeSelection.endOffset),
        )
    }


    fun codeSelectionToLineSelection(selection: CodeSelection?): LineSelection? {
        if (selection == null) return null
        val anchorPosition = offsetToPosition(selection.anchorOffset)
        val caretPosition = offsetToPosition(selection.caretOffset)
        return LineSelection(
            startLine = anchorPosition.lineIndex,
            startOffset = anchorPosition.columnIndex,
            endLine = caretPosition.lineIndex,
            endOffset = caretPosition.columnIndex,
        )
    }


    fun selectionFromCursor(cursor: Cursor?): LineSelection? {
        val safeCursor = clampCursor(cursor) ?: return null
        return LineSelection.collapsed(
            line = safeCursor.line,
            offset = safeCursor.offset,
        )
    }


    fun cursorFromSelection(selection: CodeSelection?): Cursor? {
        if (selection == null) return null
        return offsetToCursor(selection.caretOffset)
    }


    private fun findLineIndexForOffset(offset: Int): Int {
        var low = 0
        var high = lineStarts.lastIndex

        while (low <= high) {
            val middle = (low + high) ushr 1
            val lineStart = lineStarts[middle]
            if (lineStart == offset) {
                return middle
            }
            if (lineStart < offset) {
                low = middle + 1
            } else {
                high = middle - 1
            }
        }

        return high.coerceAtLeast(0)
    }
}
