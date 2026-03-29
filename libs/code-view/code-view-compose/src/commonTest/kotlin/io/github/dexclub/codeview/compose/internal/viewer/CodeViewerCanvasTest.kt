package io.github.dexclub.codeview.compose.internal.viewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeViewerCanvasTest {
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
}
