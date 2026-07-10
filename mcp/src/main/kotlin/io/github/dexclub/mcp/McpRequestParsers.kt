package io.github.dexclub.mcp

import io.github.dexclub.core.api.dex.MethodDetailSection
import io.github.dexclub.core.api.resource.ManifestInspectionSection

internal val manifestInspectionSectionNames = setOf(
    "uses-sdk",
    "application",
    "uses-permissions",
    "defined-permissions",
    "uses-features",
    "queries",
    "activities",
    "activity-aliases",
    "services",
    "receivers",
    "providers",
)

internal fun parseMethodDetailSections(rawValues: List<String>?): Set<MethodDetailSection> {
    if (rawValues.isNullOrEmpty()) return MethodDetailSection.entries.toSet()
    return rawValues.map { raw ->
        when (raw.trim()) {
            "using-fields" -> MethodDetailSection.UsingFields
            "callers" -> MethodDetailSection.Callers
            "invokes" -> MethodDetailSection.Invokes
            "strings" -> MethodDetailSection.Strings
            "annotations" -> MethodDetailSection.Annotations
            else -> throw IllegalArgumentException(
                "Unsupported include section: $raw. Supported sections: using-fields, callers, invokes, strings, annotations",
            )
        }
    }.toSet()
}

internal fun parseManifestInspectionSections(rawValues: List<String>?): Set<ManifestInspectionSection> {
    if (rawValues.isNullOrEmpty()) return ManifestInspectionSection.entries.toSet()
    return rawValues.map { raw ->
        when (raw.trim()) {
            "uses-sdk" -> ManifestInspectionSection.UsesSdk
            "application" -> ManifestInspectionSection.Application
            "uses-permissions" -> ManifestInspectionSection.UsesPermissions
            "defined-permissions" -> ManifestInspectionSection.DefinedPermissions
            "uses-features" -> ManifestInspectionSection.UsesFeatures
            "queries" -> ManifestInspectionSection.Queries
            "activities" -> ManifestInspectionSection.Activities
            "activity-aliases" -> ManifestInspectionSection.ActivityAliases
            "services" -> ManifestInspectionSection.Services
            "receivers" -> ManifestInspectionSection.Receivers
            "providers" -> ManifestInspectionSection.Providers
            else -> throw IllegalArgumentException(
                "Unsupported include section: $raw. Supported sections: ${manifestInspectionSectionNames.joinToString(", ")}",
            )
        }
    }.toSet()
}

internal fun parseRequestedFields(
    rawValues: List<String>?,
    supported: Set<String>,
    sessionRequiredFields: Set<String> = emptySet(),
    hasSession: Boolean = true,
): Set<String>? {
    if (rawValues.isNullOrEmpty()) return null
    val normalized = rawValues.map { it.trim() }
    if (normalized.any { it.isEmpty() }) {
        throw IllegalArgumentException("fields must not contain blank entries")
    }
    if (!hasSession) {
        val requiresSession = normalized.filter { it in sessionRequiredFields }
        if (requiresSession.isNotEmpty()) {
            throw IllegalArgumentException(
                "Fields require session_id: ${requiresSession.joinToString(",")}. Open a target session first, or omit those fields when using workdir",
            )
        }
    }
    val unsupported = normalized.filter { it !in supported }
    if (unsupported.isNotEmpty()) {
        throw IllegalArgumentException("Unsupported fields: ${unsupported.joinToString(",")}")
    }
    return normalized.toSet()
}
