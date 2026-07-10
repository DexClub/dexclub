package io.github.dexclub.mcp

import io.github.dexclub.core.api.shared.Services
import io.github.dexclub.core.api.shared.createDefaultServices
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal class McpApp(
    internal val services: Services = createDefaultServices(),
    internal val sessionStore: McpSessionStore = McpSessionStore(),
) {
    private fun enumStringArraySchema(values: Set<String>) = buildJsonObject {
        put("type", "array")
        put(
            "items",
            buildJsonObject {
                put("type", "string")
                put(
                    "enum",
                    JsonArray(
                        values.sorted().map { value ->
                            kotlinx.serialization.json.JsonPrimitive(value)
                        },
                    ),
                )
            },
        )
    }

    internal val methodFieldsSchema = buildJsonObject {
        put("type", "array")
        put("items", enumStringArraySchema(methodFieldNamesWithHandle).getValue("items"))
    }

    internal val classFieldsSchema = buildJsonObject {
        put("type", "array")
        put("items", enumStringArraySchema(classFieldNamesWithHandle).getValue("items"))
    }

    internal val resourceEntryFieldsSchema = buildJsonObject {
        put("type", "array")
        put("items", enumStringArraySchema(resourceEntryFieldNames).getValue("items"))
    }

    internal val resourceValueFieldsSchema = buildJsonObject {
        put("type", "array")
        put("items", enumStringArraySchema(resourceValueFieldNames).getValue("items"))
    }

    internal val manifestIncludeSchema = buildJsonObject {
        val schema = enumStringArraySchema(manifestInspectionSectionNames)
        put("type", schema.getValue("type"))
        put("items", schema.getValue("items"))
    }

    internal val smaliModeSchema = buildJsonObject {
        put("type", "string")
        put(
            "enum",
            JsonArray(
                listOf("snippet", "class").map { value ->
                    kotlinx.serialization.json.JsonPrimitive(value)
                },
            ),
        )
    }

    internal val sessionIdOnlySchema = io.modelcontextprotocol.kotlin.sdk.types.ToolSchema(
        properties = buildJsonObject {
            put("session_id", stringSchema())
        },
        required = listOf("session_id"),
    )

    internal val json = Json {
        prettyPrint = false
        encodeDefaults = true
        explicitNulls = false
    }

    fun createServer(): Server {
        val server = Server(
            serverInfo = Implementation(
                name = "dexclub-mcp",
                version = McpBuildInfo.version,
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = false),
                ),
            ),
        )
        registerSessionTools(server)
        registerDexTools(server)
        registerResourceTools(server)

        return server
    }

    internal fun Server.addLoggedTool(
        name: String,
        description: String,
        inputSchema: io.modelcontextprotocol.kotlin.sdk.types.ToolSchema,
        handler: suspend (CallToolRequest) -> CallToolResult,
    ) {
        addTool(
            name = name,
            description = description,
            inputSchema = inputSchema,
        ) { request ->
            val summary = summarizeToolArguments(request.arguments)
            McpRuntimeDiagnostics.toolStarted(name, summary)
            try {
                handler(request).also { result ->
                    McpRuntimeDiagnostics.toolFinished(name, result.isError == true)
                }
            } catch (cause: Throwable) {
                McpRuntimeDiagnostics.toolFailed(name, cause)
                throw cause
            }
        }
    }

    private fun summarizeToolArguments(arguments: JsonObject?): String {
        if (arguments == null || arguments.isEmpty()) return ""
        val parts = mutableListOf<String>()
        val keys = arguments.keys.sorted()
        parts += "keys=${keys.joinToString(prefix = "[", postfix = "]")}"
        appendToolSummary(parts, arguments, "session_id")
        appendToolSummary(parts, arguments, "workdir")
        appendToolSummary(parts, arguments, "descriptor")
        appendToolSummary(parts, arguments, "method_handle")
        appendToolSummary(parts, arguments, "class_handle")
        appendToolSummary(parts, arguments, "class_name_contains")
        appendToolSummary(parts, arguments, "method_name_contains")
        appendToolSummary(parts, arguments, "descriptor_contains")
        appendToolSummary(parts, arguments, "type")
        appendToolSummary(parts, arguments, "name")
        appendToolSummary(parts, arguments, "resource_id")
        appendToolSummary(parts, arguments, "value")
        appendArraySummary(parts, arguments, "contains_any_strings")
        appendArraySummary(parts, arguments, "contains_all_strings")
        appendArraySummary(parts, arguments, "include")
        appendArraySummary(parts, arguments, "fields")
        return parts.joinToString(separator = " ")
    }

    private fun appendToolSummary(parts: MutableList<String>, arguments: JsonObject, key: String) {
        val value = arguments[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() } ?: return
        parts += "$key=${value.take(120)}"
    }

    private fun appendArraySummary(parts: MutableList<String>, arguments: JsonObject, key: String) {
        val array = arguments[key] as? JsonArray ?: return
        parts += "$key#=${array.size}"
    }

    fun close() {
        (services.dex as? AutoCloseable)?.close()
    }

    internal fun openTargetSession(input: String): TargetSession {
        val workspace = services.workspace.initialize(input)
        return sessionStore.openTargetSession(workspace)
    }

    internal fun listTargetSessions(): List<TargetSession> = sessionStore.listTargetSessions()

    internal fun getTargetSession(sessionId: String): TargetSession? = sessionStore.getTargetSession(sessionId)

    internal fun closeTargetSession(sessionId: String): TargetSession? = sessionStore.closeTargetSession(sessionId)

    internal fun diagnoseTargetSessions(): SessionStoreSnapshot = sessionStore.snapshot()
}
