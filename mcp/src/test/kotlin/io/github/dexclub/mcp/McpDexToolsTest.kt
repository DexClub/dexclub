package io.github.dexclub.mcp

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.FindClassesUsingStringsRequest
import io.github.dexclub.core.api.dex.FindMethodsUsingStringsRequest
import io.github.dexclub.core.api.dex.MethodDetail
import io.github.dexclub.core.api.dex.MethodDetailSection
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.shared.MethodSmaliMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class McpDexToolsTest {
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
        val app = createTestApp(
            workspace = workspace,
            workspaceService = workspaceService,
            dexService = dexService,
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
        val app = createTestApp(workspace = workspace, dexService = dexService)

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
        val app = createTestApp(workspace = workspace, dexService = dexService)
        val session = app.openTargetSession("sample.apk")

        val detail = app.inspectMethod(
            workspace = session.workspace,
            descriptor = "Lsample/Test;->foo()V",
            source = SourceLocator(sourcePath = "sample.apk", sourceEntry = "classes.dex"),
            includes = setOf(MethodDetailSection.Strings, MethodDetailSection.Annotations),
        )

        assertEquals(workspace, dexService.lastWorkspace)
        assertEquals("Lsample/Test;->foo()V", dexService.lastInspectRequest?.descriptor)
        assertEquals("sample.apk", dexService.lastInspectRequest?.source?.sourcePath)
        assertEquals("classes.dex", dexService.lastInspectRequest?.source?.sourceEntry)
        assertEquals(setOf(MethodDetailSection.Strings, MethodDetailSection.Annotations), dexService.lastInspectRequest?.includes)
        assertEquals(listOf("alpha"), detail.strings)
        assertEquals(listOf("Lsample/Anno;"), detail.annotations)
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
        val app = createTestApp(workspace = workspace, dexService = dexService)
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
        val app = createTestApp(workspace = workspace, dexService = dexService)
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
        val app = createTestApp(workspace = workspace, dexService = dexService)
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
        val app = createTestApp(workspace = workspace, dexService = dexService)
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
        val app = createTestApp(workspace = workspace, dexService = dexService)
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
    fun exportMethodSmaliTextExplainsSupportedModes() {
        val app = createTestApp()
        val session = app.openTargetSession("sample.apk")

        val error = kotlin.test.assertFailsWith<IllegalArgumentException> {
            app.exportMethodSmaliText(
                workspace = session.workspace,
                descriptor = "Lsample/Test;->foo()V",
                mode = "full",
            )
        }

        assertEquals("Unsupported smali mode: full. Supported modes: snippet, class", error.message)
    }

    @Test
    fun exportClassJavaTextUsesSessionWorkspaceAndReturnsFileContent() {
        val workspace = fakeWorkspaceContext()
        val dexService = FakeDexAnalysisService()
        val app = createTestApp(workspace = workspace, dexService = dexService)
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
        val app = createTestApp(workspace = workspace, dexService = dexService)
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
}
