package io.github.dexclub.mcp

import io.github.dexclub.core.app.contract.FieldUsageType
import io.github.dexclub.core.app.contract.InputType
import io.github.dexclub.core.app.contract.WorkspaceKind
import kotlinx.serialization.Serializable

@Serializable
internal data class TargetSessionView(
    val sessionId: String,
    val createdAt: String,
    val workspace: WorkspaceContextView,
)

@Serializable
internal data class TargetSessionDiagnosticsView(
    val sessionId: String,
    val createdAt: String,
    val lastAccessedAt: String,
    val expiresAt: String? = null,
    val workspace: WorkspaceContextView,
)

@Serializable
internal data class WorkspaceContextView(
    val workdir: String,
    val dexclubDir: String,
    val workspaceId: String,
    val activeTargetId: String,
    val activeTarget: TargetHandleView,
    val snapshot: TargetSnapshotSummaryView,
)

@Serializable
internal data class TargetHandleView(
    val targetId: String,
    val inputType: InputType,
    val inputPath: String,
)

@Serializable
internal data class TargetSnapshotSummaryView(
    val kind: WorkspaceKind,
    val inventoryFingerprint: String,
    val contentFingerprint: String,
    val capabilities: CapabilitySetView,
    val inventoryCounts: InventoryCountsView,
)

@Serializable
internal data class CapabilitySetView(
    val inspect: Boolean,
    val findClass: Boolean,
    val findMethod: Boolean,
    val findField: Boolean,
    val exportDex: Boolean,
    val exportSmali: Boolean,
    val exportJava: Boolean,
    val manifestDecode: Boolean,
    val resourceTableDecode: Boolean,
    val xmlDecode: Boolean,
    val resourceEntryList: Boolean,
)

@Serializable
internal data class InventoryCountsView(
    val apkCount: Int,
    val dexCount: Int,
    val manifestCount: Int,
    val arscCount: Int,
    val binaryXmlCount: Int,
)

@Serializable
internal data class MethodDetailView(
    val method: MethodHitView,
    val counts: MethodDetailCountsView? = null,
    val usingFields: List<MethodFieldUsageView>? = null,
    val callers: List<MethodHitView>? = null,
    val invokes: List<MethodHitView>? = null,
    val strings: List<String>? = null,
    val annotations: List<String>? = null,
)

@Serializable
internal data class MethodDetailCountsView(
    val usingFields: Int? = null,
    val callers: Int? = null,
    val invokes: Int? = null,
    val strings: Int? = null,
    val annotations: Int? = null,
)

@Serializable
internal data class MethodHitView(
    val className: String,
    val methodName: String,
    val descriptor: String,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
)

@Serializable
internal data class ClassHitView(
    val className: String,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
)

@Serializable
internal data class FieldHitView(
    val className: String,
    val fieldName: String,
    val descriptor: String,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
)

@Serializable
internal data class MethodFieldUsageView(
    val usingType: FieldUsageType,
    val field: FieldHitView,
)

@Serializable
internal data class ResourceValueView(
    val resourceId: String? = null,
    val type: String,
    val name: String,
    val value: String? = null,
    val pluralItems: List<ResourcePluralItemView>? = null,
)

@Serializable
internal data class ResourcePluralItemView(
    val quantity: String,
    val value: String,
)

@Serializable
internal data class ResourceEntryView(
    val resourceId: String? = null,
    val type: String? = null,
    val name: String? = null,
    val filePath: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val resolution: String,
)

@Serializable
internal data class ResourceEntryValueHitView(
    val resourceId: String? = null,
    val type: String? = null,
    val name: String? = null,
    val value: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
)

@Serializable
internal data class DecodedXmlView(
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val text: String,
)

@Serializable
internal data class ManifestInspectionView(
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val packageName: String,
    val versionCode: String? = null,
    val versionName: String? = null,
    val sharedUserId: String? = null,
    val usesSdk: ManifestUsesSdkView? = null,
    val application: ManifestApplicationView? = null,
    val usesPermissions: List<String>? = null,
    val definedPermissions: List<String>? = null,
    val usesFeatures: List<ManifestUsesFeatureView>? = null,
    val queriesPackages: List<String>? = null,
    val queriesProviders: List<String>? = null,
    val queriesIntents: List<ManifestIntentFilterView>? = null,
    val activities: List<ManifestComponentView>? = null,
    val activityAliases: List<ManifestComponentView>? = null,
    val services: List<ManifestComponentView>? = null,
    val receivers: List<ManifestComponentView>? = null,
    val providers: List<ManifestComponentView>? = null,
    val text: String? = null,
)

@Serializable
internal data class ManifestUsesSdkView(
    val minSdkVersion: String? = null,
    val targetSdkVersion: String? = null,
    val maxSdkVersion: String? = null,
)

@Serializable
internal data class ManifestApplicationView(
    val name: String? = null,
    val rawName: String? = null,
    val label: String? = null,
    val icon: String? = null,
    val debuggable: Boolean? = null,
    val allowBackup: Boolean? = null,
    val usesCleartextTraffic: Boolean? = null,
    val networkSecurityConfig: String? = null,
    val metaData: List<ManifestMetaDataView> = emptyList(),
)

@Serializable
internal data class ManifestComponentView(
    val name: String,
    val rawName: String? = null,
    val exported: Boolean? = null,
    val enabled: Boolean? = null,
    val permission: String? = null,
    val process: String? = null,
    val authorities: String? = null,
    val targetActivity: String? = null,
    val intentFilters: List<ManifestIntentFilterView> = emptyList(),
    val metaData: List<ManifestMetaDataView> = emptyList(),
)

@Serializable
internal data class ManifestIntentFilterView(
    val actions: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val data: List<ManifestIntentDataView> = emptyList(),
)

@Serializable
internal data class ManifestIntentDataView(
    val scheme: String? = null,
    val host: String? = null,
    val port: String? = null,
    val path: String? = null,
    val pathPrefix: String? = null,
    val pathPattern: String? = null,
    val pathSuffix: String? = null,
    val mimeType: String? = null,
)

@Serializable
internal data class ManifestMetaDataView(
    val name: String,
    val value: String? = null,
    val resource: String? = null,
)

@Serializable
internal data class ManifestUsesFeatureView(
    val name: String? = null,
    val required: Boolean? = null,
    val glEsVersion: String? = null,
)

