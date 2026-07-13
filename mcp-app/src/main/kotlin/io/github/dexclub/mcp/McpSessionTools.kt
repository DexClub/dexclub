package io.github.dexclub.mcp

import io.github.dexclub.core.api.workspace.WorkspaceInitError
import io.github.dexclub.core.api.workspace.WorkspaceInitErrorReason
import io.github.dexclub.core.app.session.TargetSession
import io.modelcontextprotocol.kotlin.sdk.server.Server

internal fun McpApp.registerSessionTools(server: Server) {
    registerCatalogTool(server, McpSessionToolCatalog.require("open_target_session")) { request ->
        val input = request.optionalStringArgument("input")
            ?: return@registerCatalogTool missingRequiredArgumentsResult("input")

        val targetSession = try {
            openTargetSession(input)
        } catch (cause: WorkspaceInitError) {
            return@registerCatalogTool errorResult(workspaceInitErrorMessage(cause))
        }
        successResult(OpenTargetSessionResult.serializer(), targetSession.toResult())
    }

    registerCatalogTool(server, McpSessionToolCatalog.require("list_target_sessions")) {
        val sessions = listTargetSessions()
        successResult(
            ListTargetSessionsResult.serializer(),
            ListTargetSessionsResult(
                total = sessions.size,
                items = sessions.map { it.toView() },
            ),
        )
    }

    registerCatalogTool(server, McpSessionToolCatalog.require("get_target_session")) { request ->
        val sessionId = request.optionalStringArgument("session_id")
            ?: return@registerCatalogTool missingRequiredArgumentsResult("session_id")
        val session = getTargetSession(sessionId)
            ?: return@registerCatalogTool missingSessionResult(sessionId)
        successResult(TargetSessionView.serializer(), session.toView())
    }

    registerCatalogTool(server, McpSessionToolCatalog.require("close_target_session")) { request ->
        val sessionId = request.optionalStringArgument("session_id")
            ?: return@registerCatalogTool missingRequiredArgumentsResult("session_id")
        val session = closeTargetSession(sessionId)
        successResult(
            CloseTargetSessionResult.serializer(),
            CloseTargetSessionResult(
                closed = session != null,
                session = session?.toView(),
            ),
        )
    }

    registerCatalogTool(server, McpSessionToolCatalog.require("refresh_target_session")) { request ->
        val sessionId = request.optionalStringArgument("session_id")
            ?: return@registerCatalogTool missingRequiredArgumentsResult("session_id")
        val session = refreshTargetSession(sessionId)
            ?: return@registerCatalogTool missingSessionResult(sessionId)
        successResult(
            RefreshTargetSessionResult.serializer(),
            RefreshTargetSessionResult(
                session = session.toView(),
                handlesCleared = true,
            ),
        )
    }

    registerCatalogTool(server, McpSessionToolCatalog.require("diagnose_target_sessions")) {
        val snapshot = diagnoseTargetSessions()
        successResult(DiagnoseTargetSessionsResult.serializer(), snapshot.toView())
    }
}

private fun workspaceInitErrorMessage(error: WorkspaceInitError): String =
    when (error.reason) {
        WorkspaceInitErrorReason.MissingInput ->
            "${error.message}. open_target_session.input must be an absolute path to an existing target file. Relative paths are resolved against the MCP process working directory."

        WorkspaceInitErrorReason.InvalidInputPath ->
            "${error.message}. open_target_session.input must stay within the target workdir after normalization."

        else -> error.message ?: "Failed to initialize target session"
    }
