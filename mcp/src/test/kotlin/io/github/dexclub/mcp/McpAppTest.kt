package io.github.dexclub.mcp

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.ExportClassDexRequest
import io.github.dexclub.core.api.dex.ExportClassJavaRequest
import io.github.dexclub.core.api.dex.ExportClassSmaliRequest
import io.github.dexclub.core.api.dex.ExportMethodDexRequest
import io.github.dexclub.core.api.dex.ExportMethodJavaRequest
import io.github.dexclub.core.api.dex.ExportMethodSmaliRequest
import io.github.dexclub.core.api.dex.ExportResult
import io.github.dexclub.core.api.dex.FieldHit
import io.github.dexclub.core.api.dex.FindClassesRequest
import io.github.dexclub.core.api.dex.FindClassesUsingStringsRequest
import io.github.dexclub.core.api.dex.FindFieldsRequest
import io.github.dexclub.core.api.dex.FindMethodsRequest
import io.github.dexclub.core.api.dex.FindMethodsUsingStringsRequest
import io.github.dexclub.core.api.dex.InspectMethodRequest
import io.github.dexclub.core.api.dex.MethodDetail
import io.github.dexclub.core.api.dex.MethodDetailSection
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.shared.CacheState
import io.github.dexclub.core.api.shared.CapabilitySet
import io.github.dexclub.core.api.shared.InputType
import io.github.dexclub.core.api.shared.InventoryCounts
import io.github.dexclub.core.api.shared.MethodSmaliMode
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.shared.Services
import io.github.dexclub.core.api.shared.WorkspaceIssue
import io.github.dexclub.core.api.shared.WorkspaceKind
import io.github.dexclub.core.api.shared.WorkspaceState
import io.github.dexclub.core.api.resource.DecodeXmlRequest
import io.github.dexclub.core.api.resource.DecodedXmlResult
import io.github.dexclub.core.api.resource.FindResourcesRequest
import io.github.dexclub.core.api.resource.InspectManifestRequest
import io.github.dexclub.core.api.resource.ManifestApplicationInfo
import io.github.dexclub.core.api.resource.ManifestComponentInfo
import io.github.dexclub.core.api.resource.ManifestInspectionResult
import io.github.dexclub.core.api.resource.ManifestInspectionSection
import io.github.dexclub.core.api.resource.ManifestIntentFilter
import io.github.dexclub.core.api.resource.ManifestMetaData
import io.github.dexclub.core.api.resource.ManifestResult
import io.github.dexclub.core.api.resource.ManifestUsesSdk
import io.github.dexclub.core.api.resource.ResourceEntry
import io.github.dexclub.core.api.resource.ResourceEntryValueHit
import io.github.dexclub.core.api.resource.ResourceService
import io.github.dexclub.core.api.resource.ResourceTableResult
import io.github.dexclub.core.api.resource.ResourceValue
import io.github.dexclub.core.api.resource.ResolveResourceRequest
import io.github.dexclub.core.api.workspace.GcResult
import io.github.dexclub.core.api.workspace.InspectResult
import io.github.dexclub.core.api.workspace.TargetHandle
import io.github.dexclub.core.api.workspace.TargetSnapshotSummary
import io.github.dexclub.core.api.workspace.TargetSummary
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceRef
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.api.workspace.WorkspaceStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal fun fakeWorkspaceContext(): WorkspaceContext =
    WorkspaceContext(
        workdir = "D:/tmp/workspace",
        dexclubDir = "D:/tmp/workspace/.dexclub",
        workspaceId = "ws-1",
        activeTargetId = "target-1",
        activeTarget = TargetHandle(
            targetId = "target-1",
            inputType = InputType.File,
            inputPath = "sample.apk",
        ),
        snapshot = TargetSnapshotSummary(
            kind = WorkspaceKind.Apk,
            inventoryFingerprint = "inv-1",
            contentFingerprint = "content-1",
            capabilities = CapabilitySet(
                inspect = true,
                findClass = true,
                findMethod = true,
                exportSmali = true,
            ),
            inventoryCounts = InventoryCounts(
                apkCount = 1,
                dexCount = 2,
                manifestCount = 1,
                arscCount = 1,
                binaryXmlCount = 3,
            ),
        ),
    )

class McpAppTest {
    @Test
    fun openTargetSessionDelegatesToWorkspaceInitializeAndCachesSession() {
        val workspace = fakeWorkspaceContext()
        val workspaceService = FakeWorkspaceService(workspace)
        val dexService = FakeDexAnalysisService()
        val app = McpApp(
            services = Services(
                workspace = workspaceService,
                dex = dexService,
                resource = FakeResourceService(),
            ),
            sessionStore = McpSessionStore(),
        )

        val session = app.openTargetSession("sample.apk")

        assertEquals("sample.apk", workspaceService.initializedInput)
        assertEquals(workspace.workdir, session.workspace.workdir)
        assertEquals(session, app.getTargetSession(session.sessionId))
    }

    @Test
    fun listTargetSessionsReturnsNewestFirst() {
        val timestamps = mutableListOf(
            java.time.Instant.parse("2026-05-03T10:00:00Z"),
            java.time.Instant.parse("2026-05-03T10:00:01Z"),
        )
        val workspaceService = FakeWorkspaceService(fakeWorkspaceContext())
        val app = McpApp(
            services = Services(
                workspace = workspaceService,
                dex = FakeDexAnalysisService(),
                resource = FakeResourceService(),
            ),
            sessionStore = McpSessionStore(nowProvider = { timestamps.removeAt(0) }),
        )

        val first = app.openTargetSession("first.apk")
        val second = app.openTargetSession("second.apk")

        val sessions = app.listTargetSessions()

        assertEquals(listOf(second.sessionId, first.sessionId), sessions.map { it.sessionId })
    }

    @Test
    fun closeTargetSessionRemovesSessionAndClearsHandles() {
        val store = McpSessionStore()
        val session = store.openTargetSession(fakeWorkspaceContext())
        val methodHandle = store.putMethodHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;->foo()V",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )
        val classHandle = store.putClassHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )

        val closed = store.closeTargetSession(session.sessionId)

        assertEquals(session, closed)
        assertEquals(null, store.getTargetSession(session.sessionId))
        assertEquals(null, store.getMethodHandle(session.sessionId, methodHandle))
        assertEquals(null, store.getClassHandle(session.sessionId, classHandle))
    }

    @Test
    fun findMethodsSupportsWorkdirFallbackWithoutSession() {
        val workspace = fakeWorkspaceContext()
        val workspaceService = FakeWorkspaceService(workspace)
        val dexService = FakeDexAnalysisService(
            findMethodsResponse = listOf(
                MethodHit(
                    className = "fixture.samples.SampleSearchTarget",
                    methodName = "exposeNeedle",
                    descriptor = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                ),
            ),
        )
        val app = McpApp(
            services = Services(
                workspace = workspaceService,
                dex = dexService,
                resource = FakeResourceService(),
            ),
            sessionStore = McpSessionStore(),
        )

        val hits = app.findMethods(
            workspace = workspace,
            classNameContains = "SampleSearch",
            methodNameContains = "expose",
        )
        val result = ExecutionContext(session = null, workspace = workspace).toFindMethodsResult(
            hits,
            handleProvider = null,
            fields = setOf("descriptor"),
        )

        assertEquals(null, workspaceService.openedRef)
        assertEquals(null, result.sessionId)
        assertEquals(setOf("descriptor"), result.items.single().keys)
    }

    @Test
    fun exportMethodJavaTextSupportsWorkspaceFallback() {
        val workspace = fakeWorkspaceContext()
        val dexService = FakeDexAnalysisService()
        val app = McpApp(
            services = Services(
                workspace = FakeWorkspaceService(workspace),
                dex = dexService,
                resource = FakeResourceService(),
            ),
            sessionStore = McpSessionStore(),
        )

        val text = app.exportMethodJavaText(
            workspace = workspace,
            descriptor = "Lsample/Test;->foo()V",
            source = SourceLocator(sourcePath = "sample.apk", sourceEntry = "classes.dex"),
        )

        assertEquals("Lsample/Test;->foo()V", dexService.lastExportMethodJavaRequest?.methodSignature)
        assertEquals("method-java:Lsample/Test;->foo()V", text)
    }

    @Test
    fun inspectMethodUsesSessionWorkspaceAndIncludes() {
        val workspace = fakeWorkspaceContext()
        val workspaceService = FakeWorkspaceService(workspace)
        val dexService = FakeDexAnalysisService(
            detail = MethodDetail(
                method = MethodHit(
                    className = "Lsample/Test;",
                    methodName = "foo",
                    descriptor = "Lsample/Test;->foo()V",
                    sourcePath = "sample.apk",
                    sourceEntry = "classes.dex",
                ),
                strings = listOf("alpha"),
                annotations = listOf("Lsample/Anno;"),
            ),
        )
        val app = McpApp(
            services = Services(
                workspace = workspaceService,
                dex = dexService,
                resource = FakeResourceService(),
            ),
            sessionStore = McpSessionStore(),
        )
        val session = app.openTargetSession("sample.apk")

        val detail = app.inspectMethod(
            workspace = session.workspace,
            descriptor = "Lsample/Test;->foo()V",
            includes = setOf(MethodDetailSection.Strings, MethodDetailSection.Annotations),
        )

        assertEquals(workspace, dexService.lastWorkspace)
        assertEquals("Lsample/Test;->foo()V", dexService.lastInspectRequest?.descriptor)
        assertEquals(setOf(MethodDetailSection.Strings, MethodDetailSection.Annotations), dexService.lastInspectRequest?.includes)
        assertEquals(listOf("alpha"), detail.strings)
        assertEquals(listOf("Lsample/Anno;"), detail.annotations)
    }

    @Test
    fun inspectManifestUsesSessionWorkspaceAndCarriesStructuredResult() {
        val workspace = fakeWorkspaceContext()
        val resourceService = FakeResourceService()
        val app = McpApp(
            services = Services(
                workspace = FakeWorkspaceService(workspace),
                dex = FakeDexAnalysisService(),
                resource = resourceService,
            ),
            sessionStore = McpSessionStore(),
        )
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
    fun parseMethodDetailSectionsSupportsCliStyleNames() {
        val sections = parseMethodDetailSections(listOf("using-fields", "callers", "invokes", "strings", "annotations"))

        assertEquals(
            setOf(
                MethodDetailSection.UsingFields,
                MethodDetailSection.Callers,
                MethodDetailSection.Invokes,
                MethodDetailSection.Strings,
                MethodDetailSection.Annotations,
            ),
            sections,
        )
    }

    @Test
    fun parseMethodDetailSectionsFallsBackToAllWhenMissing() {
        val sections = parseMethodDetailSections(null)

        assertEquals(MethodDetailSection.entries.toSet(), sections)
    }

    @Test
    fun inspectMethodBriefReturnsCountsWithoutExpandedSections() {
        val session = McpSessionStore().openTargetSession(fakeWorkspaceContext())
        val result = session.sessionContext().toInspectMethodResult(
            detail = MethodDetail(
                method = MethodHit(
                    className = "Lsample/Test;",
                    methodName = "foo",
                    descriptor = "Lsample/Test;->foo()V",
                ),
                usingFields = emptyList(),
                callers = listOf(
                    MethodHit(
                        className = "Lcaller/Test;",
                        methodName = "call",
                        descriptor = "Lcaller/Test;->call()V",
                    ),
                ),
                invokes = listOf(
                    MethodHit(
                        className = "Lcallee/Test;",
                        methodName = "run",
                        descriptor = "Lcallee/Test;->run()V",
                    ),
                ),
                strings = listOf("alpha", "beta"),
                annotations = listOf("Lsample/Anno;"),
            ),
            brief = true,
        )

        assertEquals(0, result.detail.counts?.usingFields)
        assertEquals(1, result.detail.counts?.callers)
        assertEquals(1, result.detail.counts?.invokes)
        assertEquals(2, result.detail.counts?.strings)
        assertEquals(1, result.detail.counts?.annotations)
        assertEquals(null, result.detail.callers)
        assertEquals(null, result.detail.invokes)
        assertEquals(null, result.detail.strings)
        assertEquals(null, result.detail.annotations)
    }

    @Test
    fun findMethodsUsingStringsUsesSessionWorkspaceAndMapsShortInputs() {
        val workspace = fakeWorkspaceContext()
        val workspaceService = FakeWorkspaceService(workspace)
        val dexService = FakeDexAnalysisService(
            findMethodsUsingStringsResponses = listOf(
                listOf(
                    MethodHit(
                        className = "fixture.samples.SampleSearchTarget",
                        methodName = "exposeNeedle",
                        descriptor = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                        sourcePath = "fixture.dex",
                        sourceEntry = null,
                    ),
                    MethodHit(
                        className = "fixture.samples.OtherTarget",
                        methodName = "secondary",
                        descriptor = "Lfixture/samples/OtherTarget;->secondary()V",
                        sourcePath = "fixture.dex",
                        sourceEntry = null,
                    ),
                ),
                listOf(
                    MethodHit(
                        className = "fixture.samples.SampleSearchTarget",
                        methodName = "exposeNeedle",
                        descriptor = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                        sourcePath = "fixture.dex",
                        sourceEntry = null,
                    ),
                ),
            ),
        )
        val app = McpApp(
            services = Services(
                workspace = workspaceService,
                dex = dexService,
                resource = FakeResourceService(),
            ),
            sessionStore = McpSessionStore(),
        )
        val session = app.openTargetSession("sample.apk")

        val hits = app.findMethodsUsingStrings(
            workspace = session.workspace,
            containsAnyStrings = listOf("needle-a", "needle-b"),
            containsAllStrings = listOf("must-have"),
            offset = 1,
            limit = 5,
        )

        assertEquals(workspace, dexService.lastWorkspace)
        assertEquals(2, dexService.findMethodsUsingStringsRequests.size)
        val anyQuery = Json.parseToJsonElement(dexService.findMethodsUsingStringsRequests[0].queryText).jsonObject["groups"]!!.jsonObject
        assertEquals(1, anyQuery["any-0"]!!.jsonArray.size)
        assertEquals("needle-a", anyQuery["any-0"]!!.jsonArray[0].jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals("needle-b", anyQuery["any-1"]!!.jsonArray[0].jsonObject["value"]!!.jsonPrimitive.content)
        val allQuery = Json.parseToJsonElement(dexService.findMethodsUsingStringsRequests[1].queryText).jsonObject["groups"]!!.jsonObject
        assertEquals(1, allQuery["all"]!!.jsonArray.size)
        assertEquals("must-have", allQuery["all"]!!.jsonArray[0].jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals(1, hits.total)
        assertEquals(1, hits.offset)
        assertEquals(5, hits.limit)
        assertEquals(false, hits.hasMore)
        assertTrue(hits.items.isEmpty())
    }

    @Test
    fun findMethodsUsesSessionWorkspaceAndMapsShortInputs() {
        val workspace = fakeWorkspaceContext()
        val workspaceService = FakeWorkspaceService(workspace)
        val dexService = FakeDexAnalysisService(
            findMethodsResponse = listOf(
                MethodHit(
                    className = "fixture.samples.SampleSearchTarget",
                    methodName = "exposeNeedle",
                    descriptor = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                    sourcePath = "fixture.dex",
                    sourceEntry = null,
                ),
                MethodHit(
                    className = "fixture.samples.SampleSearchTarget",
                    methodName = "secondary",
                    descriptor = "Lfixture/samples/SampleSearchTarget;->secondary()V",
                    sourcePath = "fixture.dex",
                    sourceEntry = null,
                ),
            ),
        )
        val app = McpApp(
            services = Services(
                workspace = workspaceService,
                dex = dexService,
                resource = FakeResourceService(),
            ),
            sessionStore = McpSessionStore(),
        )
        val session = app.openTargetSession("sample.apk")

        val hits = app.findMethods(
            workspace = session.workspace,
            classNameContains = "SampleSearch",
            methodNameContains = "expose",
            descriptorContains = "Needle",
            offset = 0,
            limit = 10,
        )

        assertEquals(workspace, dexService.lastWorkspace)
        val query = Json.parseToJsonElement(dexService.lastFindMethodsRequest!!.queryText).jsonObject
        val matcher = query["matcher"]!!.jsonObject
        assertEquals(
            "SampleSearch",
            matcher["declaredClass"]!!.jsonObject["className"]!!.jsonObject["value"]!!.jsonPrimitive.content,
        )
        assertEquals(
            "expose",
            matcher["name"]!!.jsonObject["value"]!!.jsonPrimitive.content,
        )
        assertEquals(1, hits.total)
        assertEquals(0, hits.offset)
        assertEquals(10, hits.limit)
        assertEquals(false, hits.hasMore)
        assertEquals("Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;", hits.items.single().descriptor)
    }

    @Test
    fun findMethodsResultSupportsFieldProjectionAndBrief() {
        val session = McpSessionStore().openTargetSession(fakeWorkspaceContext())
        val result = session.sessionContext().toFindMethodsResult(
            result = WindowedItems(
                total = 2,
                offset = 0,
                limit = 1,
                hasMore = true,
                items = listOf(
                    MethodHit(
                        className = "fixture.samples.SampleSearchTarget",
                        methodName = "exposeNeedle",
                        descriptor = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                        sourcePath = "fixture.dex",
                        sourceEntry = "classes.dex",
                    ),
                ),
            ),
            handleProvider = { "method-handle-1" },
            fields = setOf("descriptor"),
            brief = true,
        )

        val item = result.items.single()
        assertEquals("Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;", item["descriptor"]!!.jsonPrimitive.content)
        assertEquals(setOf("descriptor"), item.keys)
        assertEquals(0, result.offset)
        assertEquals(1, result.limit)
        assertEquals(true, result.hasMore)
    }

    @Test
    fun buildFindMethodsRequestRejectsEmptyFilters() {
        val error = kotlin.test.assertFailsWith<IllegalArgumentException> {
            buildFindMethodsRequest(
                classNameContains = null,
                methodNameContains = null,
            )
        }

        assertEquals("At least one of class_name_contains or method_name_contains is required", error.message)
    }

    @Test
    fun buildFindMethodsUsingStringsRequestRejectsEmptyFilters() {
        val error = kotlin.test.assertFailsWith<IllegalArgumentException> {
            buildFindMethodsUsingStringsRequest(
                strings = emptyList(),
                requireAll = false,
            )
        }

        assertEquals("At least one non-blank string filter is required", error.message)
    }

    @Test
    fun findMethodsResultCanProjectMethodHandle() {
        val session = McpSessionStore().openTargetSession(fakeWorkspaceContext())
        val result = session.sessionContext().toFindMethodsResult(
            result = WindowedItems(
                total = 1,
                offset = 0,
                limit = 1,
                hasMore = false,
                items = listOf(
                    MethodHit(
                        className = "fixture.samples.SampleSearchTarget",
                        methodName = "exposeNeedle",
                        descriptor = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                    ),
                ),
            ),
            handleProvider = { "method-handle-1" },
            fields = setOf("methodHandle"),
        )

        assertEquals("method-handle-1", result.items.single()["methodHandle"]!!.jsonPrimitive.content)
    }

    @Test
    fun findClassesUsingStringsResultCanProjectClassHandle() {
        val session = McpSessionStore().openTargetSession(fakeWorkspaceContext())
        val result = session.sessionContext().toFindClassesUsingStringsResult(
            result = WindowedItems(
                total = 1,
                offset = 0,
                limit = 1,
                hasMore = false,
                items = listOf(
                    ClassHit(
                        className = "Lfixture/samples/SampleSearchTarget;",
                    ),
                ),
            ),
            handleProvider = { "class-handle-1" },
            fields = setOf("classHandle"),
        )

        assertEquals("class-handle-1", result.items.single()["classHandle"]!!.jsonPrimitive.content)
    }

    @Test
    fun sessionStoreCanReuseMethodHandleAcrossInspectAndExport() {
        val store = McpSessionStore()
        val session = store.openTargetSession(fakeWorkspaceContext())

        val handle = store.putMethodHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;->foo()V",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )

        val resolved = store.getMethodHandle(session.sessionId, handle)

        assertEquals("Lsample/Test;->foo()V", resolved?.descriptor)
        assertEquals("sample.apk", resolved?.sourcePath)
        assertEquals("classes.dex", resolved?.sourceEntry)
    }

    @Test
    fun sessionStoreCanReuseClassHandleAcrossExport() {
        val store = McpSessionStore()
        val session = store.openTargetSession(fakeWorkspaceContext())

        val handle = store.putClassHandle(
            sessionId = session.sessionId,
            descriptor = "Lsample/Test;",
            sourcePath = "sample.apk",
            sourceEntry = "classes.dex",
        )

        val resolved = store.getClassHandle(session.sessionId, handle)

        assertEquals("Lsample/Test;", resolved?.descriptor)
        assertEquals("sample.apk", resolved?.sourcePath)
        assertEquals("classes.dex", resolved?.sourceEntry)
    }

    @Test
    fun findClassesUsingStringsUsesSessionWorkspaceAndMapsShortInputs() {
        val workspace = fakeWorkspaceContext()
        val workspaceService = FakeWorkspaceService(workspace)
        val dexService = FakeDexAnalysisService(
            findClassesUsingStringsResponses = listOf(
                listOf(
                    ClassHit(
                        className = "Lfixture/samples/SampleSearchTarget;",
                        sourcePath = "fixture.dex",
                        sourceEntry = null,
                    ),
                    ClassHit(
                        className = "Lfixture/samples/OtherTarget;",
                        sourcePath = "fixture.dex",
                        sourceEntry = null,
                    ),
                ),
                listOf(
                    ClassHit(
                        className = "Lfixture/samples/SampleSearchTarget;",
                        sourcePath = "fixture.dex",
                        sourceEntry = null,
                    ),
                ),
            ),
        )
        val app = McpApp(
            services = Services(
                workspace = workspaceService,
                dex = dexService,
                resource = FakeResourceService(),
            ),
            sessionStore = McpSessionStore(),
        )
        val session = app.openTargetSession("sample.apk")

        val hits = app.findClassesUsingStrings(
            workspace = session.workspace,
            containsAnyStrings = listOf("needle-a", "needle-b"),
            containsAllStrings = listOf("must-have"),
            offset = 1,
            limit = 5,
        )

        assertEquals(workspace, dexService.lastWorkspace)
        assertEquals(2, dexService.findClassesUsingStringsRequests.size)
        val anyQuery = Json.parseToJsonElement(dexService.findClassesUsingStringsRequests[0].queryText).jsonObject["groups"]!!.jsonObject
        assertEquals(1, anyQuery["any-0"]!!.jsonArray.size)
        assertEquals("needle-a", anyQuery["any-0"]!!.jsonArray[0].jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals("needle-b", anyQuery["any-1"]!!.jsonArray[0].jsonObject["value"]!!.jsonPrimitive.content)
        val allQuery = Json.parseToJsonElement(dexService.findClassesUsingStringsRequests[1].queryText).jsonObject["groups"]!!.jsonObject
        assertEquals(1, allQuery["all"]!!.jsonArray.size)
        assertEquals("must-have", allQuery["all"]!!.jsonArray[0].jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals(1, hits.total)
        assertEquals(1, hits.offset)
        assertEquals(5, hits.limit)
        assertEquals(false, hits.hasMore)
        assertTrue(hits.items.isEmpty())
    }

    @Test
    fun buildFindClassesUsingStringsRequestRejectsEmptyFilters() {
        val error = kotlin.test.assertFailsWith<IllegalArgumentException> {
            buildFindClassesUsingStringsRequest(
                strings = emptyList(),
                requireAll = false,
            )
        }

        assertEquals("At least one non-blank string filter is required", error.message)
    }

    @Test
    fun exportMethodJavaTextUsesSessionWorkspaceAndReturnsFileContent() {
        val workspace = fakeWorkspaceContext()
        val dexService = FakeDexAnalysisService()
        val app = McpApp(
            services = Services(
                workspace = FakeWorkspaceService(workspace),
                dex = dexService,
                resource = FakeResourceService(),
            ),
            sessionStore = McpSessionStore(),
        )
        val session = app.openTargetSession("sample.apk")

        val text = app.exportMethodJavaText(
            workspace = session.workspace,
            descriptor = "Lsample/Test;->foo()V",
            source = SourceLocator(sourcePath = "sample.apk", sourceEntry = "classes.dex"),
        )

        assertEquals(workspace, dexService.lastWorkspace)
        assertEquals("Lsample/Test;->foo()V", dexService.lastExportMethodJavaRequest?.methodSignature)
        assertEquals(SourceLocator(sourcePath = "sample.apk", sourceEntry = "classes.dex"), dexService.lastExportMethodJavaRequest?.source)
        assertTrue(
            dexService.lastExportMethodJavaRequest!!.outputPath.replace('\\', '/')
                .contains("/.dexclub/targets/${workspace.activeTargetId}/cache/exports/tmp/"),
        )
        assertEquals("method-java:Lsample/Test;->foo()V", text)
    }

    @Test
    fun exportMethodSmaliTextSupportsClassMode() {
        val workspace = fakeWorkspaceContext()
        val dexService = FakeDexAnalysisService()
        val app = McpApp(
            services = Services(
                workspace = FakeWorkspaceService(workspace),
                dex = dexService,
                resource = FakeResourceService(),
            ),
            sessionStore = McpSessionStore(),
        )
        val session = app.openTargetSession("sample.apk")

        val text = app.exportMethodSmaliText(
            workspace = session.workspace,
            descriptor = "Lsample/Test;->foo()V",
            source = SourceLocator(sourcePath = "sample.apk", sourceEntry = "classes.dex"),
            mode = "class",
        )

        assertEquals(MethodSmaliMode.Class, dexService.lastExportMethodSmaliRequest?.mode)
        assertTrue(
            dexService.lastExportMethodSmaliRequest!!.outputPath.replace('\\', '/')
                .contains("/.dexclub/targets/${workspace.activeTargetId}/cache/exports/tmp/"),
        )
        assertEquals("method-smali:Lsample/Test;->foo()V:class", text)
    }

    @Test
    fun exportClassJavaTextUsesSessionWorkspaceAndReturnsFileContent() {
        val workspace = fakeWorkspaceContext()
        val dexService = FakeDexAnalysisService()
        val app = McpApp(
            services = Services(
                workspace = FakeWorkspaceService(workspace),
                dex = dexService,
                resource = FakeResourceService(),
            ),
            sessionStore = McpSessionStore(),
        )
        val session = app.openTargetSession("sample.apk")

        val text = app.exportClassJavaText(
            workspace = session.workspace,
            descriptor = "Lsample/Test;",
            source = SourceLocator(sourcePath = "sample.apk", sourceEntry = "classes.dex"),
        )

        assertEquals("Lsample/Test;", dexService.lastExportClassJavaRequest?.className)
        assertTrue(
            dexService.lastExportClassJavaRequest!!.outputPath.replace('\\', '/')
                .contains("/.dexclub/targets/${workspace.activeTargetId}/cache/exports/tmp/"),
        )
        assertEquals("class-java:Lsample/Test;", text)
    }

    @Test
    fun exportClassSmaliTextUsesSessionWorkspaceAndReturnsFileContent() {
        val workspace = fakeWorkspaceContext()
        val dexService = FakeDexAnalysisService()
        val app = McpApp(
            services = Services(
                workspace = FakeWorkspaceService(workspace),
                dex = dexService,
                resource = FakeResourceService(),
            ),
            sessionStore = McpSessionStore(),
        )
        val session = app.openTargetSession("sample.apk")

        val text = app.exportClassSmaliText(
            workspace = session.workspace,
            descriptor = "Lsample/Test;",
            source = SourceLocator(sourcePath = "sample.apk", sourceEntry = "classes.dex"),
        )

        assertEquals("Lsample/Test;", dexService.lastExportClassSmaliRequest?.className)
        assertTrue(
            dexService.lastExportClassSmaliRequest!!.outputPath.replace('\\', '/')
                .contains("/.dexclub/targets/${workspace.activeTargetId}/cache/exports/tmp/"),
        )
        assertEquals("class-smali:Lsample/Test;", text)
    }

    @Test
    fun resolveResourceUsesSessionWorkspaceAndSupportsResourceId() {
        val workspace = fakeWorkspaceContext()
        val resourceService = FakeResourceService()
        val app = McpApp(
            services = Services(
                workspace = FakeWorkspaceService(workspace),
                dex = FakeDexAnalysisService(),
                resource = resourceService,
            ),
            sessionStore = McpSessionStore(),
        )
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
        val app = McpApp(
            services = Services(
                workspace = FakeWorkspaceService(workspace),
                dex = FakeDexAnalysisService(),
                resource = resourceService,
            ),
            sessionStore = McpSessionStore(),
        )
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
                    resolution = io.github.dexclub.core.api.resource.ResourceResolution.TableBacked,
                ),
                ResourceEntry(
                    resourceId = "0x7f010002",
                    type = "layout",
                    name = "main",
                    filePath = "res/layout/main.xml",
                    sourcePath = "sample.apk",
                    sourceEntry = "res/layout/main.xml",
                    resolution = io.github.dexclub.core.api.resource.ResourceResolution.TableBacked,
                ),
                ResourceEntry(
                    resourceId = "0x7f010003",
                    type = "string",
                    name = "beta",
                    filePath = "res/values/strings.xml",
                    sourcePath = "sample.apk",
                    sourceEntry = "res/values/strings.xml",
                    resolution = io.github.dexclub.core.api.resource.ResourceResolution.TableBacked,
                ),
            ),
        )
        val app = McpApp(
            services = Services(
                workspace = FakeWorkspaceService(workspace),
                dex = FakeDexAnalysisService(),
                resource = resourceService,
            ),
            sessionStore = McpSessionStore(),
        )
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
        val app = McpApp(
            services = Services(
                workspace = FakeWorkspaceService(workspace),
                dex = FakeDexAnalysisService(),
                resource = resourceService,
            ),
            sessionStore = McpSessionStore(),
        )
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

}

internal class FakeWorkspaceService(
    private val workspace: WorkspaceContext,
) : WorkspaceService {
    var initializedInput: String? = null
    var openedRef: WorkspaceRef? = null

    override fun initialize(input: String): WorkspaceContext {
        initializedInput = input
        return workspace
    }

    override fun switchTarget(ref: WorkspaceRef, input: String): WorkspaceRef = ref

    override fun open(ref: WorkspaceRef): WorkspaceContext {
        openedRef = ref
        return workspace
    }

    override fun listTargets(ref: WorkspaceRef): List<TargetSummary> = emptyList()

    override fun loadStatus(ref: WorkspaceRef): WorkspaceStatus =
        WorkspaceStatus(
            workspaceId = workspace.workspaceId,
            activeTargetId = workspace.activeTargetId,
            state = WorkspaceState.Healthy,
            issues = emptyList<WorkspaceIssue>(),
            activeTarget = workspace.activeTarget,
            snapshot = workspace.snapshot,
            cacheState = CacheState.Present,
        )

    override fun gc(workspace: WorkspaceContext): GcResult =
        GcResult(
            workdir = workspace.workdir,
            targetId = workspace.activeTargetId,
            deletedFiles = 0,
            deletedBytes = 0,
        )

    override fun inspect(workspace: WorkspaceContext): InspectResult =
        InspectResult(
            target = workspace.activeTarget,
            snapshot = workspace.snapshot,
            classCount = null,
        )
}

internal class FakeDexAnalysisService(
    private val detail: MethodDetail = MethodDetail(
        method = MethodHit(
            className = "Lsample/Test;",
            methodName = "foo",
            descriptor = "Lsample/Test;->foo()V",
        ),
    ),
    private val findMethodsResponse: List<MethodHit> = emptyList(),
    private val findClassesUsingStringsResponses: List<List<ClassHit>> = emptyList(),
    private val findMethodsUsingStringsResponses: List<List<MethodHit>> = emptyList(),
) : DexAnalysisService {
    var lastWorkspace: WorkspaceContext? = null
    var lastInspectRequest: InspectMethodRequest? = null
    var lastFindMethodsRequest: FindMethodsRequest? = null
    var lastFindClassesUsingStringsRequest: FindClassesUsingStringsRequest? = null
    var lastFindMethodsUsingStringsRequest: FindMethodsUsingStringsRequest? = null
    var lastExportClassSmaliRequest: ExportClassSmaliRequest? = null
    var lastExportClassJavaRequest: ExportClassJavaRequest? = null
    var lastExportMethodSmaliRequest: ExportMethodSmaliRequest? = null
    var lastExportMethodJavaRequest: ExportMethodJavaRequest? = null
    val findClassesUsingStringsRequests = mutableListOf<FindClassesUsingStringsRequest>()
    val findMethodsUsingStringsRequests = mutableListOf<FindMethodsUsingStringsRequest>()

    override fun findClasses(workspace: WorkspaceContext, request: FindClassesRequest): List<ClassHit> = emptyList()

    override fun findMethods(workspace: WorkspaceContext, request: FindMethodsRequest): List<MethodHit> {
        lastWorkspace = workspace
        lastFindMethodsRequest = request
        return findMethodsResponse
    }

    override fun findFields(workspace: WorkspaceContext, request: FindFieldsRequest): List<FieldHit> = emptyList()

    override fun findClassesUsingStrings(
        workspace: WorkspaceContext,
        request: FindClassesUsingStringsRequest,
    ): List<ClassHit> {
        lastWorkspace = workspace
        lastFindClassesUsingStringsRequest = request
        findClassesUsingStringsRequests += request
        val nextIndex = findClassesUsingStringsRequests.size - 1
        return findClassesUsingStringsResponses.getOrElse(nextIndex) { emptyList() }
    }

    override fun findMethodsUsingStrings(
        workspace: WorkspaceContext,
        request: FindMethodsUsingStringsRequest,
    ): List<MethodHit> {
        lastWorkspace = workspace
        lastFindMethodsUsingStringsRequest = request
        findMethodsUsingStringsRequests += request
        val nextIndex = findMethodsUsingStringsRequests.size - 1
        return findMethodsUsingStringsResponses.getOrElse(nextIndex) { emptyList() }
    }

    override fun inspectMethod(workspace: WorkspaceContext, request: InspectMethodRequest): MethodDetail {
        lastWorkspace = workspace
        lastInspectRequest = request
        return detail
    }

    override fun exportClassDex(workspace: WorkspaceContext, request: ExportClassDexRequest): ExportResult =
        ExportResult(request.outputPath)

    override fun exportClassSmali(workspace: WorkspaceContext, request: ExportClassSmaliRequest): ExportResult {
        lastWorkspace = workspace
        lastExportClassSmaliRequest = request
        java.io.File(request.outputPath).writeText("class-smali:${request.className}")
        return ExportResult(request.outputPath)
    }

    override fun exportClassJava(workspace: WorkspaceContext, request: ExportClassJavaRequest): ExportResult {
        lastWorkspace = workspace
        lastExportClassJavaRequest = request
        java.io.File(request.outputPath).writeText("class-java:${request.className}")
        return ExportResult(request.outputPath)
    }

    override fun exportMethodSmali(workspace: WorkspaceContext, request: ExportMethodSmaliRequest): ExportResult {
        lastWorkspace = workspace
        lastExportMethodSmaliRequest = request
        java.io.File(request.outputPath).writeText(
            "method-smali:${request.methodSignature}:${request.mode.name.lowercase()}",
        )
        return ExportResult(request.outputPath)
    }

    override fun exportMethodDex(workspace: WorkspaceContext, request: ExportMethodDexRequest): ExportResult =
        ExportResult(request.outputPath)

    override fun exportMethodJava(workspace: WorkspaceContext, request: ExportMethodJavaRequest): ExportResult {
        lastWorkspace = workspace
        lastExportMethodJavaRequest = request
        java.io.File(request.outputPath).writeText("method-java:${request.methodSignature}")
        return ExportResult(request.outputPath)
    }
}

internal class FakeResourceService : ResourceService {
    var lastWorkspace: WorkspaceContext? = null
    var lastInspectManifestRequest: InspectManifestRequest? = null
    var lastResolveResourceRequest: ResolveResourceRequest? = null
    var lastFindResourcesRequest: FindResourcesRequest? = null
    private val resourceEntries: List<ResourceEntry>
    private val resourceValueHits: List<ResourceEntryValueHit>

    constructor(
        resourceEntries: List<ResourceEntry> = emptyList(),
        resourceValueHits: List<ResourceEntryValueHit> = emptyList(),
    ) {
        this.resourceEntries = resourceEntries
        this.resourceValueHits = resourceValueHits
    }

    override fun decodeManifest(workspace: WorkspaceContext): ManifestResult {
        lastWorkspace = workspace
        return ManifestResult(
            sourcePath = "sample.apk",
            sourceEntry = "AndroidManifest.xml",
            text = "<manifest package=\"fixture.sample\"/>",
        )
    }

    override fun inspectManifest(
        workspace: WorkspaceContext,
        request: InspectManifestRequest,
    ): ManifestInspectionResult {
        lastWorkspace = workspace
        lastInspectManifestRequest = request
        return ManifestInspectionResult(
            sourcePath = "sample.apk",
            sourceEntry = "AndroidManifest.xml",
            packageName = "fixture.sample",
            versionName = "1.0",
            usesSdk = ManifestUsesSdk(minSdkVersion = "21", targetSdkVersion = "34"),
            application = ManifestApplicationInfo(
                name = "fixture.sample.App",
                metaData = listOf(ManifestMetaData(name = "feature", value = "enabled")),
            ),
            activities = listOf(
                ManifestComponentInfo(
                    name = "fixture.sample.MainActivity",
                    exported = true,
                    intentFilters = listOf(
                        ManifestIntentFilter(actions = listOf("android.intent.action.MAIN")),
                    ),
                ),
            ),
            text = "<manifest package=\"fixture.sample\"/>".takeIf { request.includeText },
        )
    }

    override fun dumpResourceTable(workspace: WorkspaceContext): ResourceTableResult =
        ResourceTableResult(packageCount = 0, typeCount = 0, entryCount = 0)

    override fun decodeXml(workspace: WorkspaceContext, request: DecodeXmlRequest): DecodedXmlResult =
        DecodedXmlResult(text = "<xml/>")

    override fun listResourceEntries(workspace: WorkspaceContext): List<ResourceEntry> =
        resourceEntries.also {
            lastWorkspace = workspace
        }

    override fun getResourceValue(workspace: WorkspaceContext, request: ResolveResourceRequest): ResourceValue =
        ResourceValue(
            resourceId = request.resourceId ?: "0x7f010001",
            type = request.type ?: "string",
            name = request.name ?: "fixture_name",
            value = "Fixture Name",
        ).also {
            lastWorkspace = workspace
            lastResolveResourceRequest = request
        }

    override fun findResourceValues(
        workspace: WorkspaceContext,
        request: FindResourcesRequest,
    ): List<ResourceEntryValueHit> =
        resourceValueHits.also {
            lastWorkspace = workspace
            lastFindResourcesRequest = request
        }
}
