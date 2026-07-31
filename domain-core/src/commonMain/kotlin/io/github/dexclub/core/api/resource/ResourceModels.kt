package io.github.dexclub.core.api.resource

import io.github.dexclub.core.api.shared.PageWindow

enum class ResourceResolution {
    TableBacked,
    TableValue,
    Unresolved,
    TableHole,
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
    val componentName: String? = null,
    val componentType: ManifestComponentType? = null,
)

enum class ManifestComponentType { Activity, ActivityAlias, Service, Receiver, Provider }

data class ManifestAttribute(
    val namespaceUri: String? = null,
    val prefix: String? = null,
    val localName: String,
    val rawName: String,
    val value: String,
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
    val theme: String? = null,
    val attributes: List<ManifestAttribute> = emptyList(),
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
    val theme: String? = null,
    val windowSoftInputMode: String? = null,
    val attributes: List<ManifestAttribute> = emptyList(),
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
    val packageName: String? = null,
    val type: String? = null,
    val name: String? = null,
    val filePath: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val resolution: ResourceResolution = ResourceResolution.Unresolved,
)

fun ResourceEntry.normalizedResolution(): ResourceEntry =
    when {
        resolution == ResourceResolution.Unresolved &&
            filePath.isNullOrBlank() &&
            !name.isNullOrBlank() ->
            copy(resolution = ResourceResolution.TableValue)

        resolution == ResourceResolution.Unresolved &&
            name.isNullOrBlank() &&
            filePath.isNullOrBlank() &&
            sourceEntry.isNullOrBlank() ->
            copy(resolution = ResourceResolution.TableHole)

        else -> this
    }

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
    val packageName: String? = null,
    val type: String? = null,
    val name: String? = null,
    val qualifier: String? = null,
    val includeAllVariants: Boolean = true,
)

data class ResourceValue(
    val resourceId: String? = null,
    val packageName: String? = null,
    val type: String,
    val name: String,
    val variants: List<ResourceValueVariant> = emptyList(),
)

data class ResourceValueVariant(
    val configuration: ResourceConfiguration,
    val value: ResourceTypedValue? = null,
    val bag: ResourceBag? = null,
)

data class ResourceConfiguration(
    val qualifiers: String,
    val isDefault: Boolean,
)

data class ResourceTypedValue(
    val valueType: String,
    val rawData: Int,
    val rawDataHex: String,
    val decodedValue: String? = null,
    val referencedResourceId: String? = null,
)

enum class ResourceBagKind {
    Style,
    Array,
    Attribute,
    Plurals,
    Unknown,
}

data class ResourceBag(
    val kind: ResourceBagKind,
    val parentResourceId: String? = null,
    val parentResourceName: String? = null,
    val items: List<ResourceBagItem> = emptyList(),
)

data class ResourceBagItem(
    val rawKey: String,
    val keyResourceId: String? = null,
    val keyName: String? = null,
    val index: Int? = null,
    val quantity: String? = null,
    val attributeType: String? = null,
    val attributeFormats: List<String>? = null,
    val value: ResourceTypedValue,
)

data class FindResourcesRequest(
    val queryText: String,
    val window: PageWindow = PageWindow(),
)

data class ResourceEntryValueHit(
    val resourceId: String? = null,
    val packageName: String? = null,
    val type: String? = null,
    val name: String? = null,
    val value: String? = null,
    val qualifier: String? = null,
    val valueKind: String? = null,
    val matchTarget: String? = null,
    val bagIndex: Int? = null,
    val bagKey: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
)
