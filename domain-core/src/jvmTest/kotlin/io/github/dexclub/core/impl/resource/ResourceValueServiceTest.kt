package io.github.dexclub.core.impl.resource

import io.github.dexclub.core.api.resource.FindResourcesRequest
import io.github.dexclub.core.api.resource.ResolveResourceRequest
import io.github.dexclub.core.api.resource.ResourceBagKind
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
    fun resourceValueQueryRejectsLegacyTypeField() {
        val error = assertFailsWith<io.github.dexclub.core.api.resource.ResourceDecodeError> {
            ResourceSearchQueryParser().parse("""{"type":"string","value":"login"}""")
        }

        assertEquals("Resource query field 'resourceType' must be a non-empty string", error.message)
    }

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
        assertEquals("DexClub Fixture", result.singleDecodedValue())
    }

    @Test
    fun resolveResourceValueByUnsignedHighBitId() {
        val workdir = createTempDirectory("dexclub-resource-resolve-high-id")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.resolvehighid"><application android:label="@string/app_name" /></manifest>""",
            resourceXml = """
                <resources>
                    <string name="app_name">High ID Fixture</string>
                </resources>
            """.trimIndent(),
            packageId = "0x80",
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))
        val resourceId = services.resource.dumpResourceTable(workspace).entries
            .first { it.type == "string" && it.name == "app_name" }
            .resourceId
            ?: error("Missing resource ID")

        assertTrue(resourceId.startsWith("0x80"))
        val result = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(resourceId = resourceId),
        )

        assertEquals(resourceId, result.resourceId)
        assertEquals("High ID Fixture", result.singleDecodedValue())
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
        assertEquals("DexClub Fixture", result.singleDecodedValue())
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

        assertEquals("true", boolValue.singleDecodedValue())
        assertEquals("3", integerValue.singleDecodedValue())
        assertTrue(dimenValue.singleDecodedValue().orEmpty().contains("8"))
        assertTrue(dimenValue.singleDecodedValue() != "true")
    }

    @Test
    fun resolveResourceValuePreservesVariantsTypedValuesAndBags() {
        val workdir = createTempDirectory("dexclub-resource-complete-values")
        val apkFile = workdir.resolve("app.apk").toFile()
        compileResourceApk(
            outputApk = apkFile,
            manifestText = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.complete"><application android:theme="@style/AppTheme" /></manifest>""",
            resourceFiles = mapOf(
                "values/resources.xml" to """
                    <resources>
                        <string name="app_name">Default Name</string>
                        <string name="app_alias">@string/app_name</string>
                        <attr name="customMode" format="enum">
                            <enum name="first" value="1" />
                            <enum name="second" value="2" />
                        </attr>
                        <style name="AppTheme" parent="@android:style/Theme.Material.Light">
                            <item name="android:windowLightStatusBar">true</item>
                            <item name="customMode">second</item>
                        </style>
                        <string-array name="labels"><item>One</item><item>@string/app_name</item></string-array>
                        <plurals name="count"><item quantity="one">%d item</item><item quantity="other">%d items</item></plurals>
                    </resources>
                """.trimIndent(),
                "values-night/resources.xml" to """<resources><string name="app_name">Night Name</string></resources>""",
                "values-fr/resources.xml" to """<resources><string name="app_name">Nom francais</string></resources>""",
            ),
        )

        val services = createDefaultServices()
        services.workspace.initialize(apkFile.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.toString()))
        services.resource.dumpResourceTable(workspace)

        val appName = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(type = "string", name = "app_name"),
        )
        assertEquals("fixture.complete", appName.packageName)
        assertEquals(listOf("", "-fr", "-night-v8"), appName.variants.map { it.configuration.qualifiers })
        assertTrue(appName.variants.first().configuration.isDefault)
        assertEquals("Default Name", appName.variants.first().value?.decodedValue)
        val french = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(
                type = "string",
                name = "app_name",
                qualifier = "-fr",
                includeAllVariants = false,
            ),
        )
        assertEquals("Nom francais", french.singleDecodedValue())
        assertEquals(listOf("-fr"), french.variants.map { it.configuration.qualifiers })

        val alias = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(type = "string", name = "app_alias"),
        ).variants.single().value ?: error("Missing alias typed value")
        assertEquals("REFERENCE", alias.valueType)
        assertEquals("@string/app_name", alias.decodedValue)
        assertEquals(alias.rawDataHex, alias.referencedResourceId)

        val style = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(type = "style", name = "AppTheme"),
        )
        assertTrue(style.variants.all { it.bag?.kind == ResourceBagKind.Style })
        assertTrue(style.variants.all { it.bag?.parentResourceId == "0x01030237" })
        assertTrue(style.variants.flatMap { it.bag?.items.orEmpty() }.any { it.keyName == "customMode" })

        val array = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(type = "array", name = "labels"),
        ).variants.single().bag ?: error("Missing array bag")
        assertEquals(ResourceBagKind.Array, array.kind)
        assertEquals(listOf(0, 1), array.items.map { it.index })
        assertEquals("REFERENCE", array.items.last().value.valueType)

        val attribute = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(type = "attr", name = "customMode"),
        ).variants.single().bag ?: error("Missing attribute bag")
        assertEquals(ResourceBagKind.Attribute, attribute.kind)
        assertTrue(attribute.items.any { "ENUM" in it.attributeFormats.orEmpty() })

        val plurals = services.resource.getResourceValue(
            workspace,
            ResolveResourceRequest(type = "plurals", name = "count"),
        ).variants.single().bag ?: error("Missing plurals bag")
        assertEquals(ResourceBagKind.Plurals, plurals.kind)
        assertEquals(listOf("one", "other"), plurals.items.map { it.quantity })

        val frenchHits = services.resource.findResourceValues(
            workspace,
            FindResourcesRequest(
                queryText = """{"resourceType":"string","value":"Nom francais","qualifier":"-fr"}""",
            ),
        )
        assertEquals(listOf("-fr"), frenchHits.map { it.qualifier })
        assertEquals("decoded_value", frenchHits.single().matchTarget)

        val referenceHits = services.resource.findResourceValues(
            workspace,
            FindResourcesRequest(
                queryText = """{"resourceType":"string","value":"${alias.referencedResourceId}","matchTarget":"reference"}""",
            ),
        )
        assertEquals("app_alias", referenceHits.single().name)
        assertEquals("REFERENCE", referenceHits.single().valueKind)

        val bagKeyHits = services.resource.findResourceValues(
            workspace,
            FindResourcesRequest(
                queryText = """{"resourceType":"style","value":"customMode","matchTarget":"bag_key"}""",
            ),
        )
        assertTrue(bagKeyHits.isNotEmpty())
        assertTrue(bagKeyHits.all { it.bagKey == "customMode" && it.matchTarget == "bag_key" })

        val mismatchedBagKindHits = services.resource.findResourceValues(
            workspace,
            FindResourcesRequest(
                queryText = """{"resourceType":"plurals","value":"one","valueKind":"Plurals","matchTarget":"bag_key"}""",
            ),
        )
        assertTrue(mismatchedBagKindHits.isEmpty())
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
        val items = result.variants.single().bag?.items ?: error("Missing plurals bag")
        assertEquals(2, items.size)
        assertEquals("one", items[0].quantity)
        assertEquals("%d comment", items[0].value.decodedValue)
        assertEquals("other", items[1].quantity)
        assertEquals("%d comments", items[1].value.decodedValue)
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

        assertEquals("%d cached comments", result.variants.single().bag?.items?.last()?.value?.decodedValue)
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

        assertEquals("Cached Override", result.singleDecodedValue())
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
                queryText = """{"resourceType":"string","value":"login","contains":true,"ignoreCase":true}""",
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
            FindResourcesRequest(queryText = """{"resourceType":"string","value":"cache only","contains":true,"ignoreCase":true}"""),
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
                FindResourcesRequest(queryText = """{"resourceType":"string","value":"login"}"""),
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
                    queryText = """{"resourceType":"string","value":"login","contains":true,"ignoreCase":true}""",
                    window = PageWindow(offset = -1),
                ),
            )
        }
        assertEquals("offset must be non-negative", negativeOffset.message)

        val invalidLimit = assertFailsWith<IllegalArgumentException> {
            services.resource.findResourceValues(
                workspace,
                FindResourcesRequest(
                    queryText = """{"resourceType":"string","value":"login","contains":true,"ignoreCase":true}""",
                    window = PageWindow(limit = 0),
                ),
            )
        }
        assertEquals("limit must be positive when specified", invalidLimit.message)
    }
}

private fun io.github.dexclub.core.api.resource.ResourceValue.singleDecodedValue(): String? =
    variants.single().value?.decodedValue
