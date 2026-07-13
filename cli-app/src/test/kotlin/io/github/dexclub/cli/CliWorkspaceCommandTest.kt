package io.github.dexclub.cli

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
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.app.createDefaultAppServices
import io.github.dexclub.core.api.shared.CacheState
import io.github.dexclub.core.api.shared.CapabilitySet
import io.github.dexclub.core.api.shared.InputType
import io.github.dexclub.core.api.shared.InventoryCounts
import io.github.dexclub.core.api.shared.Services
import io.github.dexclub.core.api.shared.WorkspaceIssue
import io.github.dexclub.core.api.shared.WorkspaceKind
import io.github.dexclub.core.api.shared.WorkspaceState
import io.github.dexclub.core.api.resource.DecodeXmlRequest
import io.github.dexclub.core.api.resource.DecodedXmlResult
import io.github.dexclub.core.api.resource.FindResourcesRequest
import io.github.dexclub.core.api.resource.InspectManifestRequest
import io.github.dexclub.core.api.resource.ManifestInspectionResult
import io.github.dexclub.core.api.resource.ManifestResult
import io.github.dexclub.core.api.resource.ResourceEntry
import io.github.dexclub.core.api.resource.ResourceEntryValueHit
import io.github.dexclub.core.api.resource.ResourceService
import io.github.dexclub.core.api.resource.ResourceTableResult
import io.github.dexclub.core.api.resource.ResolveResourceRequest
import io.github.dexclub.core.api.resource.ResourceValue
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
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CliWorkspaceCommandTest {
    @Test
    fun initStatusGcAndInspectCommandsRunThroughCliPipeline() {
        val workspaceDir = createTempDirectory("dexclub")
        val dexFile = workspaceDir.resolve("1.dex")
        dexFile.writeText("")
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", dexFile.toString()))
        assertEquals(0, initOut.exitCode)
        assertTrue(initOut.stdout.contains("state=healthy"))
        assertTrue(workspaceDir.resolve(".dexclub/workspace.json").exists())

        val statusOut = runCli(app, listOf("status"))
        assertEquals(0, statusOut.exitCode)
        assertTrue(statusOut.stdout.contains("workspaceId="))
        assertTrue(statusOut.stdout.contains("kind=dex"))

        val targetId = workspaceDir.resolve(".dexclub/targets").toFile().listFiles()!!.single().name
        val cacheFile = workspaceDir.resolve(".dexclub/targets/$targetId/cache/decoded/manifest.json")
        cacheFile.parent.createDirectories()
        cacheFile.writeText("cached")
        val gcOut = runCli(app, listOf("gc"))
        assertEquals(0, gcOut.exitCode)
        assertTrue(gcOut.stdout.contains("deletedFiles=1"))
        assertTrue(!cacheFile.exists())

        val inspectOut = runCli(app, listOf("inspect"))
        assertEquals(0, inspectOut.exitCode)
        assertTrue(inspectOut.stdout.contains("dexCount=1"))
        assertTrue(inspectOut.stdout.contains("capabilities=inspect,findClass"))
        assertTrue(!inspectOut.stdout.contains("classCount="))
    }

    @Test
    fun refreshCommandRebuildsSnapshotExplicitly() {
        val workspaceDir = createTempDirectory("dexclub-refresh")
        val dexFile = workspaceDir.resolve("1.dex")
        dexFile.writeText("before")
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initOut = runCli(app, listOf("init", dexFile.toString()))
        assertEquals(0, initOut.exitCode, initOut.stderr)
        val initialStatus = runCli(app, listOf("status", "--json"))
        assertEquals(0, initialStatus.exitCode, initialStatus.stderr)
        val initialFingerprint = Json.parseToJsonElement(initialStatus.stdout).jsonObject
            .getValue("contentFingerprint")
            .jsonPrimitive
            .content

        dexFile.writeText("after")

        val refreshOut = runCli(app, listOf("refresh", "--json"))
        assertEquals(0, refreshOut.exitCode, refreshOut.stderr)
        assertEquals("1", Json.parseToJsonElement(refreshOut.stdout).jsonObject
            .getValue("dexCount")
            .jsonPrimitive
            .content)

        val statusOut = runCli(app, listOf("status", "--json"))
        assertEquals(0, statusOut.exitCode, statusOut.stderr)
        val refreshedFingerprint = Json.parseToJsonElement(statusOut.stdout).jsonObject
            .getValue("contentFingerprint")
            .jsonPrimitive
            .content

        assertNotEquals(initialFingerprint, refreshedFingerprint)
    }

    @Test
    fun switchCommandReactivatesPreviouslyInitializedTarget() {
        val workspaceDir = createTempDirectory("dexclub-switch")
        val aDex = workspaceDir.resolve("a.dex")
        val bDex = workspaceDir.resolve("b.dex")
        aDex.writeText("")
        bDex.writeText("")
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        val initA = runCli(app, listOf("init", aDex.toString()))
        assertEquals(0, initA.exitCode)

        val initB = runCli(app, listOf("init", bDex.toString()))
        assertEquals(0, initB.exitCode)
        assertTrue(initB.stdout.contains("inputPath=b.dex"))

        val switched = runCli(app, listOf("switch", aDex.toString()))
        assertEquals(0, switched.exitCode, switched.stderr)
        assertTrue(switched.stdout.contains("inputPath=a.dex"))

        val status = runCli(app, listOf("status"))
        assertEquals(0, status.exitCode)
        assertTrue(status.stdout.contains("inputPath=a.dex"))
    }

    @Test
    fun targetsCommandListsInitializedTargetsAndMarksActiveOne() {
        val workspaceDir = createTempDirectory("dexclub-targets")
        val aDex = workspaceDir.resolve("a.dex")
        val bDex = workspaceDir.resolve("b.dex")
        aDex.writeText("")
        bDex.writeText("")
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        assertEquals(0, runCli(app, listOf("init", aDex.toString())).exitCode)
        assertEquals(0, runCli(app, listOf("init", bDex.toString())).exitCode)

        val textOut = runCli(app, listOf("targets"))
        assertEquals(0, textOut.exitCode, textOut.stderr)
        assertTrue(textOut.stdout.contains("active\ttargetId\tinputType\tinputPath\tcreatedAt\tupdatedAt"))
        assertTrue(textOut.stdout.contains("file\ta.dex"))
        assertTrue(textOut.stdout.contains("*\t"))
        assertTrue(textOut.stdout.contains("file\tb.dex"))

        val jsonOut = runCli(app, listOf("targets", "--json"))
        assertEquals(0, jsonOut.exitCode, jsonOut.stderr)
        val parsed = Json.parseToJsonElement(jsonOut.stdout).jsonArray
        assertEquals(2, parsed.size)
        assertEquals(listOf("a.dex", "b.dex"), parsed.map { it.jsonObject.getValue("inputPath").jsonPrimitive.content })
        assertEquals(listOf(false, true), parsed.map { it.jsonObject.getValue("active").jsonPrimitive.content.toBoolean() })
    }

    @Test
    fun switchCommandCanReactivateMissingTargetInputWithinCurrentWorkspace() {
        val workspaceDir = createTempDirectory("dexclub-switch-missing")
        val aDex = workspaceDir.resolve("a.dex")
        val bDex = workspaceDir.resolve("b.dex")
        aDex.writeText("")
        bDex.writeText("")
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        assertEquals(0, runCli(app, listOf("init", aDex.toString())).exitCode)
        assertEquals(0, runCli(app, listOf("init", bDex.toString())).exitCode)
        aDex.deleteExisting()

        val switched = runCli(app, listOf("switch", "a.dex"))
        assertEquals(2, switched.exitCode, switched.stderr)
        assertTrue(switched.stdout.contains("inputPath=a.dex"))
        assertTrue(switched.stdout.contains("state=broken"))
    }

    @Test
    fun statusUsesBrokenExitCodeWhenInputIsMissing() {
        val workspaceDir = createTempDirectory("dexclub-broken")
        val apkFile = workspaceDir.resolve("app.apk")
        apkFile.writeText("")
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { workspaceDir.toString() },
        )

        runCli(app, listOf("init", apkFile.toString()))
        apkFile.deleteExisting()

        val statusOut = runCli(app, listOf("status"))
        assertEquals(2, statusOut.exitCode)
        assertTrue(statusOut.stdout.contains("state=broken"))
        assertTrue(statusOut.stdout.contains("issueCount="))
    }

    @Test
    fun inspectCommandOnlyUsesWorkspaceServiceThroughAppUseCases() {
        val workspace = fakeWorkspaceContext()
        val workspaceService = RecordingWorkspaceService(workspace)
        val dexService = FailingDexAnalysisService()
        val resourceService = FailingResourceService()
        val app = CliApp(
            services = Services(
                workspace = workspaceService,
                dex = dexService,
                resource = resourceService,
            ),
            cwdProvider = { workspace.workdir },
        )

        val output = runCli(app, listOf("inspect"))

        assertEquals(0, output.exitCode, output.stderr)
        assertEquals(listOf(WorkspaceRef(workspace.workdir)), workspaceService.openedRefs)
        assertEquals(listOf(workspace), workspaceService.inspectedWorkspaces)
        assertEquals(0, dexService.calls)
        assertEquals(0, resourceService.calls)
    }
}

private fun fakeWorkspaceContext(): WorkspaceContext =
    cliAppTestDir("workspace").let { workdir ->
        WorkspaceContext(
            workdir = workdir.toString(),
            dexclubDir = cliAppTestDir("workspace", ".dexclub").toString(),
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

private class RecordingWorkspaceService(
    private val workspace: WorkspaceContext,
) : WorkspaceService {
    val openedRefs = mutableListOf<WorkspaceRef>()
    val inspectedWorkspaces = mutableListOf<WorkspaceContext>()

    override fun initialize(input: String): WorkspaceContext = workspace

    override fun switchTarget(ref: WorkspaceRef, input: String): WorkspaceRef = ref

    override fun open(ref: WorkspaceRef): WorkspaceContext {
        openedRefs += ref
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

    override fun refresh(workspace: WorkspaceContext): InspectResult =
        InspectResult(
            target = workspace.activeTarget,
            snapshot = workspace.snapshot,
            classCount = null,
        )

    override fun inspect(workspace: WorkspaceContext): InspectResult {
        inspectedWorkspaces += workspace
        return InspectResult(
            target = workspace.activeTarget,
            snapshot = workspace.snapshot,
            classCount = null,
        )
    }
}

private class FailingDexAnalysisService : DexAnalysisService {
    var calls = 0

    override fun findClasses(workspace: WorkspaceContext, request: FindClassesRequest): List<ClassHit> =
        fail()

    override fun findMethods(workspace: WorkspaceContext, request: FindMethodsRequest): List<MethodHit> =
        fail()

    override fun findFields(workspace: WorkspaceContext, request: FindFieldsRequest): List<FieldHit> =
        fail()

    override fun findClassesUsingStrings(
        workspace: WorkspaceContext,
        request: FindClassesUsingStringsRequest,
    ): List<ClassHit> = fail()

    override fun findMethodsUsingStrings(
        workspace: WorkspaceContext,
        request: FindMethodsUsingStringsRequest,
    ): List<MethodHit> = fail()

    override fun inspectMethod(workspace: WorkspaceContext, request: InspectMethodRequest): MethodDetail = fail()

    override fun exportClassDex(workspace: WorkspaceContext, request: ExportClassDexRequest): ExportResult = fail()

    override fun exportClassSmali(workspace: WorkspaceContext, request: ExportClassSmaliRequest): ExportResult = fail()

    override fun exportClassJava(workspace: WorkspaceContext, request: ExportClassJavaRequest): ExportResult = fail()

    override fun exportMethodSmali(workspace: WorkspaceContext, request: ExportMethodSmaliRequest): ExportResult = fail()

    override fun exportMethodDex(workspace: WorkspaceContext, request: ExportMethodDexRequest): ExportResult = fail()

    override fun exportMethodJava(workspace: WorkspaceContext, request: ExportMethodJavaRequest): ExportResult = fail()

    private fun <T> fail(): T {
        calls += 1
        error("dex service must not be used by cli inspect")
    }
}

private class FailingResourceService : ResourceService {
    var calls = 0

    override fun decodeManifest(workspace: WorkspaceContext): ManifestResult = fail()

    override fun inspectManifest(
        workspace: WorkspaceContext,
        request: InspectManifestRequest,
    ): ManifestInspectionResult = fail()

    override fun dumpResourceTable(workspace: WorkspaceContext): ResourceTableResult = fail()

    override fun decodeXml(workspace: WorkspaceContext, request: DecodeXmlRequest): DecodedXmlResult = fail()

    override fun listResourceEntries(workspace: WorkspaceContext): List<ResourceEntry> = fail()

    override fun getResourceValue(workspace: WorkspaceContext, request: ResolveResourceRequest): ResourceValue = fail()

    override fun findResourceValues(
        workspace: WorkspaceContext,
        request: FindResourcesRequest,
    ): List<ResourceEntryValueHit> = fail()

    private fun <T> fail(): T {
        calls += 1
        error("resource service must not be used by cli inspect")
    }
}
