package io.github.dexclub.core.impl.dex

import io.github.dexclub.core.api.dex.DexInspectError
import io.github.dexclub.core.api.dex.DexInspectErrorReason
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.shared.createDefaultServices
import io.github.dexclub.core.api.workspace.WorkspaceRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultDexAnalysisInspectTest {
    @Test
    fun inspectMethodCanBeNarrowedBySourcePathOnAmbiguousApk() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.ambiguousApkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.ambiguousWorkspaceDir.absolutePath))

        val detail = services.dex.inspectMethod(
            workspace = workspace,
            request = io.github.dexclub.core.api.dex.InspectMethodRequest(
                descriptor = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                source = SourceLocator(sourcePath = "fixture.apk", sourceEntry = "classes2.dex"),
            ),
        )

        assertEquals("fixture.apk", detail.method.sourcePath)
        assertEquals("classes2.dex", detail.method.sourceEntry)
        assertEquals("Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;", detail.method.descriptor)
    }

    @Test
    fun inspectMethodRequiresUniqueDescriptorOnAmbiguousApk() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.ambiguousApkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.ambiguousWorkspaceDir.absolutePath))

        val error = assertFailsWith<DexInspectError> {
            services.dex.inspectMethod(
                workspace = workspace,
                request = io.github.dexclub.core.api.dex.InspectMethodRequest(
                    descriptor = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                ),
            )
        }

        assertEquals(DexInspectErrorReason.AmbiguousMethod, error.reason)
        assertTrue(error.message.orEmpty().contains("requires a unique descriptor within the workspace"))
    }

    @Test
    fun inspectMethodMatchesArrayParameterDescriptor() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))

        val detail = services.dex.inspectMethod(
            workspace = workspace,
            request = io.github.dexclub.core.api.dex.InspectMethodRequest(
                descriptor = "Lfixture/samples/SampleSearchTarget;->acceptObjects([Ljava/lang/Object;)V",
            ),
        )

        assertEquals("fixture.samples.SampleSearchTarget", detail.method.className)
        assertEquals("acceptObjects", detail.method.methodName)
        assertEquals("Lfixture/samples/SampleSearchTarget;->acceptObjects([Ljava/lang/Object;)V", detail.method.descriptor)
        assertEquals("fixture.dex", detail.method.sourcePath)
    }
}
