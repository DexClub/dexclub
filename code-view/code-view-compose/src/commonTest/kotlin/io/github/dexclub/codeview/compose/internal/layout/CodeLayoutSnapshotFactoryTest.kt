package io.github.dexclub.codeview.compose.internal.layout

import io.github.dexclub.codeview.core.token.CodeTokenKind
import io.github.dexclub.codeview.core.token.CodeTokenSpan
import io.github.dexclub.codeview.core.text.TextOffsetRange
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeLayoutSnapshotFactoryTest {
    @Test
    fun create_splitsMixedLineEndingsAndTrailingNewline() {
        val snapshot = CodeLayoutSnapshotFactory.create("ab\r\nc\n")

        assertEquals(3, snapshot.lineCount)
        assertEquals("ab", snapshot.lineAt(0).content)
        assertEquals("c", snapshot.lineAt(1).content)
        assertEquals("", snapshot.lineAt(2).content)

        assertEquals(0, snapshot.lineAt(0).startOffset)
        assertEquals(4, snapshot.lineAt(1).startOffset)
        assertEquals(6, snapshot.lineAt(2).startOffset)
    }

    @Test
    fun offsetAndPositionMapping_handlesLineBoundaries() {
        val snapshot = CodeLayoutSnapshotFactory.create("ab\r\nc\n")

        val newlineBoundary = snapshot.offsetToPosition(3)
        assertEquals(0, newlineBoundary.lineIndex)
        assertEquals(2, newlineBoundary.columnIndex)

        val secondLine = snapshot.offsetToPosition(5)
        assertEquals(1, secondLine.lineIndex)
        assertEquals(1, secondLine.columnIndex)

        assertEquals(5, snapshot.positionToOffset(1, 1))
        assertEquals(6, snapshot.positionToOffset(2, 0))
    }

    @Test
    fun withDecorations_reusesTextLayoutAndAppliesTokenSlices() {
        val base = CodeLayoutSnapshotFactory.create("abc\ndef")

        val decorated = CodeLayoutSnapshotFactory.withDecorations(
            base = base,
            tokens = listOf(
                CodeTokenSpan(
                    range = TextOffsetRange(start = 1, end = 5),
                    kind = CodeTokenKind.Keyword,
                )
            ),
        )

        assertEquals(base.lines, decorated.lines)
        assertEquals(base.lineStarts.toList(), decorated.lineStarts.toList())
        assertEquals(1, decorated.tokensForLine(0).size)
        assertEquals(1, decorated.tokensForLine(1).size)
        assertEquals(1, decorated.tokensForLine(0).single().startColumn)
        assertEquals(3, decorated.tokensForLine(0).single().endColumn)
        assertEquals(0, decorated.tokensForLine(1).single().startColumn)
        assertEquals(1, decorated.tokensForLine(1).single().endColumn)
    }

    @Test
    fun withDecorations_sortsTokensByStartColumnPerLine() {
        val decorated = CodeLayoutSnapshotFactory.create(
            text = "abcdef",
            tokens = listOf(
                CodeTokenSpan(
                    range = TextOffsetRange(start = 4, end = 6),
                    kind = CodeTokenKind.StringLiteral,
                ),
                CodeTokenSpan(
                    range = TextOffsetRange(start = 1, end = 3),
                    kind = CodeTokenKind.Keyword,
                ),
            ),
        )

        assertEquals(listOf(1, 4), decorated.tokensForLine(0).map { it.startColumn })
    }
}
