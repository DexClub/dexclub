package io.github.dexclub.mcp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class McpExportTextSanitizationTest {
    @Test
    fun exportTextResultSanitizesVolatileMcpExportPaths() {
        val raw = """
            /* loaded from: C:\Users\Gang\repo\.dexclub\targets\target-1\cache\exports\tmp\mcp-export-123456\input.dex */
            public class Sample {}
        """.trimIndent()

        val sanitized = raw.stabilizeMcpExportText()

        assertTrue(sanitized.contains("public class Sample {}"))
        assertEquals(false, sanitized.contains("loaded from:"))
        assertEquals(false, sanitized.contains(".dexclub"))
        assertEquals(false, sanitized.contains("mcp-export-"))
        assertEquals(false, sanitized.contains("<mcp-export-input.dex>"))
    }
}
