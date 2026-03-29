package io.github.dexclub.codeview.compose.internal.viewport

import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshotFactory
import io.github.dexclub.codeview.core.text.Cursor
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeViewportStateTest {
    @Test
    fun revealCursor_scrollsVerticallyAndHorizontallyWhenNeeded() {
        val snapshot = CodeLayoutSnapshotFactory.create(
            text = "0\n1\n2\n3\n0123456789\n5",
        )
        val viewport = CodeViewportState(
            firstVisibleLine = 0,
            horizontalScrollPx = 0f,
            viewportWidthPx = 30f,
            viewportHeightPx = 40f,
            lineHeightPx = 20f,
        )

        val revealed = viewport.revealCursor(
            layout = snapshot,
            cursor = Cursor(line = 4, offset = 8),
            charWidthPx = 10f,
        )

        assertEquals(3, revealed.firstVisibleLine)
        assertEquals(60f, revealed.horizontalScrollPx)
    }

    @Test
    fun revealCursor_keepsViewportWhenCursorAlreadyVisible() {
        val snapshot = CodeLayoutSnapshotFactory.create(
            text = "0\n1\n2\n3\n0123456789\n5",
        )
        val viewport = CodeViewportState(
            firstVisibleLine = 3,
            horizontalScrollPx = 60f,
            viewportWidthPx = 30f,
            viewportHeightPx = 40f,
            lineHeightPx = 20f,
        )

        val revealed = viewport.revealCursor(
            layout = snapshot,
            cursor = Cursor(line = 4, offset = 8),
            charWidthPx = 10f,
        )

        assertEquals(3, revealed.firstVisibleLine)
        assertEquals(60f, revealed.horizontalScrollPx)
    }

    @Test
    fun revealCursor_withBottomReserveLines_movesCursorSlightlyEarlierThanBottomEdge() {
        val snapshot = CodeLayoutSnapshotFactory.create(
            text = (0..9).joinToString("\n") { it.toString() },
        )
        val viewport = CodeViewportState(
            firstVisibleLine = 0,
            horizontalScrollPx = 0f,
            viewportWidthPx = 30f,
            viewportHeightPx = 80f,
            lineHeightPx = 20f,
        )

        val revealed = viewport.revealCursor(
            layout = snapshot,
            cursor = Cursor(line = 3, offset = 0),
            charWidthPx = 10f,
            preferredBottomReserveLines = 1,
        )

        assertEquals(3, viewport.lastVisibleLine(snapshot.lineCount))
        assertEquals(1, revealed.firstVisibleLine)
    }

    @Test
    fun clamp_allowsScrollingIntoScrollPastEndReserve() {
        val snapshot = CodeLayoutSnapshotFactory.create(
            text = "0\n1\n2\n3\n4\n5",
        )
        val viewport = CodeViewportState(
            firstVisibleLine = 8,
            horizontalScrollPx = 0f,
            viewportWidthPx = 30f,
            viewportHeightPx = 40f,
            lineHeightPx = 20f,
        )

        val clamped = viewport.clamp(
            layout = snapshot,
            extraBottomLines = 5,
        )

        assertEquals(8, clamped.firstVisibleLine)
    }

    @Test
    fun renderLineRange_expandsVisibleLinesWithOverscan() {
        val viewport = CodeViewportState(
            firstVisibleLine = 3,
            horizontalScrollPx = 0f,
            viewportWidthPx = 30f,
            viewportHeightPx = 40f,
            lineHeightPx = 20f,
        )

        assertEquals(
            1..6,
            viewport.renderLineRange(
                lineCount = 10,
                extraLeadingLines = 2,
                extraTrailingLines = 2,
            )
        )
    }
}
