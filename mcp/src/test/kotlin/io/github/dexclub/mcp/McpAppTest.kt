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
            session = session,
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
            session,
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
            session = session,
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
        assertTrue(hits.items.isEmpty())
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
            session = session,
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
            session = session,
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
            session = session,
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
            session = session,
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
            session = session,
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

    private fun fakeWorkspaceContext(): WorkspaceContext =
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
}

private class FakeWorkspaceService(
    private val workspace: WorkspaceContext,
) : WorkspaceService {
    var initializedInput: String? = null

    override fun initialize(input: String): WorkspaceContext {
        initializedInput = input
        return workspace
    }

    override fun switchTarget(ref: WorkspaceRef, input: String): WorkspaceRef = ref

    override fun open(ref: WorkspaceRef): WorkspaceContext = workspace

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

private class FakeDexAnalysisService(
    private val detail: MethodDetail = MethodDetail(
        method = MethodHit(
            className = "Lsample/Test;",
            methodName = "foo",
            descriptor = "Lsample/Test;->foo()V",
        ),
    ),
    private val findClassesUsingStringsResponses: List<List<ClassHit>> = emptyList(),
    private val findMethodsUsingStringsResponses: List<List<MethodHit>> = emptyList(),
) : DexAnalysisService {
    var lastWorkspace: WorkspaceContext? = null
    var lastInspectRequest: InspectMethodRequest? = null
    var lastFindClassesUsingStringsRequest: FindClassesUsingStringsRequest? = null
    var lastFindMethodsUsingStringsRequest: FindMethodsUsingStringsRequest? = null
    var lastExportClassSmaliRequest: ExportClassSmaliRequest? = null
    var lastExportClassJavaRequest: ExportClassJavaRequest? = null
    var lastExportMethodSmaliRequest: ExportMethodSmaliRequest? = null
    var lastExportMethodJavaRequest: ExportMethodJavaRequest? = null
    val findClassesUsingStringsRequests = mutableListOf<FindClassesUsingStringsRequest>()
    val findMethodsUsingStringsRequests = mutableListOf<FindMethodsUsingStringsRequest>()

    override fun findClasses(workspace: WorkspaceContext, request: FindClassesRequest): List<ClassHit> = emptyList()

    override fun findMethods(workspace: WorkspaceContext, request: FindMethodsRequest): List<MethodHit> = emptyList()

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

private class FakeResourceService : ResourceService {
    var lastWorkspace: WorkspaceContext? = null
    var lastInspectManifestRequest: InspectManifestRequest? = null

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

    override fun listResourceEntries(workspace: WorkspaceContext): List<ResourceEntry> = emptyList()

    override fun resolveResourceValue(workspace: WorkspaceContext, request: ResolveResourceRequest): ResourceValue =
        ResourceValue(type = "string", name = "name", value = null)

    override fun findResourceEntries(
        workspace: WorkspaceContext,
        request: FindResourcesRequest,
    ): List<ResourceEntryValueHit> = emptyList()
}
