package io.github.dexclub.codeview.compose.internal.layout

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.core.text.TextOffsetRange
import io.github.dexclub.codeview.core.token.CodeTokenSpan

internal object CodeLayoutSnapshotFactory {
    fun create(
        text: String,
        tokens: List<CodeTokenSpan> = emptyList(),
        annotations: List<CodeAnnotation> = emptyList(),
    ): CodeLayoutSnapshot {
        val lines = buildLines(text)
        return create(
            text = text,
            lines = lines,
            tokens = tokens,
            annotations = annotations,
        )
    }


    fun withDecorations(
        base: CodeLayoutSnapshot,
        tokens: List<CodeTokenSpan> = emptyList(),
        annotations: List<CodeAnnotation> = emptyList(),
    ): CodeLayoutSnapshot {
        return create(
            text = base.text,
            lines = base.lines,
            tokens = tokens,
            annotations = annotations,
        )
    }


    private fun buildLines(text: String): List<CodeLineLayout> {
        val lines = mutableListOf<CodeLineLayout>()
        var lineIndex = 0
        var lineStart = 0
        var index = 0

        while (index < text.length) {
            when (text[index]) {
                '\r' -> {
                    lines += buildLine(
                        lineIndex = lineIndex,
                        lineStart = lineStart,
                        lineEndExclusive = index,
                    )
                    if (index + 1 < text.length && text[index + 1] == '\n') {
                        index += 1
                    }
                    lineIndex += 1
                    lineStart = index + 1
                }

                '\n' -> {
                    lines += buildLine(
                        lineIndex = lineIndex,
                        lineStart = lineStart,
                        lineEndExclusive = index,
                    )
                    lineIndex += 1
                    lineStart = index + 1
                }
            }
            index += 1
        }

        lines += buildLine(
            lineIndex = lineIndex,
            lineStart = lineStart,
            lineEndExclusive = text.length,
        )

        return lines
    }


    private fun buildLine(
        lineIndex: Int,
        lineStart: Int,
        lineEndExclusive: Int,
    ): CodeLineLayout {
        return CodeLineLayout(
            lineIndex = lineIndex,
            startOffset = lineStart,
            endOffsetExclusive = lineEndExclusive,
        )
    }


    private fun create(
        text: String,
        lines: List<CodeLineLayout>,
        tokens: List<CodeTokenSpan>,
        annotations: List<CodeAnnotation>,
    ): CodeLayoutSnapshot {
        val lineStarts = IntArray(lines.size) { index -> lines[index].startOffset }
        return CodeLayoutSnapshot(
            text = text,
            lineStarts = lineStarts,
            lines = lines,
            maxLineLength = lines.maxOf(CodeLineLayout::length),
            tokensByLine = buildTokensByLine(
                textLength = text.length,
                lines = lines,
                tokens = tokens,
            ),
            annotations = annotations.filter { annotation ->
                annotation.range.start < text.length && annotation.range.end <= text.length
            },
        )
    }


    private fun buildTokensByLine(
        textLength: Int,
        lines: List<CodeLineLayout>,
        tokens: List<CodeTokenSpan>,
    ): List<List<CodeLineTokenSpan>> {
        if (tokens.isEmpty()) {
            return List(lines.size) { emptyList<CodeLineTokenSpan>() }
        }

        val orderedTokens = tokens.sortedByStartOffsetIfNeeded()
        val tokensByLine = arrayOfNulls<MutableList<CodeLineTokenSpan>>(lines.size)
        val lineNeedsSort = BooleanArray(lines.size)
        var startLineIndexHint = 0
        var endLineIndexHint = 0

        orderedTokens.forEach { token ->
            val safeStart = token.range.start.coerceIn(0, textLength)
            val safeEnd = token.range.end.coerceIn(safeStart, textLength)
            if (safeStart >= safeEnd) return@forEach

            val startLineIndex = findLineIndexForOffset(
                lines = lines,
                offset = safeStart,
                hint = startLineIndexHint,
            )
            startLineIndexHint = startLineIndex
            val endLineIndex = findLineIndexForOffset(
                lines = lines,
                offset = safeEnd - 1,
                hint = maxOf(startLineIndex, endLineIndexHint),
            )
            endLineIndexHint = endLineIndex

            for (lineIndex in startLineIndex..endLineIndex) {
                val line = lines[lineIndex]
                val segmentStart = maxOf(safeStart, line.startOffset)
                val segmentEnd = minOf(safeEnd, line.endOffsetExclusive)
                if (segmentStart >= segmentEnd) continue

                val lineToken = CodeLineTokenSpan(
                    range = TextOffsetRange(
                        start = segmentStart,
                        end = segmentEnd,
                    ),
                    kind = token.kind,
                    startColumn = segmentStart - line.startOffset,
                    endColumn = segmentEnd - line.startOffset,
                )
                val lineTokens = tokensByLine[lineIndex] ?: mutableListOf<CodeLineTokenSpan>().also { created ->
                    tokensByLine[lineIndex] = created
                }
                val previousStartColumn = lineTokens.lastOrNull()?.startColumn ?: Int.MIN_VALUE
                if (previousStartColumn > lineToken.startColumn) {
                    lineNeedsSort[lineIndex] = true
                }
                lineTokens += lineToken
            }
        }

        return List(lines.size) { lineIndex ->
            val lineTokens = tokensByLine[lineIndex] ?: return@List emptyList<CodeLineTokenSpan>()
            if (lineNeedsSort[lineIndex]) {
                lineTokens.sortBy(CodeLineTokenSpan::startColumn)
            }
            lineTokens.toList()
        }
    }


    private fun findLineIndexForOffset(
        lines: List<CodeLineLayout>,
        offset: Int,
        hint: Int,
    ): Int {
        if (hint in lines.indices) {
            val line = lines[hint]
            val nextLineStart = lines.getOrNull(hint + 1)?.startOffset ?: Int.MAX_VALUE
            if (offset in line.startOffset until nextLineStart) {
                return hint
            }
            if (offset >= line.startOffset) {
                var currentIndex = hint
                while (currentIndex + 1 < lines.size && lines[currentIndex + 1].startOffset <= offset) {
                    currentIndex += 1
                }
                return currentIndex
            }
        }

        return findLineIndexForOffset(
            lines = lines,
            offset = offset,
        )
    }


    private fun findLineIndexForOffset(
        lines: List<CodeLineLayout>,
        offset: Int,
    ): Int {
        var low = 0
        var high = lines.lastIndex

        while (low <= high) {
            val middle = (low + high) ushr 1
            val lineStart = lines[middle].startOffset
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


    private fun List<CodeTokenSpan>.sortedByStartOffsetIfNeeded(): List<CodeTokenSpan> {
        if (size < 2) {
            return this
        }

        for (index in 1 until size) {
            val previous = this[index - 1]
            val current = this[index]
            if (
                current.range.start < previous.range.start ||
                (current.range.start == previous.range.start && current.range.end < previous.range.end)
            ) {
                return sortedWith(
                    compareBy<CodeTokenSpan> { token -> token.range.start }
                        .thenBy { token -> token.range.end },
                )
            }
        }

        return this
    }
}
