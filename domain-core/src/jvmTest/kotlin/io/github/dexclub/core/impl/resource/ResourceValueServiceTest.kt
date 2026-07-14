package io.github.dexclub.core.impl.resource

import io.github.dexclub.core.api.resource.FindResourcesRequest
import io.github.dexclub.core.api.resource.ResolveResourceRequest
import io.github.dexclub.core.api.shared.CapabilityError
import io.github.dexclub.core.api.shared.Operation
import io.github.dexclub.core.api.shared.PageWindow
import io.github.dexclub.core.api.shared.createDefaultServices
import io.github.dexclub.core.api.workspace.WorkspaceRef
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResourceValueServiceTest {
    @Test
    fun resolveResourceValueById() {
        val workdir = createTempDirectory("dexclub-resource-resolve-id")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.resolveid"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                </resources>
            """.trimIndent(),
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))
        val resourceId = services.resource.dumpResourceTable(workspace).entries
            .first { it.type == "string" && it.name == "app_name" }
            .resourceId
            ?: error("Missing resource ID")

        val result = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(resourceId = resourceId),
        )

        assertEquals(resourceId, result.resourceId)
        assertEquals("string", result.type)
        assertEquals("app_name", result.name)
        assertEquals("DexClub Fixture", result.value)
    }

    @Test
    fun resolveResourceValueByTypeAndName() {
        val workdir = createTempDirectory("dexclub-resource-resolve-name")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.resolvename"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                </resources>
            """.trimIndent(),
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val result = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(
                type = "string",
                name = "app_name",
            ),
        )

        assertEquals("string", result.type)
        assertEquals("app_name", result.name)
        assertEquals("DexClub Fixture", result.value)
    }

    @Test
    fun resolveResourceValueUsesRealValueTypesForScalars() {
        val workdir = createTempDirectory("dexclub-resource-resolve-scalars")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.resolvescalars"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                    <bool name="feature_enabled">true</bool>
                    <integer name="max_items">3</integer>
                    <dimen name="spacing">8dp</dimen>
                </resources>
            """.trimIndent(),
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val boolValue = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(type = "bool", name = "feature_enabled"),
        )
        val integerValue = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(type = "integer", name = "max_items"),
        )
        val dimenValue = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(type = "dimen", name = "spacing"),
        )

        assertEquals("true", boolValue.value)
        assertEquals("3", integerValue.value)
        assertTrue(dimenValue.value.orEmpty().contains("8"))
        assertTrue(dimenValue.value != "true")
    }

    @Test
    fun resolveResourceValueExpandsPluralsAsStructuredItems() {
        val workdir = createTempDirectory("dexclub-resource-resolve-plurals")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.resolveplurals"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                    <plurals name="comment_count">
                        <item quantity="one">%d comment</item>
                        <item quantity="other">%d comments</item>
                    </plurals>
                </resources>
            """.trimIndent(),
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val result = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(type = "plurals", name = "comment_count"),
        )

        assertEquals("plurals", result.type)
        assertEquals("comment_count", result.name)
        assertEquals(null, result.value)
        assertEquals(2, result.pluralItems?.size)
        assertEquals("one", result.pluralItems?.get(0)?.quantity)
        assertEquals("%d comment", result.pluralItems?.get(0)?.value)
        assertEquals("other", result.pluralItems?.get(1)?.quantity)
        assertEquals("%d comments", result.pluralItems?.get(1)?.value)
    }

    @Test
    fun resolveResourceValueReusesCachedPluralItems() {
        val workdir = createTempDirectory("dexclub-resource-resolve-plurals-cache")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.resolvepluralscache"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                    <plurals name="comment_count">
                        <item quantity="one">%d comment</item>
                        <item quantity="other">%d comments</item>
                    </plurals>
                </resources>
            """.trimIndent(),
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))
        services.resource.dumpResourceTable(workspace)
        val cacheFile = workdir.resolve(".dexclub/targets/${workspace.activeTargetId}/cache/decoded/resource-table.json").toFile()
        cacheFile.writeText(
            cacheFile.readText(Charsets.UTF_8).replace("%d comments", "%d cached comments"),
            Charsets.UTF_8,
        )

        val result = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(type = "plurals", name = "comment_count"),
        )

        assertEquals("%d cached comments", result.pluralItems?.last()?.value)
    }

    @Test
    fun resolveResourceValueReusesCachedResourceTablePayload() {
        val workdir = createTempDirectory("dexclub-resource-resolve-cache")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.resolvecache"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Fixture</string>
                </resources>
            """.trimIndent(),
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))
        services.resource.dumpResourceTable(workspace)
        val cacheFile = workdir.resolve(".dexclub/targets/${workspace.activeTargetId}/cache/decoded/resource-table.json").toFile()
        cacheFile.writeText(
            cacheFile.readText(Charsets.UTF_8).replace("DexClub Fixture", "Cached Override"),
            Charsets.UTF_8,
        )

        val result = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(type = "string", name = "app_name"),
        )

        assertEquals("Cached Override", result.value)
    }

    @Test
    fun resolveResourceValueRequiresResourceTableCapability() {
        val workdir = createTempDirectory("dexclub-resource-resolve-no-table")
        workdir.resolve("classes.dex").writeText("")

        val services = createDefaultServices()
        services.workspace.initialize(workdir.resolve("classes.dex").toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val error = assertFailsWith<CapabilityError> {
            services.resource.getResourceValue(
                workspace,
                ResolveResourceRequest(type = "string", name = "app_name"),
            )
        }

        assertEquals(Operation.ResourceTableDecode, error.operation)
        assertTrue(error.requiredCapability == "resourceTableDecode")
    }

    @Test
    fun findResourceEntriesAppliesStableSortAndWindow() {
        val workdir = createTempDirectory("dexclub-resource-find")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.find"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Login</string>
                    <string name="login_title">Login Title</string>
                    <string name="welcome_message">Welcome</string>
                </resources>
            """.trimIndent(),
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val hits = services.resource.findResourceValues(
            workspace,
            FindResourcesRequest(
                queryText = """{"type":"string","value":"login","contains":true,"ignoreCase":true}""",
                window = PageWindow(offset = 1, limit = 1),
            ),
        )

        assertEquals(1, hits.size)
        val hit = hits.single()
        assertEquals("login_title", hit.name)
        assertEquals("Login Title", hit.value)
        assertEquals("app.apk", hit.sourcePath)
        assertEquals("resources.arsc", hit.sourceEntry)
    }

    @Test
    fun findResourceEntriesReusesCachedResourceTablePayload() {
        val workdir = createTempDirectory("dexclub-resource-find-cache")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.findcache"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Login</string>
                    <string name="login_title">Login Title</string>
                </resources>
            """.trimIndent(),
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))
        services.resource.dumpResourceTable(workspace)
        val cacheFile = workdir.resolve(".dexclub/targets/${workspace.activeTargetId}/cache/decoded/resource-table.json").toFile()
        cacheFile.writeText(
            cacheFile.readText(Charsets.UTF_8).replace("Login Title", "Cache Only Match"),
            Charsets.UTF_8,
        )

        val hits = services.resource.findResourceValues(
            workspace,
            FindResourcesRequest(queryText = """{"type":"string","value":"cache only","contains":true,"ignoreCase":true}"""),
        )

        assertEquals(1, hits.size)
        assertEquals("login_title", hits.single().name)
        assertEquals("Cache Only Match", hits.single().value)
    }

    @Test
    fun findResourceEntriesRequiresResourceTableCapability() {
        val workdir = createTempDirectory("dexclub-resource-find-no-table")
        workdir.resolve("classes.dex").writeText("")

        val services = createDefaultServices()
        services.workspace.initialize(workdir.resolve("classes.dex").toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val error = assertFailsWith<CapabilityError> {
            services.resource.findResourceValues(
                workspace,
                FindResourcesRequest(queryText = """{"type":"string","value":"login"}"""),
            )
        }

        assertEquals(Operation.ResourceTableDecode, error.operation)
        assertTrue(error.requiredCapability == "resourceTableDecode")
    }

    @Test
    fun findResourceEntriesRejectsInvalidWindowArguments() {
        val workdir = createTempDirectory("dexclub-resource-find-invalid-window")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.invalidwindow"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">DexClub Login</string>
                </resources>
            """.trimIndent(),
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))

        val negativeOffset = assertFailsWith<IllegalArgumentException> {
            services.resource.findResourceValues(
                workspace,
                FindResourcesRequest(
                    queryText = """{"type":"string","value":"login","contains":true,"ignoreCase":true}""",
                    window = PageWindow(offset = -1),
                ),
            )
        }
        assertEquals("offset must be non-negative", negativeOffset.message)

        val invalidLimit = assertFailsWith<IllegalArgumentException> {
            services.resource.findResourceValues(
                workspace,
                FindResourcesRequest(
                    queryText = """{"type":"string","value":"login","contains":true,"ignoreCase":true}""",
                    window = PageWindow(limit = 0),
                ),
            )
        }
        assertEquals("limit must be positive when specified", invalidLimit.message)
    }
}
