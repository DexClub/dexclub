package io.github.dexclub.core.impl.dex

import io.github.dexclub.core.api.dex.FindClassesRequest
import io.github.dexclub.core.api.dex.FindClassesUsingStringsRequest
import io.github.dexclub.core.api.dex.FindFieldsRequest
import io.github.dexclub.core.api.dex.FindMethodsRequest
import io.github.dexclub.core.api.dex.FindMethodsUsingStringsRequest
import io.github.dexclub.core.api.shared.CapabilityError
import io.github.dexclub.core.api.shared.Operation
import io.github.dexclub.core.api.shared.PageWindow
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.shared.createDefaultServices
import io.github.dexclub.core.api.workspace.WorkspaceRef
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultDexAnalysisSearchTest {
    @Test
    fun findClassSortsBeforeApplyingWindow() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))

        val hits = services.dex.findClasses(
            workspace = workspace,
            request = FindClassesRequest(
                queryText = QUERY_SEARCH_TARGETS,
                window = PageWindow(offset = 1, limit = 1),
            ),
        )

        assertEquals(1, hits.size)
        assertEquals("Lfixture/samples/SampleSearchTarget;", hits.single().className)
        assertEquals("fixture.dex", hits.single().sourcePath)
        assertEquals(null, hits.single().sourceEntry)
    }

    @Test
    fun findClassOnApkReportsSourceEntry() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.apkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.apkWorkspaceDir.absolutePath))

        val hits = services.dex.findClasses(
            workspace = workspace,
            request = FindClassesRequest(queryText = QUERY_SAMPLE_CLASS),
        )

        assertTrue(hits.any { it.sourcePath == "fixture.apk" && it.sourceEntry == "classes.dex" })
    }

    @Test
    fun findMethodSortsBeforeApplyingWindow() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))

        val hits = services.dex.findMethods(
            workspace = workspace,
            request = FindMethodsRequest(
                queryText = QUERY_EXPOSE_METHOD,
                window = PageWindow(offset = 1, limit = 1),
            ),
        )

        assertEquals(1, hits.size)
        assertEquals("fixture.samples.SampleSearchTarget", hits.single().className)
        assertEquals("exposeNeedle", hits.single().methodName)
        assertEquals("fixture.dex", hits.single().sourcePath)
    }

    @Test
    fun findMethodOnApkReportsSourceEntry() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.apkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.apkWorkspaceDir.absolutePath))

        val hits = services.dex.findMethods(
            workspace = workspace,
            request = FindMethodsRequest(queryText = QUERY_EXPOSE_METHOD),
        )

        assertTrue(
            hits.any {
                it.methodName == "exposeNeedle" &&
                    it.sourcePath == "fixture.apk" &&
                    it.sourceEntry == "classes.dex"
            },
        )
        assertApkDexCacheContainsOnly(fixture.apkWorkspaceDir, workspace, "classes.dex")
    }

    @Test
    fun findFieldSortsBeforeApplyingWindow() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))

        val hits = services.dex.findFields(
            workspace = workspace,
            request = FindFieldsRequest(
                queryText = QUERY_NEEDLE_FIELD,
                window = PageWindow(offset = 1, limit = 1),
            ),
        )

        assertEquals(1, hits.size)
        assertEquals("fixture.samples.SampleSearchTarget", hits.single().className)
        assertEquals("NEEDLE", hits.single().fieldName)
        assertEquals("fixture.dex", hits.single().sourcePath)
    }

    @Test
    fun findFieldOnApkReportsSourceEntry() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.apkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.apkWorkspaceDir.absolutePath))

        val hits = services.dex.findFields(
            workspace = workspace,
            request = FindFieldsRequest(queryText = QUERY_NEEDLE_FIELD),
        )

        assertTrue(
            hits.any {
                it.fieldName == "NEEDLE" &&
                    it.sourcePath == "fixture.apk" &&
                    it.sourceEntry == "classes.dex"
            },
        )
        assertApkDexCacheContainsOnly(fixture.apkWorkspaceDir, workspace, "classes.dex")
    }

    @Test
    fun findClassUsingStringsDeduplicatesAcrossGroupsBeforeWindow() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))

        val hits = services.dex.findClassesUsingStrings(
            workspace = workspace,
            request = FindClassesUsingStringsRequest(
                queryText = QUERY_CLASS_USING_STRINGS_DUP_GROUPS,
                window = PageWindow(offset = 1, limit = 1),
            ),
        )

        assertEquals(1, hits.size)
        assertEquals("Lfixture/samples/SampleSearchTarget;", hits.single().className)
        assertEquals("fixture.dex", hits.single().sourcePath)
    }

    @Test
    fun findClassUsingStringsOnApkReportsSourceEntry() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.apkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.apkWorkspaceDir.absolutePath))

        val hits = services.dex.findClassesUsingStrings(
            workspace = workspace,
            request = FindClassesUsingStringsRequest(queryText = QUERY_CLASS_USING_STRINGS_SINGLE_GROUP),
        )

        assertTrue(hits.any { it.sourcePath == "fixture.apk" && it.sourceEntry == "classes.dex" })
        assertApkDexCacheContainsOnly(fixture.apkWorkspaceDir, workspace, "classes.dex")
    }

    @Test
    fun findMethodUsingStringsDeduplicatesAcrossGroupsBeforeWindow() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))

        val hits = services.dex.findMethodsUsingStrings(
            workspace = workspace,
            request = FindMethodsUsingStringsRequest(
                queryText = QUERY_METHOD_USING_STRINGS_DUP_GROUPS,
                window = PageWindow(offset = 1, limit = 1),
            ),
        )

        assertEquals(1, hits.size)
        assertEquals("fixture.samples.SampleSearchTarget", hits.single().className)
        assertEquals("exposeNeedle", hits.single().methodName)
        assertEquals("fixture.dex", hits.single().sourcePath)
    }

    @Test
    fun findMethodUsingStringsOnApkReportsSourceEntry() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.apkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.apkWorkspaceDir.absolutePath))

        val hits = services.dex.findMethodsUsingStrings(
            workspace = workspace,
            request = FindMethodsUsingStringsRequest(queryText = QUERY_METHOD_USING_STRINGS_SINGLE_GROUP),
        )

        assertTrue(
            hits.any {
                it.methodName == "exposeNeedle" &&
                    it.sourcePath == "fixture.apk" &&
                    it.sourceEntry == "classes.dex"
            },
        )
        assertApkDexCacheContainsOnly(fixture.apkWorkspaceDir, workspace, "classes.dex")
    }

    @Test
    fun dexEntrySortKeySortsPrimaryThenNumericThenOtherDexEntries() {
        val names = listOf("classes10.dex", "classes.dex", "classes2.dex", "classesx.dex")
        val sorted = names.sortedWith(compareBy({ dexEntrySortKey(it).first }, { dexEntrySortKey(it).second }, { it }))

        assertContentEquals(
            listOf("classes.dex", "classes2.dex", "classes10.dex", "classesx.dex"),
            sorted,
        )
    }

    @Test
    fun sourceLocatorDescribeUsesStableCliStyleKeys() {
        assertEquals(
            "source_path=fixture.apk, source_entry=classes2.dex",
            SourceLocator(sourcePath = "fixture.apk", sourceEntry = "classes2.dex").describe(),
        )
        assertEquals(
            "source_path=fixture.dex",
            SourceLocator(sourcePath = "fixture.dex").describe(),
        )
        assertEquals(
            "source_entry=classes.dex",
            SourceLocator(sourceEntry = "classes.dex").describe(),
        )
        assertEquals(null, SourceLocator().describe())
    }

    @Test
    fun findClassRequiresDexCapability() {
        val workdir = Files.createTempDirectory("dexclub-core-manifest").toFile()
        val manifest = workdir.toPath().resolve("AndroidManifest.xml")
        manifest.writeText("<manifest package=\"fixture.samples\"/>")
        val services = createDefaultServices()
        services.workspace.initialize(manifest.toString())
        val workspace = services.workspace.open(WorkspaceRef(workdir.absolutePath))

        val error = assertFailsWith<CapabilityError> {
            services.dex.findClasses(
                workspace = workspace,
                request = FindClassesRequest(queryText = QUERY_SAMPLE_CLASS),
            )
        }

        assertEquals(Operation.FindClass, error.operation)
        assertEquals("findClass", error.requiredCapability)
        assertEquals("manifest", error.kind)
    }
}
