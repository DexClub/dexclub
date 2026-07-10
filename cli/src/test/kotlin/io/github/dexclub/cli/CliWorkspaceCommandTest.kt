package io.github.dexclub.cli

import io.github.dexclub.core.api.shared.createDefaultServices
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CliWorkspaceCommandTest {
    @Test
    fun initStatusGcAndInspectCommandsRunThroughCliPipeline() {
        val workspaceDir = createTempDirectory("dexclub-cli")
        val dexFile = workspaceDir.resolve("1.dex")
        dexFile.writeText("")
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", dexFile.toString()))
        assertEquals(0, initOut.exitCode)
        assertTrue(initOut.stdout.contains("state=healthy"))
        assertTrue(workspaceDir.resolve(".dexclub/workspace.json").exists())

        val statusOut = runCli(app, listOf("status"))
        assertEquals(0, statusOut.exitCode)
        assertTrue(statusOut.stdout.contains("workspaceId="))
        assertTrue(statusOut.stdout.contains("kind=dex"))

        val targetId = workspaceDir.resolve(".dexclub/targets").toFile().listFiles()!!.single().name
        val cacheFile = workspaceDir.resolve(".dexclub/targets/$targetId/cache/decoded/manifest.json")
        cacheFile.parent.createDirectories()
        cacheFile.writeText("cached")
        val gcOut = runCli(app, listOf("gc"))
        assertEquals(0, gcOut.exitCode)
        assertTrue(gcOut.stdout.contains("deletedFiles=1"))
        assertTrue(!cacheFile.exists())

        val inspectOut = runCli(app, listOf("inspect"))
        assertEquals(0, inspectOut.exitCode)
        assertTrue(inspectOut.stdout.contains("dexCount=1"))
        assertTrue(inspectOut.stdout.contains("capabilities=inspect,findClass"))
        assertTrue(!inspectOut.stdout.contains("classCount="))
    }

    @Test
    fun switchCommandReactivatesPreviouslyInitializedTarget() {
        val workspaceDir = createTempDirectory("dexclub-cli-switch")
        val aDex = workspaceDir.resolve("a.dex")
        val bDex = workspaceDir.resolve("b.dex")
        aDex.writeText("")
        bDex.writeText("")
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initA = runCli(app, listOf("init", aDex.toString()))
        assertEquals(0, initA.exitCode)

        val initB = runCli(app, listOf("init", bDex.toString()))
        assertEquals(0, initB.exitCode)
        assertTrue(initB.stdout.contains("inputPath=b.dex"))

        val switched = runCli(app, listOf("switch", aDex.toString()))
        assertEquals(0, switched.exitCode, switched.stderr)
        assertTrue(switched.stdout.contains("inputPath=a.dex"))

        val status = runCli(app, listOf("status"))
        assertEquals(0, status.exitCode)
        assertTrue(status.stdout.contains("inputPath=a.dex"))
    }

    @Test
    fun targetsCommandListsInitializedTargetsAndMarksActiveOne() {
        val workspaceDir = createTempDirectory("dexclub-cli-targets")
        val aDex = workspaceDir.resolve("a.dex")
        val bDex = workspaceDir.resolve("b.dex")
        aDex.writeText("")
        bDex.writeText("")
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        assertEquals(0, runCli(app, listOf("init", aDex.toString())).exitCode)
        assertEquals(0, runCli(app, listOf("init", bDex.toString())).exitCode)

        val textOut = runCli(app, listOf("targets"))
        assertEquals(0, textOut.exitCode, textOut.stderr)
        assertTrue(textOut.stdout.contains("active\ttargetId\tinputType\tinputPath\tcreatedAt\tupdatedAt"))
        assertTrue(textOut.stdout.contains("file\ta.dex"))
        assertTrue(textOut.stdout.contains("*\t"))
        assertTrue(textOut.stdout.contains("file\tb.dex"))

        val jsonOut = runCli(app, listOf("targets", "--json"))
        assertEquals(0, jsonOut.exitCode, jsonOut.stderr)
        val parsed = Json.parseToJsonElement(jsonOut.stdout).jsonArray
        assertEquals(2, parsed.size)
        assertEquals(listOf("a.dex", "b.dex"), parsed.map { it.jsonObject.getValue("inputPath").jsonPrimitive.content })
        assertEquals(listOf(false, true), parsed.map { it.jsonObject.getValue("active").jsonPrimitive.content.toBoolean() })
    }

    @Test
    fun switchCommandCanReactivateMissingTargetInputWithinCurrentWorkspace() {
        val workspaceDir = createTempDirectory("dexclub-cli-switch-missing")
        val aDex = workspaceDir.resolve("a.dex")
        val bDex = workspaceDir.resolve("b.dex")
        aDex.writeText("")
        bDex.writeText("")
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        assertEquals(0, runCli(app, listOf("init", aDex.toString())).exitCode)
        assertEquals(0, runCli(app, listOf("init", bDex.toString())).exitCode)
        aDex.deleteExisting()

        val switched = runCli(app, listOf("switch", "a.dex"))
        assertEquals(2, switched.exitCode, switched.stderr)
        assertTrue(switched.stdout.contains("inputPath=a.dex"))
        assertTrue(switched.stdout.contains("state=broken"))
    }

    @Test
    fun statusUsesBrokenExitCodeWhenInputIsMissing() {
        val workspaceDir = createTempDirectory("dexclub-cli-broken")
        val apkFile = workspaceDir.resolve("app.apk")
        apkFile.writeText("")
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        runCli(app, listOf("init", apkFile.toString()))
        apkFile.deleteExisting()

        val statusOut = runCli(app, listOf("status"))
        assertEquals(2, statusOut.exitCode)
        assertTrue(statusOut.stdout.contains("state=broken"))
        assertTrue(statusOut.stdout.contains("issueCount="))
    }
}
