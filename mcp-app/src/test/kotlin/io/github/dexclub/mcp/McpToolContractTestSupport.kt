package io.github.dexclub.mcp

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.assertEquals

private data class ToolInputContract(
    val properties: Map<String, String>,
    val required: Set<String> = emptySet(),
)

fun assertMcpToolInputContracts(tools: JsonArray) {
    val actual = tools.associate { element ->
        val tool = element.jsonObject
        val name = tool.getValue("name").jsonPrimitive.content
        val inputSchema = tool.getValue("inputSchema").jsonObject
        name to ToolInputContract(
            properties = inputSchema.getValue("properties").jsonObject
                .mapValues { (_, schema) -> schema.jsonObject.signature() },
            required = (inputSchema["required"] as? JsonArray)
                ?.map { it.jsonPrimitive.content }
                ?.toSet()
                .orEmpty(),
        )
    }

    assertEquals(expectedToolInputContracts, actual)
}

private fun JsonObject.signature(): String {
    val type = getValue("type").jsonPrimitive.content
    if (type != "array") {
        val enumValues = (this["enum"] as? JsonArray)
            ?.map { it.jsonPrimitive.content }
            ?.sorted()
            .orEmpty()
        return if (enumValues.isEmpty()) type else "$type:${enumValues.joinToString(",")}"
    }

    val items = getValue("items").jsonObject
    val itemType = items.getValue("type").jsonPrimitive.content
    val enumValues = (items["enum"] as? JsonArray)
        ?.map { it.jsonPrimitive.content }
        ?.sorted()
        .orEmpty()
    return if (enumValues.isEmpty()) {
        "array<$itemType>"
    } else {
        "array<$itemType:${enumValues.joinToString(",")}>"
    }
}

private fun contract(
    properties: Map<String, String> = emptyMap(),
    required: Set<String> = emptySet(),
): ToolInputContract = ToolInputContract(properties = properties, required = required)

private val expectedToolInputContracts = mapOf(
    *McpToolCatalogs.tools.associate { tool ->
        tool.name to contract(
            properties = tool.inputProperties.associate { it.name to it.signature },
            required = tool.required,
        )
    }.toList().toTypedArray(),
)
