package io.github.dexclub.mcp

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.MethodDetail
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.app.dex.FindClassesUsingStringsUseCaseResult
import io.github.dexclub.core.app.dex.FindMethodsUseCaseResult
import io.github.dexclub.core.app.dex.InspectMethodUseCaseResult
import io.github.dexclub.core.app.session.TargetSessionService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class McpModelsTest {
    @Test
    fun inspectMethodBriefReturnsCountsWithoutExpandedSections() {
        val session = TargetSessionService().openTargetSession(fakeWorkspaceContext())
        val result = InspectMethodUseCaseResult(
            session = session,
            workspace = session.workspace,
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
        ).toInspectMethodResult(brief = true)

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
    fun findMethodsResultSupportsFieldProjectionAndBrief() {
        val session = TargetSessionService().openTargetSession(fakeWorkspaceContext())
        val result = FindMethodsUseCaseResult(
            session = session,
            workspace = session.workspace,
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
        ).toFindMethodsResult(
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
    fun findMethodsResultCanProjectMethodHandle() {
        val session = TargetSessionService().openTargetSession(fakeWorkspaceContext())
        val result = FindMethodsUseCaseResult(
            session = session,
            workspace = session.workspace,
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
        ).toFindMethodsResult(
            handleProvider = { "method-handle-1" },
            fields = setOf("methodHandle"),
        )

        assertEquals("method-handle-1", result.items.single()["methodHandle"]!!.jsonPrimitive.content)
    }

    @Test
    fun findMethodsResultDefaultsToMethodHandleInBriefSessionMode() {
        val session = TargetSessionService().openTargetSession(fakeWorkspaceContext())
        val result = FindMethodsUseCaseResult(
            session = session,
            workspace = session.workspace,
            total = 1,
            offset = 0,
            limit = 1,
            hasMore = false,
            items = listOf(
                MethodHit(
                    className = "fixture.samples.SampleSearchTarget",
                    methodName = "exposeNeedle",
                    descriptor = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                    sourcePath = "fixture.dex",
                    sourceEntry = "classes.dex",
                ),
            ),
        ).toFindMethodsResult(
            handleProvider = { "method-handle-1" },
            fields = null,
            brief = true,
        )

        val item = result.items.single()
        assertEquals(
            setOf("descriptor", "sourcePath", "sourceEntry", "methodHandle"),
            item.keys,
        )
        assertEquals("method-handle-1", item["methodHandle"]!!.jsonPrimitive.content)
    }

    @Test
    fun findClassesUsingStringsResultCanProjectClassHandle() {
        val session = TargetSessionService().openTargetSession(fakeWorkspaceContext())
        val result = FindClassesUsingStringsUseCaseResult(
            session = session,
            workspace = session.workspace,
            total = 1,
            offset = 0,
            limit = 1,
            hasMore = false,
            items = listOf(
                ClassHit(
                    className = "Lfixture/samples/SampleSearchTarget;",
                ),
            ),
        ).toFindClassesUsingStringsResult(
            handleProvider = { "class-handle-1" },
            fields = setOf("classHandle"),
        )

        assertEquals("class-handle-1", result.items.single()["classHandle"]!!.jsonPrimitive.content)
    }

    @Test
    fun parseRequestedFieldsRejectsGuessedMethodAliases() {
        val error = kotlin.test.assertFailsWith<IllegalArgumentException> {
            parseRequestedFields(
                rawValues = listOf("handle", "descriptor", "name"),
                supported = methodFieldNamesWithHandle,
            )
        }

        assertEquals("Unsupported fields: handle,name", error.message)
    }

    @Test
    fun parseRequestedFieldsExplainsThatHandlesNeedSession() {
        val error = kotlin.test.assertFailsWith<IllegalArgumentException> {
            parseRequestedFields(
                rawValues = listOf("descriptor", "methodHandle"),
                supported = methodFieldNames,
                sessionRequiredFields = setOf("methodHandle"),
                hasSession = false,
            )
        }

        assertEquals(
            "Fields require session_id: methodHandle. Open a target session first, or omit those fields when using workdir",
            error.message,
        )
    }

    @Test
    fun parseRequestedFieldsRejectsUnsupportedResourceFields() {
        val error = kotlin.test.assertFailsWith<IllegalArgumentException> {
            parseRequestedFields(
                rawValues = listOf("resourceId", "filePath"),
                supported = resourceValueFieldNames,
            )
        }

        assertEquals("Unsupported fields: filePath", error.message)
    }

    @Test
    fun diagnoseTargetSessionsJsonKeepsStableTopLevelFields() {
        val store = TargetSessionService()
        store.openTargetSession(fakeWorkspaceContext())

        val encoded = Json.encodeToJsonElement(
            DiagnoseTargetSessionsResult.serializer(),
            store.snapshot().toView(),
        ).jsonObject

        assertEquals(
            setOf(
                "now",
                "idleTimeoutSeconds",
                "maxSessions",
                "maxHandlesPerSession",
                "sessionCount",
                "methodHandleCount",
                "classHandleCount",
                "sessions",
            ),
            encoded.keys,
        )
    }

    @Test
    fun errorResultEscapesMessagesCarryingUserInput() {
        val app = createTestApp()

        // Regression coverage: error messages may carry raw user input such as inspect_method include values.
        // If concatenated into JSON directly they could inject structure. Use the real
        // parseMethodDetailSections error message, including quotes and braces, to verify that the
        // output remains valid JSON and that the error field preserves the original message.
        val rawMessage = kotlin.test.assertFailsWith<IllegalArgumentException> {
            parseMethodDetailSections(listOf("\"},\"injected\":\"x"))
        }.message.orEmpty()
        assertTrue(rawMessage.contains("Unsupported include section"), "precondition: $rawMessage")

        val result = app.errorResult(rawMessage)

        assertEquals(true, result.isError)
        val text = (result.content.single() as io.modelcontextprotocol.kotlin.sdk.types.TextContent).text
        val parsed = Json.parseToJsonElement(text.orEmpty()).jsonObject
        assertEquals(rawMessage, parsed["error"]!!.jsonPrimitive.content)
        assertNull(parsed["injected"])
    }
}
