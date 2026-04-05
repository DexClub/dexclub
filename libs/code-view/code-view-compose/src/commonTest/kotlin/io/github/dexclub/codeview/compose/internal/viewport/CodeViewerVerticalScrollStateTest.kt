package io.github.dexclub.codeview.compose.internal.viewport

import kotlin.test.Test
import kotlin.test.assertEquals

class CodeViewerVerticalScrollStateTest {
    @Test
    fun dispatchRawDelta_preservesFractionalScrollOffset() {
        val state = CodeViewerVerticalScrollState()
        state.updateBounds(100f)

        state.dispatchRawDelta(0.4f)
        state.dispatchRawDelta(0.4f)

        assertEquals(0.8f, state.value)
    }

    @Test
    fun dispatchRawDelta_clampsWithinBounds() {
        val state = CodeViewerVerticalScrollState()
        state.updateBounds(10f)

        state.dispatchRawDelta(16f)
        assertEquals(10f, state.value)

        state.dispatchRawDelta(-20f)
        assertEquals(0f, state.value)
    }
}
