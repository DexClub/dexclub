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
import io.github.dexclub.core.api.resource.DecodedXmlResult
import io.github.dexclub.core.api.resource.DecodeXmlRequest
import io.github.dexclub.core.api.resource.FindResourcesRequest
import io.github.dexclub.core.api.resource.ManifestInspectionResult
import io.github.dexclub.core.api.resource.ManifestResult
import io.github.dexclub.core.api.resource.ResourceEntry
import io.github.dexclub.core.api.resource.ResourceEntryValueHit
import io.github.dexclub.core.api.resource.ResourceService
import io.github.dexclub.core.api.resource.ResourceTableResult
import io.github.dexclub.core.api.resource.ResolveResourceRequest
import io.github.dexclub.core.api.resource.ResourceValue
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
import io.github.dexclub.core.app.dex.FindMethodsUseCaseRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppUseCasesTest {
    @Test
    fun reusesSharedSessionServiceAcrossSessionAwareUseCases() {
        val workspace = fakeWorkspaceContext()
        val appUseCases = AppUseCases(
            services = Services(
                workspace = FakeWorkspaceService(workspace),
                dex = FakeDexAnalysisService(),
                resource = FakeResourceService(),
            ),
        )

        val session = appUseCases.sessionService.openTargetSession("sample.apk")
        val result = appUseCases.dex.findMethodsUseCase.execute(
            FindMethodsUseCaseRequest(
                sessionId = session.sessionId,
                methodNameContains = "main",
            ),
        )

        assertEquals(session.sessionId, result.session?.sessionId)
        assertEquals(workspace, result.session?.workspace)
        assertEquals(workspace, result.workspace)
        assertEquals(0, result.total)
        assertEquals(emptyList(), result.items)
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

private class FakeWorkspaceService(
    private val workspace: WorkspaceContext,
) : WorkspaceService {
    override fun initialize(input: String): WorkspaceContext = workspace

    override fun switchTarget(ref: WorkspaceRef, input: String): WorkspaceRef = TODO("unused in AppUseCasesTest")

    override fun open(ref: WorkspaceRef): WorkspaceContext = TODO("unused in AppUseCasesTest")

    override fun listTargets(ref: WorkspaceRef): List<TargetSummary> = TODO("unused in AppUseCasesTest")

    override fun loadStatus(ref: WorkspaceRef): WorkspaceStatus = TODO("unused in AppUseCasesTest")

    override fun gc(workspace: WorkspaceContext): GcResult = TODO("unused in AppUseCasesTest")

    override fun refresh(workspace: WorkspaceContext): InspectResult = TODO("unused in AppUseCasesTest")

    override fun inspect(workspace: WorkspaceContext): InspectResult = TODO("unused in AppUseCasesTest")
}

private class FakeDexAnalysisService : DexAnalysisService {
    override fun findClasses(workspace: WorkspaceContext, request: FindClassesRequest): List<ClassHit> =
        TODO("unused in AppUseCasesTest")

    override fun findMethods(workspace: WorkspaceContext, request: FindMethodsRequest): List<MethodHit> =
        emptyList()

    override fun findFields(workspace: WorkspaceContext, request: FindFieldsRequest): List<FieldHit> =
        TODO("unused in AppUseCasesTest")

    override fun findClassesUsingStrings(
        workspace: WorkspaceContext,
        request: FindClassesUsingStringsRequest,
    ): List<ClassHit> = TODO("unused in AppUseCasesTest")

    override fun findMethodsUsingStrings(
        workspace: WorkspaceContext,
        request: FindMethodsUsingStringsRequest,
    ): List<MethodHit> = TODO("unused in AppUseCasesTest")

    override fun inspectMethod(workspace: WorkspaceContext, request: InspectMethodRequest): MethodDetail =
        TODO("unused in AppUseCasesTest")

    override fun exportClassDex(workspace: WorkspaceContext, request: ExportClassDexRequest): ExportResult =
        TODO("unused in AppUseCasesTest")

    override fun exportClassSmali(workspace: WorkspaceContext, request: ExportClassSmaliRequest): ExportResult =
        TODO("unused in AppUseCasesTest")

    override fun exportClassJava(workspace: WorkspaceContext, request: ExportClassJavaRequest): ExportResult =
        TODO("unused in AppUseCasesTest")

    override fun exportMethodSmali(workspace: WorkspaceContext, request: ExportMethodSmaliRequest): ExportResult =
        TODO("unused in AppUseCasesTest")

    override fun exportMethodDex(workspace: WorkspaceContext, request: ExportMethodDexRequest): ExportResult =
        TODO("unused in AppUseCasesTest")

    override fun exportMethodJava(workspace: WorkspaceContext, request: ExportMethodJavaRequest): ExportResult =
        TODO("unused in AppUseCasesTest")
}

private class FakeResourceService : ResourceService {
    override fun decodeManifest(workspace: WorkspaceContext): ManifestResult = TODO("unused in AppUseCasesTest")

    override fun inspectManifest(
        workspace: WorkspaceContext,
        request: io.github.dexclub.core.api.resource.InspectManifestRequest,
    ): ManifestInspectionResult = TODO("unused in AppUseCasesTest")

    override fun dumpResourceTable(workspace: WorkspaceContext): ResourceTableResult =
        TODO("unused in AppUseCasesTest")

    override fun decodeXml(workspace: WorkspaceContext, request: DecodeXmlRequest): DecodedXmlResult =
        TODO("unused in AppUseCasesTest")

    override fun listResourceEntries(workspace: WorkspaceContext): List<ResourceEntry> =
        TODO("unused in AppUseCasesTest")

    override fun getResourceValue(workspace: WorkspaceContext, request: ResolveResourceRequest): ResourceValue =
        TODO("unused in AppUseCasesTest")

    override fun findResourceValues(
        workspace: WorkspaceContext,
        request: FindResourcesRequest,
    ): List<ResourceEntryValueHit> = TODO("unused in AppUseCasesTest")
}
