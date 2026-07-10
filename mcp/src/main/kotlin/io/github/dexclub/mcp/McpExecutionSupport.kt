package io.github.dexclub.mcp

import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.workspace.WorkspaceRef
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private sealed interface ExecutionContextLookup {
    data class Found(val context: ExecutionContext) : ExecutionContextLookup

    data class MissingSession(val sessionId: String) : ExecutionContextLookup

    data object MissingLocator : ExecutionContextLookup
}

internal sealed interface ExecutionContextResolution {
    data class Ready(val context: ExecutionContext) : ExecutionContextResolution

    data class Failed(val result: CallToolResult) : ExecutionContextResolution
}

private fun McpApp.resolveExecutionContext(request: CallToolRequest): ExecutionContextLookup {
    val sessionId = request.optionalStringArgument("session_id")
    if (sessionId != null) {
        val session = sessionStore.getTargetSession(sessionId)
            ?: return ExecutionContextLookup.MissingSession(sessionId)
        return ExecutionContextLookup.Found(ExecutionContext(session = session, workspace = session.workspace))
    }
    val workdir = request.optionalStringArgument("workdir") ?: return ExecutionContextLookup.MissingLocator
    val workspace = services.workspace.open(WorkspaceRef(workdir))
    return ExecutionContextLookup.Found(ExecutionContext(session = null, workspace = workspace))
}

internal fun McpApp.executionContextOrFailureResult(request: CallToolRequest): ExecutionContextResolution =
    when (val lookup = resolveExecutionContext(request)) {
        is ExecutionContextLookup.Found -> ExecutionContextResolution.Ready(lookup.context)
        is ExecutionContextLookup.MissingSession -> ExecutionContextResolution.Failed(missingSessionResult(lookup.sessionId))
        ExecutionContextLookup.MissingLocator -> ExecutionContextResolution.Failed(missingSessionOrWorkdirResult())
    }

internal fun McpApp.missingSessionOrWorkdirResult(): CallToolResult =
    errorResult("session_id or workdir is required")

internal fun McpApp.missingSessionResult(sessionId: String): CallToolResult =
    errorResult(staleSessionMessage(sessionId))

internal fun McpApp.staleSessionMessage(sessionId: String): String =
    "session_id not found: $sessionId. The MCP process may have restarted, the session may have expired, or the chat may have been restored. Reopen the target with open_target_session, or switch to workdir for stateless calls"

// internal 可见性便于测试：错误消息可能携带原始用户输入，必须走 JSON 序列化以避免字符串注入。
internal fun McpApp.errorResult(message: String): CallToolResult =
    CallToolResult(
        content = listOf(TextContent(json.encodeToString(buildJsonObject { put("error", message) }))),
        isError = true,
    )

internal fun CallToolRequest.toSourceLocator(ref: SourceBackedHandleRef? = null): SourceLocator =
    SourceLocator(
        sourcePath = arguments?.get("source_path")?.jsonPrimitive?.content?.trim()?.ifEmpty { null } ?: ref?.sourcePath,
        sourceEntry = arguments?.get("source_entry")?.jsonPrimitive?.content?.trim()?.ifEmpty { null } ?: ref?.sourceEntry,
    )

internal fun CallToolRequest.requiredStringArgument(name: String): String =
    arguments?.get(name)?.jsonPrimitive?.content?.trim().orEmpty()

internal fun CallToolRequest.optionalStringArgument(name: String): String? =
    arguments?.get(name)?.jsonPrimitive?.content?.trim()?.ifEmpty { null }

internal fun CallToolRequest.stringArrayArgument(name: String): List<String> =
    (arguments?.get(name) as? JsonArray)
        ?.jsonArray
        ?.map { it.jsonPrimitive.content }
        .orEmpty()

internal fun CallToolRequest.intArgument(name: String): Int? =
    arguments?.get(name)?.jsonPrimitive?.content?.toIntOrNull()

internal fun CallToolRequest.booleanArgument(name: String): Boolean? =
    arguments?.get(name)?.jsonPrimitive?.content?.toBooleanStrictOrNull()

internal fun McpApp.resolveMethodReference(request: CallToolRequest, session: TargetSession?): MethodHandleRef? {
    val handle = request.optionalStringArgument("method_handle")
    if (handle != null) {
        requireNotNull(session) { "method_handle requires session_id" }
        return sessionStore.getMethodHandle(session.sessionId, handle)
            ?: throw IllegalArgumentException(
                "method_handle not found. Handles must come from a previous dexclub result in the same session; do not construct placeholder handles manually",
            )
    }
    val descriptor = request.optionalStringArgument("descriptor") ?: return null
    return MethodHandleRef(
        sessionId = session?.sessionId.orEmpty(),
        descriptor = descriptor,
        sourcePath = request.optionalStringArgument("source_path"),
        sourceEntry = request.optionalStringArgument("source_entry"),
    )
}

internal fun McpApp.resolveClassReference(request: CallToolRequest, session: TargetSession?): ClassHandleRef? {
    val handle = request.optionalStringArgument("class_handle")
    if (handle != null) {
        requireNotNull(session) { "class_handle requires session_id" }
        return sessionStore.getClassHandle(session.sessionId, handle)
            ?: throw IllegalArgumentException(
                "class_handle not found. Handles must come from a previous dexclub result in the same session; do not construct placeholder handles manually",
            )
    }
    val descriptor = request.optionalStringArgument("descriptor") ?: return null
    return ClassHandleRef(
        sessionId = session?.sessionId.orEmpty(),
        descriptor = descriptor,
        sourcePath = request.optionalStringArgument("source_path"),
        sourceEntry = request.optionalStringArgument("source_entry"),
    )
}
