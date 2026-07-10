package io.github.dexclub.cli

import io.github.dexclub.core.api.shared.createDefaultServices
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CliResourceCommandTest {
    @Test
    fun manifestReadsPlainManifestFileThroughCliPipeline() {
        val workspaceDir = createTempDirectory("dexclub-cli-manifest-file")
        val manifestFile = workspaceDir.resolve("AndroidManifest.xml")
        val manifestText = """<manifest package="fixture.file" />"""
        manifestFile.writeText(manifestText)
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", manifestFile.toString()))
        assertEquals(0, initOut.exitCode)

        val output = runCli(app, listOf("manifest"))
        assertEquals(0, output.exitCode, output.stderr)
        assertEquals(manifestText, output.stdout.trim())
    }

    @Test
    fun manifestJsonReadsApkThroughCliPipeline() {
        val workspaceDir = createTempDirectory("dexclub-cli-manifest-apk")
        val apkFile = workspaceDir.resolve("app.apk").toFile()
        compileBinaryManifestApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.apk"><application android:label="fixture" /></manifest>""",
        )
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", apkFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(app, listOf("manifest", "--json"))
        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonObject
        assertEquals("app.apk", parsed.getValue("sourcePath").jsonPrimitive.content)
        assertEquals("AndroidManifest.xml", parsed.getValue("sourceEntry").jsonPrimitive.content)
        assertTrue(parsed.getValue("text").jsonPrimitive.content.contains("""package="fixture.apk""""))
    }

    @Test
    fun manifestReturnsCapabilityErrorOnDexWorkspace() {
        val workspaceDir = createTempDirectory("dexclub-cli-manifest-unsupported")
        workspaceDir.resolve("classes.dex").writeText("")
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", workspaceDir.resolve("classes.dex").toString()))
        assertEquals(0, initOut.exitCode)

        val output = runCli(app, listOf("manifest"))
        assertEquals(2, output.exitCode)
        assertTrue(output.stderr.contains("command 'manifest' is not supported"), output.stderr)
    }

    @Test
    fun resTableReadsApkThroughCliPipeline() {
        val workspaceDir = createTempDirectory("dexclub-cli-res-table-apk")
        val apkFile = workspaceDir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.res"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                </resources>
            """.trimIndent(),
        )
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", apkFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(app, listOf("res-table", "--json"))
        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonObject
        assertEquals("app.apk", parsed.getValue("sourcePath").jsonPrimitive.content)
        assertEquals("resources.arsc", parsed.getValue("sourceEntry").jsonPrimitive.content)
        assertEquals(1, parsed.getValue("packageCount").jsonPrimitive.content.toInt())
        assertEquals(1, parsed.getValue("typeCount").jsonPrimitive.content.toInt())
        assertEquals(1, parsed.getValue("entryCount").jsonPrimitive.content.toInt())
        val entry = parsed.getValue("entries").jsonArray.single().jsonObject
        assertEquals("string", entry.getValue("type").jsonPrimitive.content)
        assertEquals("app_name", entry.getValue("name").jsonPrimitive.content)
    }

    @Test
    fun resTableReturnsCapabilityErrorOnDexWorkspace() {
        val workspaceDir = createTempDirectory("dexclub-cli-res-table-unsupported")
        workspaceDir.resolve("classes.dex").writeText("")
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", workspaceDir.resolve("classes.dex").toString()))
        assertEquals(0, initOut.exitCode)

        val output = runCli(app, listOf("res-table"))
        assertEquals(2, output.exitCode)
        assertTrue(output.stderr.contains("command 'res-table' is not supported"), output.stderr)
    }

    @Test
    fun decodeXmlReadsApkEntryThroughCliPipeline() {
        val workspaceDir = createTempDirectory("dexclub-cli-decode-xml-apk")
        val apkFile = workspaceDir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.decode"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                </resources>
            """.trimIndent(),
            layoutXml = """
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/app_name" />
                </LinearLayout>
            """.trimIndent(),
        )
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", apkFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf("decode-xml", "--path", "res/layout/activity_main.xml", "--json"),
        )
        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonObject
        assertEquals("app.apk", parsed.getValue("sourcePath").jsonPrimitive.content)
        assertEquals("res/layout/activity_main.xml", parsed.getValue("sourceEntry").jsonPrimitive.content)
        assertTrue(parsed.getValue("text").jsonPrimitive.content.contains("LinearLayout"))
    }

    @Test
    fun decodeXmlReturnsCapabilityErrorOnDexWorkspace() {
        val workspaceDir = createTempDirectory("dexclub-cli-decode-xml-unsupported")
        workspaceDir.resolve("classes.dex").writeText("")
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", workspaceDir.resolve("classes.dex").toString()))
        assertEquals(0, initOut.exitCode)

        val output = runCli(app, listOf("decode-xml", "--path", "res/layout/activity_main.xml"))
        assertEquals(2, output.exitCode)
        assertTrue(output.stderr.contains("command 'decode-xml' is not supported"), output.stderr)
    }

    @Test
    fun listResReadsApkThroughCliPipeline() {
        val workspaceDir = createTempDirectory("dexclub-cli-list-res-apk")
        val apkFile = workspaceDir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.list"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                </resources>
            """.trimIndent(),
            layoutXml = """
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent" />
            """.trimIndent(),
        )
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", apkFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(app, listOf("list-res", "--json"))
        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonArray
        val layoutEntry = parsed.first { element ->
            val obj = element.jsonObject
            obj["type"]?.jsonPrimitive?.content == "layout" &&
                obj["name"]?.jsonPrimitive?.content == "activity_main"
        }.jsonObject
        assertEquals("res/layout/activity_main.xml", layoutEntry.getValue("filePath").jsonPrimitive.content)
        assertEquals("app.apk", layoutEntry.getValue("sourcePath").jsonPrimitive.content)
        assertEquals("table-backed", layoutEntry.getValue("resolution").jsonPrimitive.content)
    }

    @Test
    fun listResReturnsCapabilityErrorOnDexWorkspace() {
        val workspaceDir = createTempDirectory("dexclub-cli-list-res-unsupported")
        workspaceDir.resolve("classes.dex").writeText("")
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", workspaceDir.resolve("classes.dex").toString()))
        assertEquals(0, initOut.exitCode)

        val output = runCli(app, listOf("list-res"))
        assertEquals(2, output.exitCode)
        assertTrue(output.stderr.contains("command 'list-res' is not supported"), output.stderr)
    }

    @Test
    fun getResValueReadsResourceValueThroughCliPipeline() {
        val workspaceDir = createTempDirectory("dexclub-cli-get-res-value")
        val apkFile = workspaceDir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.resolve"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                </resources>
            """.trimIndent(),
        )
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", apkFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf("get-res-value", "--type", "string", "--name", "app_name", "--json"),
        )
        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonObject
        assertEquals("string", parsed.getValue("type").jsonPrimitive.content)
        assertEquals("app_name", parsed.getValue("name").jsonPrimitive.content)
        assertEquals("DexClub Fixture", parsed.getValue("value").jsonPrimitive.content)
    }

    @Test
    fun getResValueReturnsCapabilityErrorOnDexWorkspace() {
        val workspaceDir = createTempDirectory("dexclub-cli-get-res-value-unsupported")
        workspaceDir.resolve("classes.dex").writeText("")
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", workspaceDir.resolve("classes.dex").toString()))
        assertEquals(0, initOut.exitCode)

        val output = runCli(app, listOf("get-res-value", "--type", "string", "--name", "app_name"))
        assertEquals(2, output.exitCode)
        assertTrue(output.stderr.contains("command 'get-res-value' is not supported"), output.stderr)
    }

    @Test
    fun getResValueParserRejectsMutuallyExclusiveSelectors() {
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { createTempDirectory("dexclub-cli-resolve-usage").toString() },
        )

        val output = runCli(
            app,
            listOf("get-res-value", "--id", "0x7f010001", "--type", "string", "--name", "app_name"),
        )

        assertEquals(1, output.exitCode)
        assertTrue(output.stderr.contains("Error: --id and --type/--name are mutually exclusive"))
        assertTrue(output.stderr.contains("Usage:"))
    }

    @Test
    fun findResValuesRunsThroughCliPipeline() {
        val workspaceDir = createTempDirectory("dexclub-cli-find-res-values")
        val apkFile = workspaceDir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.findcli"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Login</string>
                    <string name="login_title">Login Title</string>
                    <string name="welcome_message">Welcome</string>
                </resources>
            """.trimIndent(),
        )
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", apkFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "find-res-values",
                "--query-json",
                """{"type":"string","value":"login","contains":true,"ignoreCase":true}""",
                "--offset",
                "1",
                "--limit",
                "1",
                "--json",
            ),
        )
        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonArray
        assertEquals(1, parsed.size)
        val hit = parsed.single().jsonObject
        assertEquals("login_title", hit.getValue("name").jsonPrimitive.content)
        assertEquals("Login Title", hit.getValue("value").jsonPrimitive.content)
        assertEquals("app.apk", hit.getValue("sourcePath").jsonPrimitive.content)
    }

    @Test
    fun findResValuesReturnsCapabilityErrorOnDexWorkspace() {
        val workspaceDir = createTempDirectory("dexclub-cli-find-res-values-unsupported")
        workspaceDir.resolve("classes.dex").writeText("")
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", workspaceDir.resolve("classes.dex").toString()))
        assertEquals(0, initOut.exitCode)

        val output = runCli(app, listOf("find-res-values", "--query-json", """{"type":"string","value":"login"}"""))
        assertEquals(2, output.exitCode)
        assertTrue(output.stderr.contains("command 'find-res-values' is not supported"), output.stderr)
    }
}
