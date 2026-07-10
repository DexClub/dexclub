package io.github.dexclub.core.impl.resource

import io.github.dexclub.core.api.resource.ResourceDecodeError
import io.github.dexclub.core.api.resource.ResourceDecodeErrorReason
import io.github.dexclub.core.api.resource.ResourceResolution
import io.github.dexclub.core.api.shared.CapabilityError
import io.github.dexclub.core.api.shared.Operation
import io.github.dexclub.core.api.shared.createDefaultServices
import io.github.dexclub.core.api.workspace.WorkspaceRef
import java.util.zip.ZipFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResourceTableServiceTest {
    @Test
    fun dumpResourceTableFromStandaloneArscFile() {
        val workdir = createTempDirectory("dexclub-resource-arsc-file")
        val apkFile = workdir.resolve("fixture.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.arsc"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                </resources>
            """.trimIndent(),
        )
        val arscFile = workdir.resolve("resources.arsc")
        ZipFile(apkFile).use { zip ->
            val entry = zip.getEntry("resources.arsc") ?: error("APK 中缺少 resources.arsc")
            arscFile.writeBytes(zip.getInputStream(entry).use { it.readBytes() })
        }

        val services = createDefaultServices()
        services.workspace.initialize(arscFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val result = services.resource.dumpResourceTable(workspace)

        assertEquals("resources.arsc", result.sourcePath)
        assertEquals(null, result.sourceEntry)
        assertEquals(1, result.packageCount)
        assertEquals(1, result.typeCount)
        assertEquals(1, result.entryCount)
        val entry = result.entries.single()
        assertEquals("string", entry.type)
        assertEquals("app_name", entry.name)
        assertEquals(ResourceResolution.Unresolved, entry.resolution)
        assertTrue(entry.resourceId?.startsWith("0x") == true)
    }

    @Test
    fun dumpResourceTableFromApkWorkspace() {
        val workdir = createTempDirectory("dexclub-resource-arsc-apk")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.apkarsc"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                </resources>
            """.trimIndent(),
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val result = services.resource.dumpResourceTable(workspace)

        assertEquals("app.apk", result.sourcePath)
        assertEquals("resources.arsc", result.sourceEntry)
        assertEquals(1, result.packageCount)
        assertEquals(1, result.typeCount)
        assertEquals(1, result.entryCount)
        assertEquals("app_name", result.entries.single().name)
    }

    @Test
    fun dumpResourceTableCreatesAndRebuildsCache() {
        val workdir = createTempDirectory("dexclub-resource-arsc-cache")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.tablecache"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                </resources>
            """.trimIndent(),
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))
        val targetId = workspace.activeTargetId
        val cacheFile = workdir.resolve(".dexclub/targets/$targetId/cache/decoded/resource-table.json").toFile()
        assertTrue(!cacheFile.exists())

        services.resource.dumpResourceTable(workspace)

        assertTrue(cacheFile.isFile)
        val first = Json.parseToJsonElement(cacheFile.readText()).jsonObject
        assertEquals("app.apk", first.getValue("sourcePath").jsonPrimitive.content)
        assertEquals("resources.arsc", first.getValue("sourceEntry").jsonPrimitive.content)

        cacheFile.writeText(
            """
                {
                  "schemaVersion": 1,
                  "generatedAt": "2026-04-25T12:21:00Z",
                  "targetId": "$targetId",
                  "toolVersion": "test",
                  "sourcePath": "app.apk",
                  "sourceEntry": "resources.arsc",
                  "sourceFingerprint": "stale-fingerprint",
                  "format": "resource-table-v1",
                  "payload": {
                    "packages": [],
                    "typeCount": 0,
                    "entries": []
                  }
                }
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val rebuilt = services.resource.dumpResourceTable(workspace)
        assertEquals(1, rebuilt.entryCount)
        val refreshed = Json.parseToJsonElement(cacheFile.readText()).jsonObject
        assertTrue(refreshed.getValue("sourceFingerprint").jsonPrimitive.content != "stale-fingerprint")
        assertEquals(1, refreshed.getValue("payload").jsonObject.getValue("entries").jsonArray.size)
    }

    @Test
    fun dumpResourceTableRequiresResourceTableCapability() {
        val workdir = createTempDirectory("dexclub-resource-no-table")
        workdir.resolve("classes.dex").writeText("")

        val services = createDefaultServices()
        services.workspace.initialize(workdir.resolve("classes.dex").toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val error = assertFailsWith<CapabilityError> {
            services.resource.dumpResourceTable(workspace)
        }

        assertEquals(Operation.ResourceTableDecode, error.operation)
        assertTrue(error.requiredCapability == "resourceTableDecode")
    }

    @Test
    fun dumpResourceTableFailsWhenApkHasNoResourceTable() {
        val workdir = createTempDirectory("dexclub-resource-arsc-missing")
        val apkFile = workdir.resolve("broken.apk")
        apkFile.writeBytes(createZip("AndroidManifest.xml" to """<manifest package="broken" />""".toByteArray()))

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val error = assertFailsWith<ResourceDecodeError> {
            services.resource.dumpResourceTable(workspace)
        }

        assertEquals(ResourceDecodeErrorReason.ResourceTableEntryMissing, error.reason)
    }
}
