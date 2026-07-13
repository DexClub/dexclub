package io.github.dexclub.cli

import io.github.dexclub.core.app.createDefaultAppServices
import io.github.dexclub.core.api.workspace.WorkspaceRef
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.api.workspace.WorkspaceStatus
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CliAppFailureRenderingTest {
    @Test
    fun unexpectedFailureIncludesDebugHintOrStackTraceWhenEnabled() {
        val baseServices = createDefaultAppServices()
        val failingServices = baseServices.copy(
            workspace = object : WorkspaceService by baseServices.workspace {
                override fun loadStatus(ref: WorkspaceRef): WorkspaceStatus {
                    error("boom")
                }
            },
        )
        val plainApp = CliApp(
            services = failingServices,
            cwdProvider = { createTempDirectory("dexclub-unexpected").toString() },
            debugStacktraceEnabled = false,
        )

        val plain = runCli(plainApp, listOf("status"))
        assertEquals(2, plain.exitCode)
        assertTrue(plain.stderr.contains("Error: boom"))
        assertTrue(plain.stderr.contains("Set DEXCLUB_CLI_DEBUG_STACKTRACE=true to print the stack trace."))
        assertTrue(!plain.stderr.contains("java.lang.IllegalStateException"))

        val debugApp = CliApp(
            services = failingServices,
            cwdProvider = { createTempDirectory("dexclub-unexpected-debug").toString() },
            debugStacktraceEnabled = true,
        )
        val debug = runCli(debugApp, listOf("status"))
        assertEquals(2, debug.exitCode)
        assertTrue(debug.stderr.contains("Unexpected internal error. Disable DEXCLUB_CLI_DEBUG_STACKTRACE or unset it to hide stack traces."))
        assertTrue(debug.stderr.contains("java.lang.IllegalStateException: boom"))
        assertTrue(debug.stderr.contains("at io.github.dexclub.cli.CliAppFailureRenderingTest"))
    }
}
