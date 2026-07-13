package io.github.dexclub.mcp

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal data class McpToolInputProperty(
    val name: String,
    val schema: JsonObject,
    val signature: String,
)

internal data class McpToolMetadata(
    val name: String,
    val description: String,
    val inputProperties: List<McpToolInputProperty> = emptyList(),
    val required: Set<String> = emptySet(),
) {
    fun toToolSchema(): ToolSchema =
        ToolSchema(
            properties = buildJsonObject {
                inputProperties.forEach { property ->
                    put(property.name, property.schema)
                }
            },
            required = required.toList(),
        )
}

internal object McpToolInputProperties {
    fun string(name: String): McpToolInputProperty =
        McpToolInputProperty(
            name = name,
            schema = stringSchema(),
            signature = "string",
        )

    fun boolean(name: String): McpToolInputProperty =
        McpToolInputProperty(
            name = name,
            schema = booleanSchema(),
            signature = "boolean",
        )

    fun integer(name: String): McpToolInputProperty =
        McpToolInputProperty(
            name = name,
            schema = integerSchema(),
            signature = "integer",
        )

    fun stringArray(name: String): McpToolInputProperty =
        McpToolInputProperty(
            name = name,
            schema = stringArraySchema(),
            signature = "array<string>",
        )

    fun enumString(name: String, values: Set<String>): McpToolInputProperty =
        McpToolInputProperty(
            name = name,
            schema = enumStringSchema(values),
            signature = "string:${values.sorted().joinToString(",")}",
        )

    fun enumStringArray(name: String, values: Set<String>): McpToolInputProperty =
        McpToolInputProperty(
            name = name,
            schema = enumStringArraySchema(values),
            signature = "array<string:${values.sorted().joinToString(",")}>",
        )
}

internal fun contextualInputProperties(vararg properties: McpToolInputProperty): List<McpToolInputProperty> =
    buildList {
        add(McpToolInputProperties.string("session_id"))
        add(McpToolInputProperties.string("workdir"))
        addAll(properties)
    }

internal object McpToolCatalogs {
    val tools: List<McpToolMetadata> =
        McpSessionToolCatalog.tools +
            McpDexToolCatalog.tools +
            McpResourceToolCatalog.tools
}

internal fun McpApp.registerCatalogTool(
    server: Server,
    metadata: McpToolMetadata,
    handler: suspend (CallToolRequest) -> CallToolResult,
) {
    server.addLoggedTool(
        name = metadata.name,
        description = metadata.description,
        inputSchema = metadata.toToolSchema(),
        handler = handler,
    )
}
