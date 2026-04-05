package io.github.dexclub.codeview.compose.internal.viewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeViewerCanvasTest {
    @Test
    fun resolveViewportRevealBottomReservePx_growsWithViewportShrinkUntilOneLine() {
        assertEquals(
            expected = 0f,
            actual = resolveViewportRevealBottomReservePx(
                maxObservedViewportHeightPx = 800f,
                currentViewportHeightPx = 800f,
                lineHeightPx = 20f,
                imeBottomInsetPx = 0f,
            ),
        )
        assertEquals(
            expected = 8f,
            actual = resolveViewportRevealBottomReservePx(
                maxObservedViewportHeightPx = 800f,
                currentViewportHeightPx = 792f,
                lineHeightPx = 20f,
                imeBottomInsetPx = 0f,
            ),
        )
        assertEquals(
            expected = 20f,
            actual = resolveViewportRevealBottomReservePx(
                maxObservedViewportHeightPx = 800f,
                currentViewportHeightPx = 740f,
                lineHeightPx = 20f,
                imeBottomInsetPx = 0f,
            ),
        )
    }

    @Test
    fun resolveViewportRevealBottomReservePx_usesDynamicImeInsetBeforeViewportShrinks() {
        assertEquals(
            expected = 12f,
            actual = resolveViewportRevealBottomReservePx(
                maxObservedViewportHeightPx = 800f,
                currentViewportHeightPx = 800f,
                lineHeightPx = 20f,
                imeBottomInsetPx = 150f,
            ),
        )
    }

    @Test
    fun resolveCursorVerticalRevealTargetPx_movesContinuouslyWithReserve() {
        val targetWithoutReserve = resolveCursorVerticalRevealTargetPx(
            currentVerticalScrollPx = 0f,
            cursorLine = 3,
            lineHeightPx = 20f,
            viewportHeightPx = 79f,
            preferredBottomReservePx = 0f,
        )
        val targetWithPartialReserve = resolveCursorVerticalRevealTargetPx(
            currentVerticalScrollPx = 0f,
            cursorLine = 3,
            lineHeightPx = 20f,
            viewportHeightPx = 79f,
            preferredBottomReservePx = 8f,
        )

        assertTrue(targetWithPartialReserve > targetWithoutReserve)
        assertEquals(9f, targetWithPartialReserve)
    }

    @Test
    fun resolveLineNumberDigits_respectsConfiguredMinimum() {
        assertEquals(2, resolveLineNumberDigits(lineCount = 1, minDigits = 2))
        assertEquals(3, resolveLineNumberDigits(lineCount = 99, minDigits = 3))
        assertEquals(3, resolveLineNumberDigits(lineCount = 100, minDigits = 2))
        assertEquals(4, resolveLineNumberDigits(lineCount = 1000, minDigits = 2))
    }

    @Test
    fun resolveCodeContentViewportWidthPx_subtractsGutterWidth() {
        assertEquals(
            expected = 172f,
            actual = resolveCodeContentViewportWidthPx(
                viewportWidthPx = 220f,
                contentLeftInsetPx = 48f,
            ),
        )
        assertEquals(
            expected = 0f,
            actual = resolveCodeContentViewportWidthPx(
                viewportWidthPx = 24f,
                contentLeftInsetPx = 48f,
            ),
        )
    }

    @Test
    fun contentXToViewportX_includesLineNumberInset() {
        val viewportSnapshot = CodeViewerViewportSnapshot(
            verticalScrollPx = 0f,
            horizontalScrollPx = 20f,
            viewportWidthPx = 240f,
            viewportHeightPx = 160f,
            lineHeightPx = 20f,
            contentLeftInsetPx = 48f,
            contentViewportWidthPx = 192f,
            contentStartPaddingPx = 4f,
            contentEndPaddingPx = 0f,
        )

        assertEquals(62f, viewportSnapshot.contentXToViewportX(30f))
        assertEquals(62f, viewportSnapshot.contentXToHandleViewportX(30f))
    }
}
