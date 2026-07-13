package io.github.dexclub.core.app

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
import io.github.dexclub.core.api.resource.DecodeXmlRequest
import io.github.dexclub.core.api.resource.DecodedXmlResult
import io.github.dexclub.core.api.resource.FindResourcesRequest
import io.github.dexclub.core.api.resource.ManifestInspectionResult
import io.github.dexclub.core.api.resource.ManifestResult
import io.github.dexclub.core.api.resource.ResourceEntry
import io.github.dexclub.core.api.resource.ResourceEntryValueHit
import io.github.dexclub.core.api.resource.ResourceService
import io.github.dexclub.core.api.resource.ResourceTableResult
import io.github.dexclub.core.api.resource.ResourceValue
import io.github.dexclub.core.api.resource.ResolveResourceRequest
import io.github.dexclub.core.api.shared.CapabilitySet
import io.github.dexclub.core.api.shared.InputType
import io.github.dexclub.core.api.shared.InventoryCounts
import io.github.dexclub.core.api.shared.Services
import io.github.dexclub.core.api.shared.WorkspaceKind
import io.github.dexclub.core.api.workspace.GcResult
import io.github.dexclub.core.api.workspace.InspectResult
import io.github.dexclub.core.api.workspace.TargetHandle
import io.github.dexclub.core.api.workspace.TargetSnapshotSummary
import io.github.dexclub.core.api.workspace.TargetSummary
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceRef
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.api.workspace.WorkspaceStatus
import io.github.dexclub.core.app.session.TargetSessionService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppRuntimeTest {
    @Test
    fun appRuntimeBuildsUseCasesOnTopOfSharedServices() {
        val workspace = fakeWorkspaceContext()
        val workspaceService = RuntimeTestWorkspaceService(workspace)
        val runtime = createAppRuntime(
            services = Services(
                workspace = workspaceService,
                dex = CloseTrackingDexAnalysisService(),
                resource = RuntimeTestResourceService(),
            ),
        )

        val session = runtime.appUseCases.sessionService.openTargetSession("sample.apk")
        val opened = runtime.workspaceRuntime.open(workspace.workdir)

        assertEquals("sample.apk", workspaceService.initializedInput)
        assertEquals(workspace.workdir, workspaceService.openedRef?.workdir)
        assertEquals(workspace, session.workspace)
        assertEquals(workspace, opened)

        runtime.close()
    }

    @Test
    fun sessionAppRuntimeReusesProvidedSessionServiceAcrossUseCasesAndRuntime() {
        val sessionService = TargetSessionService()
        val runtime = createSessionAppRuntime(
            services = Services(
                workspace = RuntimeTestWorkspaceService(fakeWorkspaceContext()),
                dex = CloseTrackingDexAnalysisService(),
                resource = RuntimeTestResourceService(),
            ),
            sessionService = sessionService,
        )

        assertSame(sessionService, runtime.sessionService)
        assertSame(sessionService, runtime.appUseCases.sessionService)
        assertSame(sessionService, runtime.sessionRuntime.sessionService)

        runtime.close()
    }

    @Test
    fun closingSessionAppRuntimeReleasesDexContextsAndClosesDexService() {
        val workspace = fakeWorkspaceContext()
        val dexService = CloseTrackingDexAnalysisService()
        val runtime = createSessionAppRuntime(
            services = Services(
                workspace = RuntimeTestWorkspaceService(workspace),
                dex = dexService,
                resource = RuntimeTestResourceService(),
            ),
        )

        runtime.sessionRuntime.openTargetSession("sample.apk")
        runtime.close()

        assertEquals(listOf(workspace), dexService.releasedDexContexts)
        assertTrue(dexService.closed)
    }
}

private fun fakeWorkspaceContext(): WorkspaceContext =
    appServiceTestDir("workspace").let { workdir ->
        WorkspaceContext(
            workdir = workdir.toString(),
            dexclubDir = appServiceTestDir("workspace", ".dexclub").toString(),
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

private class RuntimeTestWorkspaceService(
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

    override fun loadStatus(ref: WorkspaceRef): WorkspaceStatus = TODO("unused in AppRuntimeTest")

    override fun gc(workspace: WorkspaceContext): GcResult = TODO("unused in AppRuntimeTest")

    override fun refresh(workspace: WorkspaceContext): InspectResult =
        InspectResult(
            target = workspace.activeTarget,
            snapshot = workspace.snapshot,
            classCount = null,
        )

    override fun inspect(workspace: WorkspaceContext): InspectResult = TODO("unused in AppRuntimeTest")
}

private class CloseTrackingDexAnalysisService : DexAnalysisService, AutoCloseable {
    val releasedDexContexts = mutableListOf<WorkspaceContext>()
    var closed = false

    override fun releaseDexContext(workspace: WorkspaceContext) {
        releasedDexContexts += workspace
    }

    override fun findClasses(workspace: WorkspaceContext, request: FindClassesRequest): List<ClassHit> = emptyList()

    override fun findMethods(workspace: WorkspaceContext, request: FindMethodsRequest): List<MethodHit> = emptyList()

    override fun findFields(workspace: WorkspaceContext, request: FindFieldsRequest): List<FieldHit> = emptyList()

    override fun findClassesUsingStrings(
        workspace: WorkspaceContext,
        request: FindClassesUsingStringsRequest,
    ): List<ClassHit> = emptyList()

    override fun findMethodsUsingStrings(
        workspace: WorkspaceContext,
        request: FindMethodsUsingStringsRequest,
    ): List<MethodHit> = emptyList()

    override fun inspectMethod(workspace: WorkspaceContext, request: InspectMethodRequest): MethodDetail =
        MethodDetail(MethodHit("Lsample/Test;", "foo", "Lsample/Test;->foo()V"))

    override fun exportClassDex(workspace: WorkspaceContext, request: ExportClassDexRequest): ExportResult =
        ExportResult(request.outputPath)

    override fun exportClassSmali(workspace: WorkspaceContext, request: ExportClassSmaliRequest): ExportResult =
        ExportResult(request.outputPath)

    override fun exportClassJava(workspace: WorkspaceContext, request: ExportClassJavaRequest): ExportResult =
        ExportResult(request.outputPath)

    override fun exportMethodSmali(workspace: WorkspaceContext, request: ExportMethodSmaliRequest): ExportResult =
        ExportResult(request.outputPath)

    override fun exportMethodDex(workspace: WorkspaceContext, request: ExportMethodDexRequest): ExportResult =
        ExportResult(request.outputPath)

    override fun exportMethodJava(workspace: WorkspaceContext, request: ExportMethodJavaRequest): ExportResult =
        ExportResult(request.outputPath)

    override fun close() {
        closed = true
    }
}

private class RuntimeTestResourceService : ResourceService {
    override fun decodeManifest(workspace: WorkspaceContext): ManifestResult = TODO("unused in AppRuntimeTest")

    override fun inspectManifest(
        workspace: WorkspaceContext,
        request: io.github.dexclub.core.api.resource.InspectManifestRequest,
    ): ManifestInspectionResult = TODO("unused in AppRuntimeTest")

    override fun dumpResourceTable(workspace: WorkspaceContext): ResourceTableResult =
        TODO("unused in AppRuntimeTest")

    override fun decodeXml(workspace: WorkspaceContext, request: DecodeXmlRequest): DecodedXmlResult =
        TODO("unused in AppRuntimeTest")

    override fun listResourceEntries(workspace: WorkspaceContext): List<ResourceEntry> =
        TODO("unused in AppRuntimeTest")

    override fun getResourceValue(workspace: WorkspaceContext, request: ResolveResourceRequest): ResourceValue =
        TODO("unused in AppRuntimeTest")

    override fun findResourceValues(
        workspace: WorkspaceContext,
        request: FindResourcesRequest,
    ): List<ResourceEntryValueHit> = TODO("unused in AppRuntimeTest")
}
