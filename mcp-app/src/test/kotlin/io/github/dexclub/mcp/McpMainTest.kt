package io.github.dexclub.mcp

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class McpMainTest {
    @Test
    fun resolveRuntimeFilesDirUsesAppHomeBinWhenPresent() {
        val appHome = mcpAppTestDir("dexclub-mcp")
        val callerDir = mcpAppTestDir("caller")
        val runtimeFilesDir = resolveRuntimeFilesDir(
            appHomeRaw = appHome.toString(),
            fallbackWorkingDir = callerDir,
        )

        assertEquals(
            appHome.resolve("bin").toAbsolutePath().normalize(),
            runtimeFilesDir,
        )
    }

    @Test
    fun resolveRuntimeFilesDirFallsBackToCallerWorkingDirWithoutAppHome() {
        val callerDir = mcpAppTestDir("caller")
        val runtimeFilesDir = resolveRuntimeFilesDir(
            appHomeRaw = null,
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
