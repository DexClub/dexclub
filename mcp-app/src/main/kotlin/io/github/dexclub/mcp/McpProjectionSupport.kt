package io.github.dexclub.mcp

import io.github.dexclub.core.app.contract.ClassHit
import io.github.dexclub.core.app.contract.MethodHit
import io.github.dexclub.core.app.contract.ResourceEntry
import io.github.dexclub.core.app.contract.ResourceEntryValueHit
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal val methodFieldNames = setOf("className", "methodName", "descriptor", "sourcePath", "sourceEntry")
internal val methodFieldNamesWithHandle = methodFieldNames + "methodHandle"
internal val classFieldNames = setOf("className", "sourcePath", "sourceEntry")
internal val classFieldNamesWithHandle = classFieldNames + "classHandle"
internal val resourceEntryFieldNames = setOf("resourceId", "type", "name", "filePath", "sourcePath", "sourceEntry", "resolution")
internal val resourceValueFieldNames = setOf("resourceId", "type", "name", "value", "sourcePath", "sourceEntry")

internal fun effectiveMethodFields(fields: Set<String>?, brief: Boolean, handleEnabled: Boolean): Set<String> =
    fields ?: if (brief) {
        if (handleEnabled) setOf("descriptor", "sourcePath", "sourceEntry", "methodHandle")
        else setOf("descriptor", "sourcePath", "sourceEntry")
    } else {
        if (handleEnabled) methodFieldNamesWithHandle else methodFieldNames
    }

internal fun effectiveClassFields(fields: Set<String>?, brief: Boolean, handleEnabled: Boolean): Set<String> =
    fields ?: if (brief) {
        if (handleEnabled) setOf("className", "classHandle")
        else setOf("className")
    } else {
        if (handleEnabled) classFieldNamesWithHandle else classFieldNames
    }

internal fun effectiveResourceEntryFields(fields: Set<String>?, brief: Boolean): Set<String> =
    fields ?: if (brief) setOf("resourceId", "type", "name") else resourceEntryFieldNames

internal fun effectiveResourceValueFields(fields: Set<String>?, brief: Boolean): Set<String> =
    fields ?: if (brief) setOf("resourceId", "type", "name", "value") else resourceValueFieldNames

internal fun MethodHit.toProjectedJson(fields: Set<String>, handleProvider: ((MethodHit) -> String)?): JsonObject =
    buildJsonObject {
        if ("className" in fields) put("className", className)
        if ("methodName" in fields) put("methodName", methodName)
        if ("descriptor" in fields) put("descriptor", descriptor)
        if ("sourcePath" in fields && sourcePath != null) put("sourcePath", sourcePath)
        if ("sourceEntry" in fields && sourceEntry != null) put("sourceEntry", sourceEntry)
        if ("methodHandle" in fields) put("methodHandle", requireNotNull(handleProvider) { "methodHandle requires session_id" }(this@toProjectedJson))
    }

internal fun ClassHit.toProjectedJson(fields: Set<String>, handleProvider: ((ClassHit) -> String)?): JsonObject =
    buildJsonObject {
        if ("className" in fields) put("className", className)
        if ("sourcePath" in fields && sourcePath != null) put("sourcePath", sourcePath)
        if ("sourceEntry" in fields && sourceEntry != null) put("sourceEntry", sourceEntry)
        if ("classHandle" in fields) put("classHandle", requireNotNull(handleProvider) { "classHandle requires session_id" }(this@toProjectedJson))
    }

internal fun ResourceEntry.toProjectedJson(fields: Set<String>): JsonObject =
    buildJsonObject {
        if ("resourceId" in fields && resourceId != null) put("resourceId", resourceId)
        if ("type" in fields && type != null) put("type", type)
        if ("name" in fields && name != null) put("name", name)
        if ("filePath" in fields && filePath != null) put("filePath", filePath)
        if ("sourcePath" in fields && sourcePath != null) put("sourcePath", sourcePath)
        if ("sourceEntry" in fields && sourceEntry != null) put("sourceEntry", sourceEntry)
        if ("resolution" in fields) put("resolution", resolution.toMcpValue())
    }

internal fun ResourceEntryValueHit.toProjectedJson(fields: Set<String>): JsonObject =
    buildJsonObject {
        if ("resourceId" in fields && resourceId != null) put("resourceId", resourceId)
        if ("type" in fields && type != null) put("type", type)
        if ("name" in fields && name != null) put("name", name)
        if ("value" in fields && value != null) put("value", value)
        if ("sourcePath" in fields && sourcePath != null) put("sourcePath", sourcePath)
        if ("sourceEntry" in fields && sourceEntry != null) put("sourceEntry", sourceEntry)
    }

