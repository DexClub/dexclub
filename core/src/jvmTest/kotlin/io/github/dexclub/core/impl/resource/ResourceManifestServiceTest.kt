package io.github.dexclub.core.impl.resource

import io.github.dexclub.core.api.resource.InspectManifestRequest
import io.github.dexclub.core.api.resource.ResourceDecodeError
import io.github.dexclub.core.api.resource.ResourceDecodeErrorReason
import io.github.dexclub.core.api.shared.CapabilityError
import io.github.dexclub.core.api.shared.Operation
import io.github.dexclub.core.api.shared.createDefaultServices
import io.github.dexclub.core.api.workspace.WorkspaceRef
import java.util.zip.ZipFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResourceManifestServiceTest {
    @Test
    fun decodeManifestFromPlainManifestFile() {
        val workdir = createTempDirectory("dexclub-resource-manifest-file")
        val manifestFile = workdir.resolve("AndroidManifest.xml")
        val manifestText = """<manifest package="fixture.file" />"""
        manifestFile.writeText(manifestText)

        val services = createDefaultServices()
        services.workspace.initialize(manifestFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val result = services.resource.decodeManifest(workspace)

        assertEquals("AndroidManifest.xml", result.sourcePath)
        assertEquals(null, result.sourceEntry)
        assertEquals(manifestText, result.text)
    }

    @Test
    fun decodeManifestFromBinaryManifestFile() {
        val workdir = createTempDirectory("dexclub-resource-binary-manifest-file")
        val apkFile = workdir.resolve("fixture.apk").toFile()
        compileBinaryManifestApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.binary"><application android:label="fixture" /></manifest>""",
        )
        val manifestFile = workdir.resolve("AndroidManifest.xml")
        ZipFile(apkFile).use { zip ->
            val entry = zip.getEntry("AndroidManifest.xml") ?: error("APK 中缺少 AndroidManifest.xml")
            manifestFile.writeBytes(zip.getInputStream(entry).use { it.readBytes() })
        }

        val services = createDefaultServices()
        services.workspace.initialize(manifestFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val result = services.resource.decodeManifest(workspace)

        assertEquals("AndroidManifest.xml", result.sourcePath)
        assertEquals(null, result.sourceEntry)
        assertTrue(result.text.contains("""package="fixture.binary""""))
    }

    @Test
    fun decodeManifestFromApkWorkspace() {
        val workdir = createTempDirectory("dexclub-resource-apk")
        val apkFile = workdir.resolve("app.apk")
        val manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.apk"><application android:label="fixture" /></manifest>"""
        compileBinaryManifestApk(apkFile.toFile(), manifestText)

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val result = services.resource.decodeManifest(workspace)

        assertEquals("app.apk", result.sourcePath)
        assertEquals("AndroidManifest.xml", result.sourceEntry)
        assertTrue(result.text.contains("""package="fixture.apk""""))
        assertTrue(result.text.contains("<application"))
    }

    @Test
    fun decodeManifestCreatesAndRebuildsCache() {
        val workdir = createTempDirectory("dexclub-resource-manifest-cache")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileBinaryManifestApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.manifestcache"><application android:label="fixture" /></manifest>""",
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))
        val cacheFile = workdir.resolve(".dexclub/targets/${workspace.activeTargetId}/cache/decoded/manifest.json").toFile()
        assertTrue(!cacheFile.exists())

        services.resource.decodeManifest(workspace)

        assertTrue(cacheFile.isFile)
        val first = Json.parseToJsonElement(cacheFile.readText()).jsonObject
        assertEquals("app.apk", first.getValue("sourcePath").jsonPrimitive.content)
        assertEquals("AndroidManifest.xml", first.getValue("sourceEntry").jsonPrimitive.content)
        assertTrue(first.getValue("toolVersion").jsonPrimitive.content.isNotBlank())

        cacheFile.writeText(
            """
                {
                  "schemaVersion": 1,
                  "generatedAt": "2026-04-25T12:20:00Z",
                  "targetId": "${workspace.activeTargetId}",
                  "toolVersion": "stale-tool",
                  "sourcePath": "app.apk",
                  "sourceEntry": "AndroidManifest.xml",
                  "sourceFingerprint": "stale-fingerprint",
                  "format": "xml-text",
                  "text": "<manifest package=\"stale\" />"
                }
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val rebuilt = services.resource.decodeManifest(workspace)
        assertTrue(rebuilt.text.contains("""package="fixture.manifestcache""""))
        val refreshed = Json.parseToJsonElement(cacheFile.readText()).jsonObject
        assertTrue(refreshed.getValue("toolVersion").jsonPrimitive.content != "stale-tool")
        assertTrue(refreshed.getValue("sourceFingerprint").jsonPrimitive.content != "stale-fingerprint")
    }

    @Test
    fun inspectManifestReturnsStructuredHighValueFields() {
        val workdir = createTempDirectory("dexclub-resource-manifest-inspect")
        val manifestFile = workdir.resolve("AndroidManifest.xml")
        manifestFile.writeText(
            """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="fixture.inspect"
                    android:versionCode="42"
                    android:versionName="1.2.3">
                    <uses-sdk android:minSdkVersion="24" android:targetSdkVersion="35" />
                    <uses-permission android:name="android.permission.INTERNET" />
                    <permission android:name="fixture.inspect.permission.SYNC" />
                    <application
                        android:name=".FixtureApp"
                        android:label="@string/app_name"
                        android:debuggable="true">
                        <meta-data android:name="feature_toggle" android:value="on" />
                        <activity
                            android:name=".MainActivity"
                            android:exported="true">
                            <intent-filter>
                                <action android:name="android.intent.action.MAIN" />
                                <category android:name="android.intent.category.LAUNCHER" />
                                <data android:scheme="fixture" android:host="home" />
                            </intent-filter>
                        </activity>
                        <service android:name="fixture.inspect.SyncService" android:enabled="false" />
                        <receiver android:name="ReceiverEntry" />
                        <provider
                            android:name=".FixtureProvider"
                            android:authorities="fixture.inspect.provider" />
                    </application>
                    <queries>
                        <package android:name="com.example.market" />
                        <provider android:authorities="fixture.remote.provider" />
                        <intent>
                            <action android:name="android.intent.action.VIEW" />
                            <data android:scheme="https" android:host="example.com" />
                        </intent>
                    </queries>
                </manifest>
            """.trimIndent(),
        )

        val services = createDefaultServices()
        services.workspace.initialize(manifestFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val result = services.resource.inspectManifest(
            workspace,
            InspectManifestRequest(includeText = true),
        )

        assertEquals("fixture.inspect", result.packageName)
        assertEquals("42", result.versionCode)
        assertEquals("1.2.3", result.versionName)
        assertEquals("24", result.usesSdk?.minSdkVersion)
        assertEquals("35", result.usesSdk?.targetSdkVersion)
        assertEquals("fixture.inspect.FixtureApp", result.application?.name)
        assertEquals("feature_toggle", result.application?.metaData?.single()?.name)
        assertEquals(listOf("android.permission.INTERNET"), result.usesPermissions)
        assertEquals(listOf("fixture.inspect.permission.SYNC"), result.definedPermissions)
        assertEquals("fixture.inspect.MainActivity", result.activities?.single()?.name)
        assertEquals(true, result.activities?.single()?.exported)
        assertEquals("android.intent.action.MAIN", result.activities?.single()?.intentFilters?.single()?.actions?.single())
        assertEquals("fixture", result.activities?.single()?.intentFilters?.single()?.data?.single()?.scheme)
        assertEquals("fixture.inspect.SyncService", result.services?.single()?.name)
        assertEquals(false, result.services?.single()?.enabled)
        assertEquals("fixture.inspect.ReceiverEntry", result.receivers?.single()?.name)
        assertEquals("fixture.inspect.FixtureProvider", result.providers?.single()?.name)
        assertEquals("com.example.market", result.queriesPackages?.single())
        assertEquals("fixture.remote.provider", result.queriesProviders?.single())
        assertEquals("android.intent.action.VIEW", result.queriesIntents?.single()?.actions?.single())
        assertEquals("https", result.queriesIntents?.single()?.data?.single()?.scheme)
        assertTrue(result.text?.contains("""package="fixture.inspect"""") == true)
    }

    @Test
    fun inspectManifestCanLimitReturnedSections() {
        val workdir = createTempDirectory("dexclub-resource-manifest-inspect-sections")
        val manifestFile = workdir.resolve("AndroidManifest.xml")
        manifestFile.writeText(
            """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="fixture.sections">
                    <application android:name=".FixtureApp">
                        <activity android:name=".MainActivity" />
                        <activity-alias android:name=".AliasActivity" android:targetActivity=".MainActivity" />
                        <service android:name=".SyncService" />
                    </application>
                </manifest>
            """.trimIndent(),
        )

        val services = createDefaultServices()
        services.workspace.initialize(manifestFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val result = services.resource.inspectManifest(
            workspace,
            InspectManifestRequest(
                includes = setOf(
                    io.github.dexclub.core.api.resource.ManifestInspectionSection.Activities,
                    io.github.dexclub.core.api.resource.ManifestInspectionSection.ActivityAliases,
                ),
            ),
        )

        assertEquals("fixture.sections", result.packageName)
        assertEquals("fixture.sections.MainActivity", result.activities?.single()?.name)
        assertEquals("fixture.sections.AliasActivity", result.activityAliases?.single()?.name)
        assertEquals("fixture.sections.MainActivity", result.activityAliases?.single()?.targetActivity)
        assertEquals(null, result.application)
        assertEquals(null, result.services)
        assertEquals(null, result.text)
    }

    @Test
    fun decodeManifestRequiresManifestCapability() {
        val workdir = createTempDirectory("dexclub-resource-no-manifest")
        workdir.resolve("classes.dex").writeText("")

        val services = createDefaultServices()
        services.workspace.initialize(workdir.resolve("classes.dex").toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val error = assertFailsWith<CapabilityError> {
            services.resource.decodeManifest(workspace)
        }

        assertEquals(Operation.ManifestDecode, error.operation)
        assertTrue(error.requiredCapability == "manifestDecode")
    }

    @Test
    fun decodeManifestFailsWhenApkHasNoManifestEntry() {
        val workdir = createTempDirectory("dexclub-resource-manifest-missing")
        val apkFile = workdir.resolve("broken.apk")
        apkFile.writeBytes(createZip("classes.dex" to byteArrayOf(0x64, 0x65, 0x78)))

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val error = assertFailsWith<ResourceDecodeError> {
            services.resource.decodeManifest(workspace)
        }

        assertEquals(ResourceDecodeErrorReason.ManifestEntryMissing, error.reason)
    }
}
