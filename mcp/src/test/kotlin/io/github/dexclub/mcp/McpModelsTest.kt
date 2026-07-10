package io.github.dexclub.mcp

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.MethodDetail
import io.github.dexclub.core.api.dex.MethodHit
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
    fun findMethodsResultDefaultsToMethodHandleInBriefSessionMode() {
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
                        sourcePath = "fixture.dex",
                        sourceEntry = "classes.dex",
                    ),
                ),
            ),
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
    fun errorResultEscapesMessagesCarryingUserInput() {
        val app = createTestApp()

        // 回归：错误消息可能携带原始用户输入（如 inspect_method 的 include 值），
        // 若直接拼进 JSON 字符串会造成结构注入。这里用真实来自 parseMethodDetailSections
        // 的错误消息（含引号/花括号）验证输出仍是合法 JSON，且 error 字段与原始消息一致。
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
