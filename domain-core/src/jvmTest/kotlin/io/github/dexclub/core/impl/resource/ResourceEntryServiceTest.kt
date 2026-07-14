package io.github.dexclub.core.impl.resource

import io.github.dexclub.core.api.resource.ResourceResolution
import io.github.dexclub.core.api.resource.normalizedResolution
import io.github.dexclub.core.api.shared.CapabilityError
import io.github.dexclub.core.api.shared.Operation
import io.github.dexclub.core.api.shared.createDefaultServices
import io.github.dexclub.core.api.workspace.WorkspaceRef
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResourceEntryServiceTest {
    @Test
    fun listResourceEntriesFromApkWorkspace() {
        val workdir = createTempDirectory("dexclub-resource-list-apk")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.listapk"><application android:label="@string/app_name" /></manifest>""",
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

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val entries = services.resource.listResourceEntries(workspace)

        val layoutEntry = entries.first { it.type == "layout" && it.name == "activity_main" }
        assertEquals("res/layout/activity_main.xml", layoutEntry.filePath)
        assertEquals("app.apk", layoutEntry.sourcePath)
        assertEquals("res/layout/activity_main.xml", layoutEntry.sourceEntry)
        assertEquals(ResourceResolution.TableBacked, layoutEntry.resolution)

        val stringEntry = entries.first { it.type == "string" && it.name == "app_name" }
        assertEquals("app.apk", stringEntry.sourcePath)
        assertEquals(null, stringEntry.filePath)
        assertEquals(ResourceResolution.TableValue, stringEntry.resolution)
        assertTrue(stringEntry.resourceId?.startsWith("0x") == true)
    }

    @Test
    fun listResourceEntriesAreUnsupportedForStandaloneXmlWorkspace() {
        val workdir = createTempDirectory("dexclub-resource-list-xml")
        val xmlFile = workdir.resolve("activity_main.xml")
        xmlFile.writeText("<LinearLayout />")

        val services = createDefaultServices()
        services.workspace.initialize(xmlFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val error = assertFailsWith<CapabilityError> {
            services.resource.listResourceEntries(workspace)
        }
        assertEquals(Operation.ResourceEntryList, error.operation)
    }

    @Test
    fun listResourceEntriesCreatesAndRebuildsIndex() {
        val workdir = createTempDirectory("dexclub-resource-list-index")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.listindex"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                </resources>
            """.trimIndent(),
            layoutXml = """<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" />""",
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))
        val targetId = workspace.activeTargetId
        val indexFile = workdir.resolve(".dexclub/targets/$targetId/cache/indexes/resource-entry-index.json").toFile()
        assertTrue(!indexFile.exists())

        val entries = services.resource.listResourceEntries(workspace)
        assertTrue(indexFile.isFile)
        assertTrue(entries.isNotEmpty())

        indexFile.writeText(
            """
                {
                  "schemaVersion": 1,
                  "generatedAt": "2026-04-25T12:27:00Z",
                  "targetId": "$targetId",
                  "toolVersion": "test",
                  "contentFingerprint": "stale-fingerprint",
                  "format": "resource-entry-index-v1",
                  "entries": []
                }
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val rebuilt = services.resource.listResourceEntries(workspace)
        assertTrue(rebuilt.isNotEmpty())
        val refreshed = Json.parseToJsonElement(indexFile.readText()).jsonObject
        assertEquals(workspace.snapshot.contentFingerprint, refreshed.getValue("contentFingerprint").jsonPrimitive.content)
        assertTrue(refreshed.getValue("entries").jsonArray.isNotEmpty())
    }

    @Test
    fun listResourceEntriesRequiresResourceEntryCapability() {
        val workdir = createTempDirectory("dexclub-resource-list-no-entry")
        workdir.resolve("classes.dex").writeText("")

        val services = createDefaultServices()
        services.workspace.initialize(workdir.resolve("classes.dex").toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val error = assertFailsWith<CapabilityError> {
            services.resource.listResourceEntries(workspace)
        }

        assertEquals(Operation.ResourceEntryList, error.operation)
        assertTrue(error.requiredCapability == "resourceEntryList")
    }

    @Test
    fun normalizedResolutionMarksNamelessTableSlotsAsTableHole() {
        val hole = io.github.dexclub.core.api.resource.ResourceEntry(
            resourceId = "0x7f0d0014",
            type = "layout",
            name = null,
            filePath = null,
            sourcePath = "sample.apk",
            sourceEntry = null,
            resolution = ResourceResolution.Unresolved,
        ).normalizedResolution()

        val unresolved = io.github.dexclub.core.api.resource.ResourceEntry(
            resourceId = "0x7f110001",
            type = "string",
            name = "app_name",
            filePath = null,
            sourcePath = "sample.apk",
            sourceEntry = null,
            resolution = ResourceResolution.Unresolved,
        ).normalizedResolution()

        assertEquals(ResourceResolution.TableHole, hole.resolution)
        assertEquals(ResourceResolution.TableValue, unresolved.resolution)
    }

    @Test
    fun normalizeResourceEntriesPrefersResolvedEntriesOverUnresolvedShells() {
        val entries = normalizeResourceEntries(
            listOf(
                io.github.dexclub.core.api.resource.ResourceEntry(
                    resourceId = "0x7f0d0014",
                    type = "layout",
                    name = null,
                    filePath = null,
                    sourcePath = "sample.apk",
                    sourceEntry = null,
                    resolution = ResourceResolution.TableHole,
                ),
                io.github.dexclub.core.api.resource.ResourceEntry(
                    resourceId = "0x7f0d0014",
                    type = "layout",
                    name = "activity_main",
                    filePath = "res/layout/activity_main.xml",
                    sourcePath = "sample.apk",
                    sourceEntry = "res/layout/activity_main.xml",
                    resolution = ResourceResolution.TableBacked,
                ),
                io.github.dexclub.core.api.resource.ResourceEntry(
                    resourceId = "0x7f120001",
                    type = "xml",
                    name = null,
                    filePath = null,
                    sourcePath = "sample.apk",
                    sourceEntry = null,
                    resolution = ResourceResolution.TableHole,
                ),
                io.github.dexclub.core.api.resource.ResourceEntry(
                    resourceId = "0x7f120001",
                    type = "xml",
                    name = "network_security_config",
                    filePath = "res/xml/network_security_config.xml",
                    sourcePath = "sample.apk",
                    sourceEntry = "res/xml/network_security_config.xml",
                    resolution = ResourceResolution.TableBacked,
                ),
            ),
        )

        assertEquals(2, entries.size)

        val layout = entries.first { it.type == "layout" }
        assertEquals("activity_main", layout.name)
        assertEquals("res/layout/activity_main.xml", layout.filePath)
        assertEquals("res/layout/activity_main.xml", layout.sourceEntry)
        assertEquals(ResourceResolution.TableBacked, layout.resolution)

        val xml = entries.first { it.type == "xml" }
        assertEquals("network_security_config", xml.name)
        assertEquals("res/xml/network_security_config.xml", xml.filePath)
        assertEquals("res/xml/network_security_config.xml", xml.sourceEntry)
        assertEquals(ResourceResolution.TableBacked, xml.resolution)
    }
}
