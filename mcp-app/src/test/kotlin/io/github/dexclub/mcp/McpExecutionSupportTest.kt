package io.github.dexclub.mcp

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class McpExecutionSupportTest {
    @Test
    fun sessionLeaseStaysOwnedByRuntimeUntilSessionCloses() {
        val workspace = fakeWorkspaceContext()
        val dexService = FakeDexAnalysisService()
        val app = createTestApp(workspace = workspace, dexService = dexService)
        val session = app.openTargetSession("sample.apk")

        val lease = app.acquireToolContextLease(
            callToolRequest(
                "inspect_method",
                buildJsonObject {
                    put("session_id", session.sessionId)
                },
            ),
        )

        assertEquals(emptyList(), dexService.releasedDexContexts)

        lease!!.close()
        assertEquals(emptyList(), dexService.releasedDexContexts)

        app.closeTargetSession(session.sessionId)
        assertEquals(listOf(workspace), dexService.releasedDexContexts)
    }

    @Test
    fun workdirLeaseReleasesDexContextWhenToolLeaseCloses() {
        val workspace = fakeWorkspaceContext()
        val dexService = FakeDexAnalysisService()
        val app = createTestApp(workspace = workspace, dexService = dexService)

        val lease = app.acquireToolContextLease(
            callToolRequest(
                "find_methods",
                buildJsonObject {
                    put("workdir", workspace.workdir)
                },
            ),
        )

        assertEquals(emptyList(), dexService.releasedDexContexts)

        lease!!.close()
        assertEquals(listOf(workspace), dexService.releasedDexContexts)
    }

    @Test
    fun missingSessionTurnsIntoFailureResultInsteadOfThrowing() {
        val app = createTestApp()

        val result = app.executionContextOrFailureResult(
            callToolRequest(
                "inspect_method",
                buildJsonObject {
                    put("session_id", "dead-session")
                },
            ),
        )

        val failed = assertIs<ExecutionContextResolution.Failed>(result)
        assertEquals(true, failed.result.isError)
        assertEquals(
            app.missingSessionResult("dead-session").content,
            failed.result.content,
        )
    }

    @Test
    fun missingSessionAndWorkdirTurnsIntoFailureResultInsteadOfThrowing() {
        val app = createTestApp()

        val result = app.executionContextOrFailureResult(
            callToolRequest(
                "inspect_method",
                buildJsonObject {},
            ),
        )

        val failed = assertIs<ExecutionContextResolution.Failed>(result)
        assertEquals(
            app.missingSessionOrWorkdirResult().content,
            failed.result.content,
        )
    }

    @Test
    fun unexpectedExecutionContextFailureTurnsIntoStructuredInternalError() {
        val workspace = fakeWorkspaceContext()
        val workspaceService = object : io.github.dexclub.core.api.workspace.WorkspaceService {
            override fun initialize(input: String) = workspace

            override fun switchTarget(ref: io.github.dexclub.core.api.workspace.WorkspaceRef, input: String) = ref

            override fun open(ref: io.github.dexclub.core.api.workspace.WorkspaceRef): io.github.dexclub.core.api.workspace.WorkspaceContext {
                throw IllegalStateException("boom")
            }

            override fun listTargets(ref: io.github.dexclub.core.api.workspace.WorkspaceRef) = emptyList<io.github.dexclub.core.api.workspace.TargetSummary>()

            override fun loadStatus(ref: io.github.dexclub.core.api.workspace.WorkspaceRef): io.github.dexclub.core.api.workspace.WorkspaceStatus {
                error("unused")
            }

            override fun gc(workspace: io.github.dexclub.core.api.workspace.WorkspaceContext): io.github.dexclub.core.api.workspace.GcResult {
                error("unused")
            }

            override fun refresh(workspace: io.github.dexclub.core.api.workspace.WorkspaceContext): io.github.dexclub.core.api.workspace.InspectResult {
                error("unused")
            }

            override fun inspect(workspace: io.github.dexclub.core.api.workspace.WorkspaceContext): io.github.dexclub.core.api.workspace.InspectResult {
                error("unused")
            }
        }
        val app = createTestApp(workspace = workspace, workspaceService = workspaceService)

        val result = app.executionContextOrFailureResult(
            callToolRequest(
                "find_methods",
                buildJsonObject {
                    put("workdir", workspace.workdir)
                },
            ),
        )

        val failed = assertIs<ExecutionContextResolution.Failed>(result)
        val payload = Json.parseToJsonElement((failed.result.content.single() as io.modelcontextprotocol.kotlin.sdk.types.TextContent).text.orEmpty()).jsonObject
        assertEquals("internal_error", payload["error"]!!.jsonObject["code"]!!.jsonPrimitive.content)
        assertEquals("boom", payload["error"]!!.jsonObject["message"]!!.jsonPrimitive.content)
    }
}
