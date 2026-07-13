package io.github.dexclub.mcp

import io.github.dexclub.core.app.contract.MethodDetailSection
import io.github.dexclub.core.app.contract.ManifestInspectionSection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest

internal data class DexToolTarget(
    val sessionId: String?,
    val workdir: String?,
)

internal fun CallToolRequest.dexToolTarget(): DexToolTarget =
    DexToolTarget(
        sessionId = optionalStringArgument("session_id"),
        workdir = optionalStringArgument("workdir"),
    )

internal fun CallToolRequest.briefFlag(): Boolean =
    booleanArgument("brief") ?: false

internal fun CallToolRequest.includeTextFlag(): Boolean =
    booleanArgument("include_text") ?: false

internal fun CallToolRequest.methodIncludeSections(): Set<MethodDetailSection> =
    parseMethodDetailSections(stringArrayArgument("include"))

internal fun CallToolRequest.manifestIncludeSections(): Set<ManifestInspectionSection> =
    parseManifestInspectionSections(stringArrayArgument("include"))

internal fun CallToolRequest.methodProjectionFields(sessionId: String?): Set<String>? =
    parseRequestedFields(
        stringArrayArgument("fields"),
        supported = if (sessionId != null) methodFieldNamesWithHandle else methodFieldNames,
        sessionRequiredFields = setOf("methodHandle"),
        hasSession = sessionId != null,
    )

internal fun CallToolRequest.classProjectionFields(sessionId: String?): Set<String>? =
    parseRequestedFields(
        stringArrayArgument("fields"),
        supported = if (sessionId != null) classFieldNamesWithHandle else classFieldNames,
        sessionRequiredFields = setOf("classHandle"),
        hasSession = sessionId != null,
    )

internal fun CallToolRequest.resourceEntryProjectionFields(): Set<String>? =
    parseRequestedFields(
        stringArrayArgument("fields"),
        supported = resourceEntryFieldNames,
    )

internal fun CallToolRequest.resourceValueProjectionFields(): Set<String>? =
    parseRequestedFields(
        stringArrayArgument("fields"),
        supported = resourceValueFieldNames,
    )

