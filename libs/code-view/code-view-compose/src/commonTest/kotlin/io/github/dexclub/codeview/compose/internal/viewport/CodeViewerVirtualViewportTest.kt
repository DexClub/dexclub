package io.github.dexclub.codeview.compose.internal.viewport

import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshotFactory

import kotlin.test.Test
import kotlin.test.assertEquals

class CodeViewerVirtualViewportTest {
    @Test
    fun resolveScrollPastEndReservedHeightPx_returnsZeroWhenDisabled() {
        assertEquals(
            expected = 0f,
            actual = resolveScrollPastEndReservedHeightPx(
                scrollPastEnd = 0,
                lineHeightPx = 20f,
            ),
        )
        assertEquals(
            expected = 0f,
            actual = resolveScrollPastEndReservedHeightPx(
                scrollPastEnd = -3,
                lineHeightPx = 20f,
            ),
        )
    }

    @Test
    fun resolveScrollPastEndReservedHeightPx_scalesWithLineHeight() {
        assertEquals(
            expected = 100f,
            actual = resolveScrollPastEndReservedHeightPx(
                scrollPastEnd = 5,
                lineHeightPx = 20f,
            ),
        )
    }

    @Test
    fun resolveCodeViewerVerticalLayout_keepsViewportHeightWhenDocumentIsShort() {
        val layout = resolveCodeViewerVerticalLayout(
            lineCount = 3,
            lineHeightPx = 20f,
            viewportHeightPx = 120f,
            scrollPastEnd = 0,
        )

        assertEquals(120f, layout.totalContentHeightPx)
        assertEquals(0f, layout.maxVerticalScrollPx)
    }

    @Test
    fun resolveCodeViewerVerticalLayout_includesScrollPastEndReserve() {
        val layout = resolveCodeViewerVerticalLayout(
            lineCount = 20,
            lineHeightPx = 20f,
            viewportHeightPx = 100f,
            scrollPastEnd = 3,
        )

        assertEquals(460f, layout.totalContentHeightPx)
        assertEquals(360f, layout.maxVerticalScrollPx)
    }

    @Test
    fun resolveCodeViewerViewportState_derivesVisibleLineFromScrollOffset() {
        val snapshot = CodeLayoutSnapshotFactory.create(
            text = (0..9).joinToString("\n") { it.toString() },
        )

        val viewport = resolveCodeViewerViewportState(
            layoutSnapshot = snapshot,
            verticalScrollPx = 45f,
            horizontalScrollPx = 12f,
            viewportWidthPx = 80f,
            viewportHeightPx = 40f,
            lineHeightPx = 20f,
            maxHorizontalScrollPx = 100f,
            extraBottomLines = 0,
        )

        assertEquals(2, viewport.firstVisibleLine)
        assertEquals(12f, viewport.horizontalScrollPx)
    }
}
