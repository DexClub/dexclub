package io.github.dexclub.mcp

import kotlinx.serialization.json.buildJsonObject
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
            app.errorResult(app.staleSessionMessage("dead-session")).content,
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
}
