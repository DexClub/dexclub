package io.github.dexclub.codeview.compose.internal.interaction

import androidx.compose.ui.geometry.Offset
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshotFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CodePointerPositionResolverTest {
    private val snapshot = CodeLayoutSnapshotFactory.create("abc\ndef")
    private val charWidthPx = 10f
    private val lineHeightPx = 20f

    @Test
    fun resolveTextOffsetForPosition_returnsOffsetInsideLine() {
        val offset = resolveTextOffsetForPosition(
            layoutSnapshot = snapshot,
            position = Offset(15f, 5f),
            charWidthPx = charWidthPx,
            lineHeightPx = lineHeightPx,
            clampToLineEnd = true,
        )

        assertEquals(1, offset)
    }

    @Test
    fun resolveTextOffsetForPosition_clampsToLineEndWhenEnabled() {
        val offset = resolveTextOffsetForPosition(
            layoutSnapshot = snapshot,
            position = Offset(35f, 5f),
            charWidthPx = charWidthPx,
            lineHeightPx = lineHeightPx,
            clampToLineEnd = true,
        )

        assertEquals(3, offset)
    }

    @Test
    fun resolveTextOffsetForPosition_rejectsAfterLineEndWhenClampDisabled() {
        val offset = resolveTextOffsetForPosition(
            layoutSnapshot = snapshot,
            position = Offset(35f, 5f),
            charWidthPx = charWidthPx,
            lineHeightPx = lineHeightPx,
            clampToLineEnd = false,
        )

        assertNull(offset)
    }

    @Test
    fun resolveTextOffsetForPosition_returnsNullOutsideContentBounds() {
        assertNull(
            resolveTextOffsetForPosition(
                layoutSnapshot = snapshot,
                position = Offset(-1f, 5f),
                charWidthPx = charWidthPx,
                lineHeightPx = lineHeightPx,
                clampToLineEnd = true,
            )
        )
        assertNull(
            resolveTextOffsetForPosition(
                layoutSnapshot = snapshot,
                position = Offset(10f, 45f),
                charWidthPx = charWidthPx,
                lineHeightPx = lineHeightPx,
                clampToLineEnd = true,
            )
        )
    }

    @Test
    fun resolveTextOffsetForPosition_mapsSecondLineCorrectly() {
        val offset = resolveTextOffsetForPosition(
            layoutSnapshot = snapshot,
            position = Offset(10f, 25f),
            charWidthPx = charWidthPx,
            lineHeightPx = lineHeightPx,
            clampToLineEnd = true,
        )

        assertEquals(5, offset)
    }
}
