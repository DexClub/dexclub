package io.github.dexclub.core.api.resource

import io.github.dexclub.core.api.shared.PageWindow

enum class ResourceResolution {
    TableBacked,
    PathInferred,
    Unresolved,
}

data class ManifestResult(
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val text: String,
)

enum class ManifestInspectionSection {
    UsesSdk,
    Application,
    UsesPermissions,
    DefinedPermissions,
    UsesFeatures,
    Queries,
    Activities,
    ActivityAliases,
    Services,
    Receivers,
    Providers,
}

data class InspectManifestRequest(
    val includes: Set<ManifestInspectionSection> = ManifestInspectionSection.entries.toSet(),
    val includeText: Boolean = false,
)

data class ManifestInspectionResult(
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val packageName: String,
    val versionCode: String? = null,
    val versionName: String? = null,
    val sharedUserId: String? = null,
    val usesSdk: ManifestUsesSdk? = null,
    val application: ManifestApplicationInfo? = null,
    val usesPermissions: List<String>? = null,
    val definedPermissions: List<String>? = null,
    val usesFeatures: List<ManifestUsesFeature>? = null,
    val queriesPackages: List<String>? = null,
    val queriesProviders: List<String>? = null,
    val queriesIntents: List<ManifestIntentFilter>? = null,
    val activities: List<ManifestComponentInfo>? = null,
    val activityAliases: List<ManifestComponentInfo>? = null,
    val services: List<ManifestComponentInfo>? = null,
    val receivers: List<ManifestComponentInfo>? = null,
    val providers: List<ManifestComponentInfo>? = null,
    val text: String? = null,
)

data class ManifestUsesSdk(
    val minSdkVersion: String? = null,
    val targetSdkVersion: String? = null,
    val maxSdkVersion: String? = null,
)

data class ManifestApplicationInfo(
    val name: String? = null,
    val rawName: String? = null,
    val label: String? = null,
    val icon: String? = null,
    val debuggable: Boolean? = null,
    val allowBackup: Boolean? = null,
    val usesCleartextTraffic: Boolean? = null,
    val networkSecurityConfig: String? = null,
    val metaData: List<ManifestMetaData> = emptyList(),
)

data class ManifestComponentInfo(
    val name: String,
    val rawName: String? = null,
    val exported: Boolean? = null,
    val enabled: Boolean? = null,
    val permission: String? = null,
    val process: String? = null,
    val authorities: String? = null,
    val targetActivity: String? = null,
    val intentFilters: List<ManifestIntentFilter> = emptyList(),
    val metaData: List<ManifestMetaData> = emptyList(),
)

data class ManifestIntentFilter(
    val actions: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val data: List<ManifestIntentData> = emptyList(),
)

data class ManifestIntentData(
    val scheme: String? = null,
    val host: String? = null,
    val port: String? = null,
    val path: String? = null,
    val pathPrefix: String? = null,
    val pathPattern: String? = null,
    val pathSuffix: String? = null,
    val mimeType: String? = null,
)

data class ManifestMetaData(
    val name: String,
    val value: String? = null,
    val resource: String? = null,
)

data class ManifestUsesFeature(
    val name: String? = null,
    val required: Boolean? = null,
    val glEsVersion: String? = null,
)

data class ResourceEntry(
    val resourceId: String? = null,
    val type: String? = null,
    val name: String? = null,
    val filePath: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val resolution: ResourceResolution = ResourceResolution.Unresolved,
)

data class ResourceTableResult(
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val packageCount: Int,
    val typeCount: Int,
    val entryCount: Int,
    val entries: List<ResourceEntry> = emptyList(),
)

data class DecodeXmlRequest(
    val path: String,
)

data class DecodedXmlResult(
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val text: String,
)

data class ResolveResourceRequest(
    val resourceId: String? = null,
    val type: String? = null,
    val name: String? = null,
)

data class ResourceValue(
    val resourceId: String? = null,
    val type: String,
    val name: String,
    val value: String? = null,
)

data class FindResourcesRequest(
    val queryText: String,
    val window: PageWindow = PageWindow(),
)

data class ResourceEntryValueHit(
    val resourceId: String? = null,
    val type: String? = null,
    val name: String? = null,
    val value: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
)
