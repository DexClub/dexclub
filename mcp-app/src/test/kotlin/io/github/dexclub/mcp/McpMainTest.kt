package io.github.dexclub.mcp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class McpMainTest {
    @Test
    fun transportDefaultsToHttpForExistingLaunchers() {
        assertEquals(McpTransport.HTTP, parseMcpTransport(emptyArray()))
    }

    @Test
    fun transportAcceptsExplicitHttpAndStdio() {
        assertEquals(McpTransport.HTTP, parseMcpTransport(arrayOf("--transport=http")))
        assertEquals(McpTransport.HTTP, parseMcpTransport(arrayOf("--transport", "http")))
        assertEquals(McpTransport.STDIO, parseMcpTransport(arrayOf("--transport=stdio")))
        assertEquals(McpTransport.STDIO, parseMcpTransport(arrayOf("--transport", "stdio")))
    }

    @Test
    fun invalidTransportArgumentsFailInsteadOfStartingHttp() {
        listOf(
            arrayOf("--transport=sse"),
            arrayOf("--transport="),
            arrayOf("--transport"),
            arrayOf("--unknown"),
            arrayOf("--transport=stdio", "--transport=http"),
        ).forEach { args ->
            assertFailsWith<IllegalArgumentException>(args.joinToString(" ")) {
                parseMcpTransport(args)
            }
        }
    }

    @Test
    fun resolveRuntimeFilesDirUsesConfiguredDirectoryWhenPresent() {
        val configuredDir = mcpAppTestDir("dexclub-mcp").resolve("bin")
        val callerDir = mcpAppTestDir("caller")
        val runtimeFilesDir = resolveRuntimeFilesDir(
            configuredDirRaw = configuredDir.toString(),
            fallbackWorkingDir = callerDir,
        )

        assertEquals(
            configuredDir.toAbsolutePath().normalize(),
            runtimeFilesDir,
        )
    }

    @Test
    fun resolveRuntimeFilesDirFallsBackToCallerWorkingDirWithoutConfiguredDirectory() {
        val callerDir = mcpAppTestDir("caller")
        val runtimeFilesDir = resolveRuntimeFilesDir(
            configuredDirRaw = null,
            fallbackWorkingDir = callerDir.toAbsolutePath().normalize(),
        )

        assertEquals(
            callerDir.toAbsolutePath().normalize(),
            runtimeFilesDir,
        )
    }

    @Test
    fun loopbackHostsDoNotProduceExposureWarning() {
        listOf("localhost", "127.0.0.1", "127.12.34.56", "::1", "[::1]", "0:0:0:0:0:0:0:1")
            .forEach { host -> assertNull(nonLoopbackHostWarning(host), host) }
    }

    @Test
    fun nonLoopbackHostProducesTrustedNetworkWarning() {
        val warning = nonLoopbackHostWarning("0.0.0.0")

        assertNotNull(warning)
        assertEquals(
            "WARNING: DexClub MCP is listening on non-loopback host '0.0.0.0'. Only use this on a trusted network.",
            warning,
        )
    }
}
