package io.github.dexclub.core.impl.dex

import io.github.dexclub.core.api.dex.DexQueryError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DexQueryParserTest {
    private val parser = DexQueryParser()

    @Test
    fun publicQueriesMapToBindingQueries() {
        assertEquals(listOf("sample"), parser.parseFindClass("""{"searchPackages":["sample"]}""").searchPackages)
        assertEquals(true, parser.parseFindMethod("""{"findFirst":true}""").findFirst)
        assertEquals(false, parser.parseFindField("{}").findFirst)
    }

    @Test
    fun publicQueriesRejectNativeResultPointers() {
        assertInvalid { parser.parseFindClass("""{"searchInClasses":[]}""") }
        assertInvalid { parser.parseFindMethod("""{"searchInMethods":[]}""") }
        assertInvalid { parser.parseFindField("""{"searchInFields":[]}""") }
    }

    private fun assertInvalid(block: () -> Unit) {
        assertEquals(
            io.github.dexclub.core.api.dex.DexQueryErrorReason.InvalidQuery,
            assertFailsWith<DexQueryError> { block() }.reason,
        )
    }
}
