package io.github.dexclub.core.impl.resource

import io.github.dexclub.core.api.resource.DecodeXmlRequest
import io.github.dexclub.core.api.resource.ResourceDecodeError
import io.github.dexclub.core.api.resource.ResourceDecodeErrorReason
import io.github.dexclub.core.api.shared.CapabilityError
import io.github.dexclub.core.api.shared.Operation
import io.github.dexclub.core.api.shared.createDefaultServices
import io.github.dexclub.core.api.workspace.WorkspaceRef
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResourceXmlServiceTest {
    @Test
    fun decodeXmlFromPlainResFile() {
        val workdir = createTempDirectory("dexclub-resource-xml-text")
        val xmlFile = workdir.resolve("activity_main.xml")
        val xmlText = """
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:orientation="vertical" />
        """.trimIndent()
        xmlFile.writeText(xmlText)

        val services = createDefaultServices()
        services.workspace.initialize(xmlFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val result = services.resource.decodeXml(
            workspace,
            DecodeXmlRequest(path = "activity_main.xml"),
        )

        assertEquals("activity_main.xml", result.sourcePath)
        assertEquals(null, result.sourceEntry)
        assertEquals(xmlText, result.text)
    }

    @Test
    fun decodeXmlFromApkWorkspace() {
        val workdir = createTempDirectory("dexclub-resource-xml-apk")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.xmlapk"><application android:label="@string/app_name" /></manifest>""",
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

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val result = services.resource.decodeXml(
            workspace,
            DecodeXmlRequest(path = "res/layout/activity_main.xml"),
        )

        assertEquals("app.apk", result.sourcePath)
        assertEquals("res/layout/activity_main.xml", result.sourceEntry)
        assertTrue(result.text.contains("<LinearLayout"))
        assertTrue(result.text.contains("TextView"))
    }

    @Test
    fun decodeXmlCreatesAndRebuildsCache() {
        val workdir = createTempDirectory("dexclub-resource-xml-cache")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.xmlcache"><application android:label="@string/app_name" /></manifest>""",
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
        val targetId = workspace.activeTargetId
        val xmlId = resourceXmlCacheId("app.apk", "res/layout/activity_main.xml")
        val cacheFile = workdir.resolve(".dexclub/targets/$targetId/cache/decoded/xml/$xmlId.json").toFile()
        assertTrue(!cacheFile.exists())

        services.resource.decodeXml(workspace, DecodeXmlRequest(path = "res/layout/activity_main.xml"))

        assertTrue(cacheFile.isFile)
        cacheFile.writeText(
            """
                {
                  "schemaVersion": 1,
                  "generatedAt": "2026-04-25T12:22:00Z",
                  "targetId": "$targetId",
                  "sourcePath": "app.apk",
                  "sourceEntry": "res/layout/activity_main.xml",
                  "sourceFingerprint": "stale-fingerprint",
                  "format": "xml-text",
                  "text": "<Broken />"
                }
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val rebuilt = services.resource.decodeXml(workspace, DecodeXmlRequest(path = "res/layout/activity_main.xml"))
        assertTrue(rebuilt.text.contains("<LinearLayout"))
        val refreshed = Json.parseToJsonElement(cacheFile.readText()).jsonObject
        assertTrue(refreshed.getValue("sourceFingerprint").jsonPrimitive.content != "stale-fingerprint")
        assertTrue(refreshed.getValue("text").jsonPrimitive.content.contains("<LinearLayout"))
    }

    @Test
    fun decodeXmlRequiresXmlCapability() {
        val workdir = createTempDirectory("dexclub-resource-no-xml")
        workdir.resolve("classes.dex").writeText("")

        val services = createDefaultServices()
        services.workspace.initialize(workdir.resolve("classes.dex").toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val error = assertFailsWith<CapabilityError> {
            services.resource.decodeXml(workspace, DecodeXmlRequest(path = "res/layout/activity_main.xml"))
        }

        assertEquals(Operation.XmlDecode, error.operation)
        assertTrue(error.requiredCapability == "xmlDecode")
    }

    @Test
    fun decodeXmlFailsWhenPathIsMissing() {
        val workdir = createTempDirectory("dexclub-resource-xml-missing")
        val xmlFile = workdir.resolve("activity_main.xml")
        xmlFile.writeText("<LinearLayout />")

        val services = createDefaultServices()
        services.workspace.initialize(xmlFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val error = assertFailsWith<ResourceDecodeError> {
            services.resource.decodeXml(workspace, DecodeXmlRequest(path = "missing.xml"))
        }

        assertEquals(ResourceDecodeErrorReason.XmlPathNotFound, error.reason)
    }
}
