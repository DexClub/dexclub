package io.github.dexclub.mcp

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun stringSchema() = buildJsonObject { put("type", "string") }

internal fun booleanSchema() = buildJsonObject { put("type", "boolean") }

internal fun integerSchema() = buildJsonObject { put("type", "integer") }

internal fun stringArraySchema() = buildJsonObject {
    put("type", "array")
    put("items", stringSchema())
}

internal fun enumStringSchema(values: Set<String>) = buildJsonObject {
    put("type", "string")
    put(
        "enum",
        JsonArray(
            values.sorted().map(::JsonPrimitive),
        ),
    )
}

internal fun enumStringArraySchema(values: Set<String>) = buildJsonObject {
    put("type", "array")
    put("items", enumStringSchema(values))
}
