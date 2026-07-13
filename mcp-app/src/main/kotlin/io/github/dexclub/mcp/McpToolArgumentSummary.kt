package io.github.dexclub.mcp

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun summarizeToolArguments(arguments: JsonObject?): String {
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
