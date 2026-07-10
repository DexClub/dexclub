package io.github.dexclub.cli

import io.github.dexclub.core.api.shared.createDefaultServices
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CliHelpCommandTest {
    @Test
    fun parserErrorsUseExitCodeOneAndUsageOutput() {
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { createTempDirectory("dexclub-cli-empty").toString() },
        )

        val output = runCli(app, listOf("status", "--json", "extra"))
        assertEquals(1, output.exitCode)
        assertTrue(output.stderr.contains("Error: positional arguments must appear before options"))
        assertTrue(output.stderr.contains("Usage:"))
    }

    @Test
    fun helpCommandPrintsGeneralHelp() {
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { createTempDirectory("dexclub-cli-help").toString() },
        )

        val output = runCli(app, listOf("help"))
        assertEquals(0, output.exitCode)
        assertTrue(output.stdout.contains("DexClub CLI"))
        assertTrue(output.stdout.contains("Lifecycle Commands:"))
        assertTrue(output.stdout.contains("Run 'cli help <command>' for command-specific details."))
    }

    @Test
    fun emptyArgvPrintsGeneralHelp() {
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { createTempDirectory("dexclub-cli-empty-help").toString() },
        )

        val output = runCli(app, emptyList())
        assertEquals(0, output.exitCode)
        assertTrue(output.stdout.contains("DexClub CLI"))
        assertTrue(output.stdout.contains("Resource Commands:"))
    }

    @Test
    fun topLevelHelpFlagPrintsGeneralHelp() {
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { createTempDirectory("dexclub-cli-help-flag").toString() },
        )

        val output = runCli(app, listOf("--help"))
        assertEquals(0, output.exitCode)
        assertTrue(output.stdout.contains("DexClub CLI"))
        assertTrue(output.stdout.contains("Version:"))
        assertTrue(output.stdout.contains("Dex Analysis Commands:"))
    }

    @Test
    fun versionFlagPrintsBuildVersion() {
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { createTempDirectory("dexclub-cli-version").toString() },
        )

        val output = runCli(app, listOf("--version"))
        assertEquals(0, output.exitCode)
        assertEquals(CliBuildInfo.version, output.stdout.trim())
    }

    @Test
    fun commandHelpFlagPrintsCommandHelp() {
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { createTempDirectory("dexclub-cli-command-help").toString() },
        )

        val output = runCli(app, listOf("find-method", "--help"))
        assertEquals(0, output.exitCode)
        assertTrue(output.stdout.contains("Command:"))
        assertTrue(output.stdout.contains("find-method"))
        assertTrue(output.stdout.contains("Usage:"))
        assertTrue(output.stdout.contains(CliUsages.findMethod))
    }

    @Test
    fun helpCommandPrintsCommandHelp() {
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { createTempDirectory("dexclub-cli-help-command").toString() },
        )

        val output = runCli(app, listOf("help", "manifest"))
        assertEquals(0, output.exitCode)
        assertTrue(output.stdout.contains("Command:"))
        assertTrue(output.stdout.contains("manifest"))
        assertTrue(output.stdout.contains(CliUsages.manifest))
    }

    @Test
    fun unknownCommandHintsHelp() {
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { createTempDirectory("dexclub-cli-unknown-command").toString() },
        )

        val output = runCli(app, listOf("wat"))
        assertEquals(1, output.exitCode)
        assertTrue(output.stderr.contains("Error: unknown command: wat"))
        assertTrue(output.stderr.contains("Run 'cli help' to see available commands."))
    }
}
