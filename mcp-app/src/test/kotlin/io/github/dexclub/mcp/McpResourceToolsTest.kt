package io.github.dexclub.mcp

import io.github.dexclub.core.api.resource.ManifestInspectionSection
import io.github.dexclub.core.api.resource.ResourceEntry
import io.github.dexclub.core.api.resource.ResourceEntryValueHit
import io.github.dexclub.core.api.resource.ResourceResolution
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class McpResourceToolsTest {
    @Test
    fun getResourceValueToolReturnsStructuredErrorWhenContextIsMissing() {
        val app = createTestApp()

        val result = app.getResourceValueTool(
            callToolRequest(
                "get_resource_value",
                buildJsonObject {
                    put("resource_id", "0x7f010001")
                },
            ),
        )

        assertEquals(true, result.isError)
        assertEquals(app.missingSessionOrWorkdirResult().content, result.content)
    }

    @Test
    fun decodeXmlToolReturnsStructuredErrorWhenSessionIsStale() {
        val app = createTestApp()

        val result = app.decodeXmlTool(
            callToolRequest(
                "decode_xml",
                buildJsonObject {
                    put("session_id", "dead-session")
                    put("path", "res/layout/main.xml")
                },
            ),
        )

        assertEquals(true, result.isError)
        assertEquals(app.missingSessionResult("dead-session").content, result.content)
    }

    @Test
    fun inspectManifestUsesSessionWorkspaceAndCarriesStructuredResult() {
        val workspace = fakeWorkspaceContext()
        val resourceService = FakeResourceService()
        val app = createTestApp(workspace = workspace, resourceService = resourceService)
        val session = app.openTargetSession("sample.apk")

        val manifest = app.inspectManifest(
            workspace = session.workspace,
            includes = ManifestInspectionSection.entries.toSet(),
            includeText = true,
        )

        assertEquals(workspace, resourceService.lastWorkspace)
        assertEquals(ManifestInspectionSection.entries.toSet(), resourceService.lastInspectManifestRequest?.includes)
        assertEquals(true, resourceService.lastInspectManifestRequest?.includeText)
        assertEquals("fixture.sample", manifest.packageName)
        assertEquals("fixture.sample.MainActivity", manifest.activities?.single()?.name)
        assertEquals("<manifest package=\"fixture.sample\"/>", manifest.text)
    }

    @Test
    fun parseManifestInspectionSectionsSupportsMcpNames() {
        val sections = parseManifestInspectionSections(
            listOf(
                "uses-sdk",
                "application",
                "uses-permissions",
                "defined-permissions",
                "uses-features",
                "queries",
                "activities",
                "activity-aliases",
                "services",
                "receivers",
                "providers",
            ),
        )

        assertEquals(
            setOf(
                ManifestInspectionSection.UsesSdk,
                ManifestInspectionSection.Application,
                ManifestInspectionSection.UsesPermissions,
                ManifestInspectionSection.DefinedPermissions,
                ManifestInspectionSection.UsesFeatures,
                ManifestInspectionSection.Queries,
                ManifestInspectionSection.Activities,
                ManifestInspectionSection.ActivityAliases,
                ManifestInspectionSection.Services,
                ManifestInspectionSection.Receivers,
                ManifestInspectionSection.Providers,
            ),
            sections,
        )
    }

    @Test
    fun parseManifestInspectionSectionsFallsBackToAllWhenMissing() {
        val sections = parseManifestInspectionSections(null)

        assertEquals(ManifestInspectionSection.entries.toSet(), sections)
    }

    @Test
    fun parseManifestInspectionSectionsExplainsSupportedValues() {
        val error = kotlin.test.assertFailsWith<IllegalArgumentException> {
            parseManifestInspectionSections(listOf("permissions"))
        }

        assertEquals(
            "Unsupported include section: permissions. Supported sections: uses-sdk, application, uses-permissions, defined-permissions, uses-features, queries, activities, activity-aliases, services, receivers, providers",
            error.message,
        )
    }

    @Test
    fun resolveResourceUsesSessionWorkspaceAndSupportsResourceId() {
        val workspace = fakeWorkspaceContext()
        val resourceService = FakeResourceService()
        val app = createTestApp(workspace = workspace, resourceService = resourceService)
        val session = app.openTargetSession("sample.apk")

        val resource = app.getResourceValue(
            workspace = session.workspace,
            resourceId = "0x7f010001",
        )

        assertEquals(workspace, resourceService.lastWorkspace)
        assertEquals("0x7f010001", resourceService.lastResolveResourceRequest?.resourceId)
        assertEquals("string", resource.type)
        assertEquals("fixture_name", resource.name)
        assertEquals("Fixture Name", resource.value)
    }

    @Test
    fun resolveResourceUsesSessionWorkspaceAndSupportsTypeAndName() {
        val workspace = fakeWorkspaceContext()
        val resourceService = FakeResourceService()
        val app = createTestApp(workspace = workspace, resourceService = resourceService)
        val session = app.openTargetSession("sample.apk")

        val resource = app.getResourceValue(
            workspace = session.workspace,
            type = "string",
            name = "fixture_name",
        )

        assertEquals(workspace, resourceService.lastWorkspace)
        assertEquals("string", resourceService.lastResolveResourceRequest?.type)
        assertEquals("fixture_name", resourceService.lastResolveResourceRequest?.name)
        assertEquals("string", resource.type)
        assertEquals("fixture_name", resource.name)
    }

    @Test
    fun resolveResourceCarriesStructuredPluralItems() {
        val workspace = fakeWorkspaceContext()
        val resourceService = FakeResourceService(
            resourceValue = io.github.dexclub.core.api.resource.ResourceValue(
                resourceId = "0x7f100000",
                type = "plurals",
                name = "comment_count",
                value = null,
                pluralItems = listOf(
                    io.github.dexclub.core.api.resource.ResourcePluralItem(quantity = "one", value = "%d comment"),
                    io.github.dexclub.core.api.resource.ResourcePluralItem(quantity = "other", value = "%d comments"),
                ),
            ),
        )
        val app = createTestApp(workspace = workspace, resourceService = resourceService)
        val session = app.openTargetSession("sample.apk")

        val result = app.getResourceValueTool(
            callToolRequest(
                "get_resource_value",
                buildJsonObject {
                    put("session_id", session.sessionId)
                    put("resource_id", "0x7f100000")
                },
            ),
        )

        val payload = Json.parseToJsonElement((result.content.single() as io.modelcontextprotocol.kotlin.sdk.types.TextContent).text.orEmpty()).jsonObject
        val resource = payload["resource"]!!.jsonObject
        assertEquals("plurals", resource["type"]!!.jsonPrimitive.content)
        assertEquals("comment_count", resource["name"]!!.jsonPrimitive.content)
        val pluralItems = resource["pluralItems"]!!.jsonArray
        assertEquals("one", pluralItems[0].jsonObject["quantity"]!!.jsonPrimitive.content)
        assertEquals("%d comment", pluralItems[0].jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals("other", pluralItems[1].jsonObject["quantity"]!!.jsonPrimitive.content)
        assertEquals("%d comments", pluralItems[1].jsonObject["value"]!!.jsonPrimitive.content)
    }

    @Test
    fun listResourcesUsesSessionWorkspaceAndSupportsTypeFilterAndWindow() {
        val workspace = fakeWorkspaceContext()
        val resourceService = FakeResourceService(
            resourceEntries = listOf(
                ResourceEntry(
                    resourceId = "0x7f010001",
                    type = "string",
                    name = "alpha",
                    filePath = "res/values/strings.xml",
                    sourcePath = "sample.apk",
                    sourceEntry = "res/values/strings.xml",
                    resolution = ResourceResolution.TableBacked,
                ),
                ResourceEntry(
                    resourceId = "0x7f010002",
                    type = "layout",
                    name = "main",
                    filePath = "res/layout/main.xml",
                    sourcePath = "sample.apk",
                    sourceEntry = "res/layout/main.xml",
                    resolution = ResourceResolution.TableBacked,
                ),
                ResourceEntry(
                    resourceId = "0x7f010003",
                    type = "string",
                    name = "beta",
                    filePath = "res/values/strings.xml",
                    sourcePath = "sample.apk",
                    sourceEntry = "res/values/strings.xml",
                    resolution = ResourceResolution.TableBacked,
                ),
            ),
        )
        val app = createTestApp(workspace = workspace, resourceService = resourceService)
        val session = app.openTargetSession("sample.apk")

        val result = app.listResources(
            workspace = session.workspace,
            type = "string",
            offset = 1,
            limit = 1,
        )

        assertEquals(workspace, resourceService.lastWorkspace)
        assertEquals(2, result.total)
        assertEquals(1, result.offset)
        assertEquals(1, result.limit)
        assertEquals(false, result.hasMore)
        assertEquals(1, result.items.size)
        assertEquals("beta", result.items.single().name)
    }

    @Test
    fun findResourcesUsesSessionWorkspaceAndMapsShortInputs() {
        val workspace = fakeWorkspaceContext()
        val resourceService = FakeResourceService(
            resourceValueHits = listOf(
                ResourceEntryValueHit(
                    resourceId = "0x7f010001",
                    type = "string",
                    name = "alpha",
                    value = "Needle Alpha",
                    sourcePath = "sample.apk",
                    sourceEntry = "resources.arsc",
                ),
                ResourceEntryValueHit(
                    resourceId = "0x7f010002",
                    type = "string",
                    name = "beta",
                    value = "Needle Beta",
                    sourcePath = "sample.apk",
                    sourceEntry = "resources.arsc",
                ),
            ),
        )
        val app = createTestApp(workspace = workspace, resourceService = resourceService)
        val session = app.openTargetSession("sample.apk")

        val result = app.findResourceValues(
            workspace = session.workspace,
            type = "string",
            value = "Needle",
            contains = true,
            ignoreCase = true,
            offset = 1,
            limit = 1,
        )

        assertEquals(workspace, resourceService.lastWorkspace)
        val query = Json.parseToJsonElement(resourceService.lastFindResourcesRequest!!.queryText).jsonObject
        assertEquals("string", query["type"]!!.jsonPrimitive.content)
        assertEquals("Needle", query["value"]!!.jsonPrimitive.content)
        assertEquals(true, query["contains"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(true, query["ignoreCase"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(2, result.total)
        assertEquals(1, result.offset)
        assertEquals(1, result.limit)
        assertEquals(false, result.hasMore)
        assertEquals(1, result.items.size)
        assertEquals("beta", result.items.single().name)
    }

    @Test
    fun decodeXmlUsesSessionWorkspaceAndPath() {
        val workspace = fakeWorkspaceContext()
        val resourceService = FakeResourceService()
        val app = createTestApp(workspace = workspace, resourceService = resourceService)
        val session = app.openTargetSession("sample.apk")

        val result = app.decodeXml(
            workspace = session.workspace,
            path = "res/layout/main.xml",
        )

        assertEquals(workspace, resourceService.lastWorkspace)
        assertEquals("res/layout/main.xml", resourceService.lastDecodeXmlRequest?.path)
        assertEquals("sample.apk", result.sourcePath)
        assertEquals("res/layout/main.xml", result.sourceEntry)
        assertEquals("<LinearLayout/>", result.text)
    }

    @Test
    fun listResourcesToolReturnsRequestedFieldsEvenWhenValuesAreUnavailable() {
        val workspace = fakeWorkspaceContext()
        val resourceService = FakeResourceService(
            resourceEntries = listOf(
                ResourceEntry(
                    resourceId = "0x7f010001",
                    type = "xml",
                    name = null,
                    filePath = null,
                    sourcePath = "sample.apk",
                    sourceEntry = null,
                    resolution = ResourceResolution.TableHole,
                ),
            ),
        )
        val app = createTestApp(workspace = workspace, resourceService = resourceService)
        val session = app.openTargetSession("sample.apk")

        val result = app.listResourcesTool(
            callToolRequest(
                "list_res",
                buildJsonObject {
                    put("session_id", session.sessionId)
                    put("brief", true)
                    put(
                        "fields",
                        Json.parseToJsonElement("""["filePath","name","type","sourceEntry"]""").jsonArray,
                    )
                },
            ),
        )

        val payload = Json.parseToJsonElement((result.content.single() as io.modelcontextprotocol.kotlin.sdk.types.TextContent).text.orEmpty()).jsonObject
        val item = payload["items"]!!.jsonArray.single().jsonObject
        assertEquals("xml", item["type"]!!.jsonPrimitive.content)
        assertTrue("name" in item)
        assertTrue("filePath" in item)
        assertTrue("sourceEntry" in item)
        assertEquals(JsonNull, item["name"])
        assertEquals(JsonNull, item["filePath"])
        assertEquals(JsonNull, item["sourceEntry"])
    }

    @Test
    fun listResourcesToolExposesTableHoleResolutionForEmptyResourceSlots() {
        val workspace = fakeWorkspaceContext()
        val resourceService = FakeResourceService(
            resourceEntries = listOf(
                ResourceEntry(
                    resourceId = "0x7f0d0014",
                    type = "layout",
                    name = null,
                    filePath = null,
                    sourcePath = "sample.apk",
                    sourceEntry = null,
                    resolution = ResourceResolution.TableHole,
                ),
            ),
        )
        val app = createTestApp(workspace = workspace, resourceService = resourceService)
        val session = app.openTargetSession("sample.apk")

        val result = app.listResourcesTool(
            callToolRequest(
                "list_res",
                buildJsonObject {
                    put("session_id", session.sessionId)
                    put(
                        "fields",
                        Json.parseToJsonElement("""["resourceId","type","resolution"]""").jsonArray,
                    )
                },
            ),
        )

        val payload = Json.parseToJsonElement((result.content.single() as io.modelcontextprotocol.kotlin.sdk.types.TextContent).text.orEmpty()).jsonObject
        val item = payload["items"]!!.jsonArray.single().jsonObject
        assertEquals("0x7f0d0014", item["resourceId"]!!.jsonPrimitive.content)
        assertEquals("layout", item["type"]!!.jsonPrimitive.content)
        assertEquals("table-hole", item["resolution"]!!.jsonPrimitive.content)
    }

    @Test
    fun listResourcesToolExposesTableValueResolutionForScalarTableResources() {
        val workspace = fakeWorkspaceContext()
        val resourceService = FakeResourceService(
            resourceEntries = listOf(
                ResourceEntry(
                    resourceId = "0x7f110000",
                    type = "string",
                    name = "done",
                    filePath = null,
                    sourcePath = "sample.apk",
                    sourceEntry = null,
                    resolution = ResourceResolution.TableValue,
                ),
            ),
        )
        val app = createTestApp(workspace = workspace, resourceService = resourceService)
        val session = app.openTargetSession("sample.apk")

        val result = app.listResourcesTool(
            callToolRequest(
                "list_res",
                buildJsonObject {
                    put("session_id", session.sessionId)
                    put(
                        "fields",
                        Json.parseToJsonElement("""["resourceId","type","name","resolution"]""").jsonArray,
                    )
                },
            ),
        )

        val payload = Json.parseToJsonElement((result.content.single() as io.modelcontextprotocol.kotlin.sdk.types.TextContent).text.orEmpty()).jsonObject
        val item = payload["items"]!!.jsonArray.single().jsonObject
        assertEquals("0x7f110000", item["resourceId"]!!.jsonPrimitive.content)
        assertEquals("string", item["type"]!!.jsonPrimitive.content)
        assertEquals("done", item["name"]!!.jsonPrimitive.content)
        assertEquals("table-value", item["resolution"]!!.jsonPrimitive.content)
    }
}
