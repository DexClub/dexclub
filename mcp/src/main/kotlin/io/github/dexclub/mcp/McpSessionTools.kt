package io.github.dexclub.mcp

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal fun McpApp.registerSessionTools(server: Server) {
    server.addLoggedTool(
        name = "open_target_session",
        description = "初始化目标输入并创建可复用的 DexClub target session。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("input", stringSchema())
            },
            required = listOf("input"),
        ),
    ) { request ->
        val input = request.arguments?.get("input")?.jsonPrimitive?.content?.trim().orEmpty()
        if (input.isEmpty()) {
            return@addLoggedTool errorResult("input is required")
        }

        val targetSession = openTargetSession(input)
        CallToolResult(
            content = listOf(
                TextContent(json.encodeToString(OpenTargetSessionResult.serializer(), targetSession.toResult())),
            ),
        )
    }

    server.addLoggedTool(
        name = "list_target_sessions",
        description = "列出当前 MCP 进程中已打开的 target session，用于在同一 server 内切换不同工作区或目标。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {},
        ),
    ) {
        val sessions = listTargetSessions()
        CallToolResult(
            content = listOf(
                TextContent(
                    json.encodeToString(
                        ListTargetSessionsResult.serializer(),
                        ListTargetSessionsResult(
                            total = sessions.size,
                            items = sessions.map { it.toView() },
                        ),
                    ),
                ),
            ),
        )
    }

    server.addLoggedTool(
        name = "get_target_session",
        description = "读取单个 target session 的当前绑定工作区与 active target 信息。",
        inputSchema = sessionIdOnlySchema,
    ) { request ->
        val sessionId = request.requiredStringArgument("session_id")
        if (sessionId.isEmpty()) {
            return@addLoggedTool errorResult("session_id is required")
        }
        val session = getTargetSession(sessionId)
            ?: return@addLoggedTool missingSessionResult(sessionId)
        CallToolResult(
            content = listOf(
                TextContent(
                    json.encodeToString(
                        TargetSessionView.serializer(),
                        session.toView(),
                    ),
                ),
            ),
        )
    }

    server.addLoggedTool(
        name = "close_target_session",
        description = "关闭一个 target session，并清理该 session 下的 method_handle / class_handle。",
        inputSchema = sessionIdOnlySchema,
    ) { request ->
        val sessionId = request.requiredStringArgument("session_id")
        if (sessionId.isEmpty()) {
            return@addLoggedTool errorResult("session_id is required")
        }
        val session = closeTargetSession(sessionId)
        CallToolResult(
            content = listOf(
                TextContent(
                    json.encodeToString(
                        CloseTargetSessionResult.serializer(),
                        CloseTargetSessionResult(
                            closed = session != null,
                            session = session?.toView(),
                        ),
                    ),
                ),
            ),
        )
    }

    server.addLoggedTool(
        name = "diagnose_target_sessions",
        description = "返回当前 MCP 进程内 target session 与 handle 的运行态摘要，用于降低黑盒感并辅助判断空闲自动回收是否生效。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {},
        ),
    ) {
        val snapshot = diagnoseTargetSessions()
        CallToolResult(
            content = listOf(
                TextContent(
                    json.encodeToString(
                        DiagnoseTargetSessionsResult.serializer(),
                        snapshot.toView(),
                    ),
                ),
            ),
        )
    }
}
