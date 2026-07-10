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
            ?: error("缺少资源 id")

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
