package io.github.dexclub.core.app.support

import kotlin.test.Test
import kotlin.test.assertEquals

class AppWindowingTest {
    @Test
    fun largeLimitDoesNotOverflowEndIndex() {
        val result = applyWindowSlice(
            items = listOf("zero", "one", "two"),
            offset = 1,
            limit = Int.MAX_VALUE,
        )

        assertEquals(listOf("one", "two"), result.items)
        assertEquals(Int.MAX_VALUE, result.limit)
        assertEquals(false, result.hasMore)
    }
}
