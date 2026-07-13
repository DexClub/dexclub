package io.github.dexclub.mcp

import io.github.dexclub.core.app.session.TargetExecutionContext
import io.github.dexclub.core.app.session.DexContextLease
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal sealed interface ExecutionContextResolution {
    data class Ready(val context: TargetExecutionContext) : ExecutionContextResolution

    data class Failed(val result: CallToolResult) : ExecutionContextResolution
}

internal fun McpApp.acquireToolContextLease(request: CallToolRequest): DexContextLease? {
    val sessionId = request.optionalStringArgument("session_id")
    val workdir = request.optionalStringArgument("workdir")
    val context = runCatching {
        sessionRuntime.resolveExecutionContext(
            sessionId = sessionId,
            workdir = workdir,
        )
    }.getOrNull() ?: return null
    return sessionRuntime.acquireDexContextForExecutionContext(context)
}

internal fun McpApp.executionContextOrFailureResult(request: CallToolRequest): ExecutionContextResolution {
    val sessionId = request.optionalStringArgument("session_id")
    val workdir = request.optionalStringArgument("workdir")
    if (sessionId == null && workdir == null) {
        return ExecutionContextResolution.Failed(missingSessionOrWorkdirResult())
    }
    return try {
        val context = sessionRuntime.resolveExecutionContext(
            sessionId = sessionId,
            workdir = workdir,
        )
        ExecutionContextResolution.Ready(context)
    } catch (_: NoSuchElementException) {
        ExecutionContextResolution.Failed(missingSessionResult(sessionId.orEmpty()))
    } catch (cause: IllegalArgumentException) {
        ExecutionContextResolution.Failed(errorResult(cause.message.orEmpty()))
    }
}

internal fun McpApp.missingSessionOrWorkdirResult(): CallToolResult =
    errorResult("session_id or workdir is required")

internal fun McpApp.missingSessionResult(sessionId: String): CallToolResult =
    errorResult(staleSessionMessage(sessionId))

internal fun McpApp.missingRequiredArgumentsResult(vararg names: String): CallToolResult =
    errorResult(missingRequiredArgumentsMessage(*names))

internal fun McpApp.missingAnyOfRequiredArgumentsResult(vararg alternatives: String): CallToolResult =
    errorResult("${alternatives.joinToString(" or ")} is required")

internal fun McpApp.missingRequiredArgumentsMessage(vararg names: String): String =
    when (names.size) {
        0 -> "required argument is missing"
        1 -> "${names.first()} is required"
        2 -> "${names[0]} and ${names[1]} are required"
        else -> "${names.dropLast(1).joinToString(", ")}, and ${names.last()} are required"
    }

internal fun McpApp.staleSessionMessage(sessionId: String): String =
    "session_id not found: $sessionId. The MCP process may have restarted, the session may have expired, or the chat may have been restored. Reopen the target with open_target_session, or switch to workdir for stateless calls"

// Keep this internal for tests: error messages may carry raw user input and must be JSON-serialized to avoid string injection.
internal fun McpApp.errorResult(message: String): CallToolResult =
    CallToolResult(
        content = listOf(TextContent(json.encodeToString(buildJsonObject { put("error", message) }))),
        isError = true,
    )

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
