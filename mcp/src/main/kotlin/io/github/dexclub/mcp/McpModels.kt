package io.github.dexclub.mcp

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.FindClassesUsingStringsRequest
import io.github.dexclub.core.api.dex.FindMethodsRequest
import io.github.dexclub.core.api.dex.FindMethodsUsingStringsRequest
import io.github.dexclub.core.api.dex.FieldHit
import io.github.dexclub.core.api.dex.FieldUsageType
import io.github.dexclub.core.api.dex.MethodDetail
import io.github.dexclub.core.api.dex.MethodDetailSection
import io.github.dexclub.core.api.dex.MethodFieldUsage
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.shared.CapabilitySet
import io.github.dexclub.core.api.shared.InputType
import io.github.dexclub.core.api.shared.InventoryCounts
import io.github.dexclub.core.api.shared.PageWindow
import io.github.dexclub.core.api.shared.WorkspaceKind
import io.github.dexclub.core.api.resource.FindResourcesRequest
import io.github.dexclub.core.api.resource.DecodedXmlResult
import io.github.dexclub.core.api.resource.ManifestApplicationInfo
import io.github.dexclub.core.api.resource.ManifestComponentInfo
import io.github.dexclub.core.api.resource.ManifestInspectionResult
import io.github.dexclub.core.api.resource.ManifestInspectionSection
import io.github.dexclub.core.api.resource.ManifestIntentData
import io.github.dexclub.core.api.resource.ManifestIntentFilter
import io.github.dexclub.core.api.resource.ManifestMetaData
import io.github.dexclub.core.api.resource.ManifestUsesFeature
import io.github.dexclub.core.api.resource.ManifestUsesSdk
import io.github.dexclub.core.api.resource.ResourceEntry
import io.github.dexclub.core.api.resource.ResourceEntryValueHit
import io.github.dexclub.core.api.resource.ResourceResolution
import io.github.dexclub.core.api.resource.ResourceValue
import io.github.dexclub.core.api.workspace.TargetHandle
import io.github.dexclub.core.api.workspace.TargetSnapshotSummary
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.dexkit.query.BatchFindClassUsingStrings
import io.github.dexclub.dexkit.query.BatchFindMethodUsingStrings
import io.github.dexclub.dexkit.query.ClassMatcher
import io.github.dexclub.dexkit.query.FindMethod
import io.github.dexclub.dexkit.query.MethodMatcher
import io.github.dexclub.dexkit.query.StringMatchType
import io.github.dexclub.dexkit.query.StringMatcher
import java.time.Duration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

@Serializable
internal data class OpenTargetSessionResult(
    val sessionId: String,
    val createdAt: String,
    val workspace: WorkspaceContextView,
)

@Serializable
internal data class TargetSessionView(
    val sessionId: String,
    val createdAt: String,
    val workspace: WorkspaceContextView,
)

@Serializable
internal data class ListTargetSessionsResult(
    val total: Int,
    val items: List<TargetSessionView>,
)

@Serializable
internal data class CloseTargetSessionResult(
    val closed: Boolean,
    val session: TargetSessionView? = null,
)

@Serializable
internal data class DiagnoseTargetSessionsResult(
    val now: String,
    val idleTimeoutSeconds: Long? = null,
    val sessionCount: Int,
    val methodHandleCount: Int,
    val classHandleCount: Int,
    val sessions: List<TargetSessionDiagnosticsView>,
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
internal data class InspectMethodResult(
    val sessionId: String? = null,
    val detail: MethodDetailView,
)

@Serializable
internal data class ManifestDecodeResult(
    val sessionId: String? = null,
    val manifest: ManifestInspectionView,
)

@Serializable
internal data class FindClassesUsingStringsResult(
    val sessionId: String? = null,
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
    val items: List<JsonObject>,
)

@Serializable
internal data class ExportTextResult(
    val sessionId: String? = null,
    val descriptor: String,
    val view: String,
    val text: String,
)

@Serializable
internal data class FindMethodsUsingStringsResult(
    val sessionId: String? = null,
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
    val items: List<JsonObject>,
)

@Serializable
internal data class FindMethodsResult(
    val sessionId: String? = null,
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
    val items: List<JsonObject>,
)

@Serializable
internal data class ResolveResourceResult(
    val sessionId: String? = null,
    val resource: ResourceValueView,
)

@Serializable
internal data class ListResourcesResult(
    val sessionId: String? = null,
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
    val items: List<JsonObject>,
)

@Serializable
internal data class FindResourcesResult(
    val sessionId: String? = null,
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
    val items: List<JsonObject>,
)

@Serializable
internal data class DecodeXmlResult(
    val sessionId: String? = null,
    val xml: DecodedXmlView,
)

internal data class WindowedItems<T>(
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
    val items: List<T>,
)

internal typealias WindowedClassHits = WindowedItems<ClassHit>
internal typealias WindowedMethodHits = WindowedItems<MethodHit>
internal typealias WindowedResourceEntries = WindowedItems<ResourceEntry>
internal typealias WindowedResourceValueHits = WindowedItems<ResourceEntryValueHit>

internal data class ExecutionContext(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
)

internal fun TargetSession.sessionContext(): ExecutionContext =
    ExecutionContext(session = this, workspace = workspace)

internal val methodFieldNames = setOf("className", "methodName", "descriptor", "sourcePath", "sourceEntry")
internal val methodFieldNamesWithHandle = methodFieldNames + "methodHandle"
internal val classFieldNames = setOf("className", "sourcePath", "sourceEntry")
internal val classFieldNamesWithHandle = classFieldNames + "classHandle"
internal val resourceEntryFieldNames = setOf("resourceId", "type", "name", "filePath", "sourcePath", "sourceEntry", "resolution")
internal val resourceValueFieldNames = setOf("resourceId", "type", "name", "value", "sourcePath", "sourceEntry")

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
) {
    companion object {
        fun from(result: ResourceValue): ResourceValueView =
            ResourceValueView(
                resourceId = result.resourceId,
                type = result.type,
                name = result.name,
                value = result.value,
            )
    }
}

@Serializable
internal data class ResourceEntryView(
    val resourceId: String? = null,
    val type: String? = null,
    val name: String? = null,
    val filePath: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val resolution: String,
) {
    companion object {
        fun from(entry: ResourceEntry): ResourceEntryView =
            ResourceEntryView(
                resourceId = entry.resourceId,
                type = entry.type,
                name = entry.name,
                filePath = entry.filePath,
                sourcePath = entry.sourcePath,
                sourceEntry = entry.sourceEntry,
                resolution = entry.resolution.toMcpValue(),
            )
    }
}

@Serializable
internal data class ResourceEntryValueHitView(
    val resourceId: String? = null,
    val type: String? = null,
    val name: String? = null,
    val value: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
) {
    companion object {
        fun from(hit: ResourceEntryValueHit): ResourceEntryValueHitView =
            ResourceEntryValueHitView(
                resourceId = hit.resourceId,
                type = hit.type,
                name = hit.name,
                value = hit.value,
                sourcePath = hit.sourcePath,
                sourceEntry = hit.sourceEntry,
            )
    }
}

@Serializable
internal data class DecodedXmlView(
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val text: String,
) {
    companion object {
        fun from(result: DecodedXmlResult): DecodedXmlView =
            DecodedXmlView(
                sourcePath = result.sourcePath,
                sourceEntry = result.sourceEntry,
                text = result.text,
            )
    }
}

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

internal fun TargetSession.toResult(): OpenTargetSessionResult =
    OpenTargetSessionResult(
        sessionId = sessionId,
        createdAt = createdAt,
        workspace = workspace.toView(),
    )

internal fun TargetSession.toView(): TargetSessionView =
    TargetSessionView(
        sessionId = sessionId,
        createdAt = createdAt,
        workspace = workspace.toView(),
    )

internal fun SessionStoreSnapshot.toView(): DiagnoseTargetSessionsResult =
    DiagnoseTargetSessionsResult(
        now = now.toString(),
        idleTimeoutSeconds = idleTimeout?.seconds,
        sessionCount = sessionCount,
        methodHandleCount = methodHandleCount,
        classHandleCount = classHandleCount,
        sessions = sessions.map { it.toDiagnosticsView(idleTimeout) },
    )

private fun TargetSession.toDiagnosticsView(idleTimeout: Duration?): TargetSessionDiagnosticsView =
    TargetSessionDiagnosticsView(
        sessionId = sessionId,
        createdAt = createdAt,
        lastAccessedAt = lastAccessedAt.toString(),
        expiresAt = idleTimeout?.let { lastAccessedAt.plus(it).toString() },
        workspace = workspace.toView(),
    )

internal fun ExecutionContext.toInspectMethodResult(detail: MethodDetail, brief: Boolean): InspectMethodResult =
    InspectMethodResult(
        sessionId = session?.sessionIdOrNull(),
        detail = detail.toView(brief = brief),
    )

internal fun ExecutionContext.toManifestDecodeResult(result: ManifestInspectionResult): ManifestDecodeResult =
    ManifestDecodeResult(
        sessionId = session?.sessionIdOrNull(),
        manifest = result.toView(),
    )

internal fun ExecutionContext.toFindClassesUsingStringsResult(
    result: WindowedClassHits,
    handleProvider: ((ClassHit) -> String)? = null,
    fields: Set<String>? = null,
    brief: Boolean = false,
): FindClassesUsingStringsResult =
    FindClassesUsingStringsResult(
        sessionId = session?.sessionIdOrNull(),
        total = result.total,
        offset = result.offset,
        limit = result.limit,
        hasMore = result.hasMore,
        items = result.items.map { it.toProjectedJson(effectiveClassFields(fields, brief, handleProvider != null), handleProvider) },
    )

internal fun ExecutionContext.toExportTextResult(
    descriptor: String,
    view: String,
    text: String,
): ExportTextResult = ExportTextResult(
    sessionId = session?.sessionIdOrNull(),
    descriptor = descriptor,
    view = view,
    text = text,
)

internal fun ExecutionContext.toFindMethodsUsingStringsResult(
    result: WindowedMethodHits,
    handleProvider: ((MethodHit) -> String)? = null,
    fields: Set<String>? = null,
    brief: Boolean = false,
): FindMethodsUsingStringsResult =
    FindMethodsUsingStringsResult(
        sessionId = session?.sessionIdOrNull(),
        total = result.total,
        offset = result.offset,
        limit = result.limit,
        hasMore = result.hasMore,
        items = result.items.map { it.toProjectedJson(effectiveMethodFields(fields, brief, handleProvider != null), handleProvider) },
    )

internal fun ExecutionContext.toFindMethodsResult(
    result: WindowedMethodHits,
    handleProvider: ((MethodHit) -> String)? = null,
    fields: Set<String>? = null,
    brief: Boolean = false,
): FindMethodsResult =
    FindMethodsResult(
        sessionId = session?.sessionIdOrNull(),
        total = result.total,
        offset = result.offset,
        limit = result.limit,
        hasMore = result.hasMore,
        items = result.items.map { it.toProjectedJson(effectiveMethodFields(fields, brief, handleProvider != null), handleProvider) },
    )

internal fun ExecutionContext.toResolveResourceResult(result: io.github.dexclub.core.api.resource.ResourceValue): ResolveResourceResult =
    ResolveResourceResult(
        sessionId = session?.sessionIdOrNull(),
        resource = ResourceValueView.from(result),
    )

internal fun ExecutionContext.toListResourcesResult(
    result: WindowedResourceEntries,
    fields: Set<String>? = null,
    brief: Boolean = false,
): ListResourcesResult =
    ListResourcesResult(
        sessionId = session?.sessionIdOrNull(),
        total = result.total,
        offset = result.offset,
        limit = result.limit,
        hasMore = result.hasMore,
        items = result.items.map { it.toProjectedJson(effectiveResourceEntryFields(fields, brief)) },
    )

internal fun ExecutionContext.toFindResourcesResult(
    result: WindowedResourceValueHits,
    fields: Set<String>? = null,
    brief: Boolean = false,
): FindResourcesResult =
    FindResourcesResult(
        sessionId = session?.sessionIdOrNull(),
        total = result.total,
        offset = result.offset,
        limit = result.limit,
        hasMore = result.hasMore,
        items = result.items.map { it.toProjectedJson(effectiveResourceValueFields(fields, brief)) },
    )

internal fun ExecutionContext.toDecodeXmlResult(result: DecodedXmlResult): DecodeXmlResult =
    DecodeXmlResult(
        sessionId = session?.sessionIdOrNull(),
        xml = DecodedXmlView.from(result),
    )

internal fun WorkspaceContext.toView(): WorkspaceContextView =
    WorkspaceContextView(
        workdir = workdir,
        dexclubDir = dexclubDir,
        workspaceId = workspaceId,
        activeTargetId = activeTargetId,
        activeTarget = activeTarget.toView(),
        snapshot = snapshot.toView(),
    )

internal fun TargetHandle.toView(): TargetHandleView =
    TargetHandleView(
        targetId = targetId,
        inputType = inputType,
        inputPath = inputPath,
    )

internal fun TargetSnapshotSummary.toView(): TargetSnapshotSummaryView =
    TargetSnapshotSummaryView(
        kind = kind,
        inventoryFingerprint = inventoryFingerprint,
        contentFingerprint = contentFingerprint,
        capabilities = capabilities.toView(),
        inventoryCounts = inventoryCounts.toView(),
    )

internal fun CapabilitySet.toView(): CapabilitySetView =
    CapabilitySetView(
        inspect = inspect,
        findClass = findClass,
        findMethod = findMethod,
        findField = findField,
        exportDex = exportDex,
        exportSmali = exportSmali,
        exportJava = exportJava,
        manifestDecode = manifestDecode,
        resourceTableDecode = resourceTableDecode,
        xmlDecode = xmlDecode,
        resourceEntryList = resourceEntryList,
    )

internal fun InventoryCounts.toView(): InventoryCountsView =
    InventoryCountsView(
        apkCount = apkCount,
        dexCount = dexCount,
        manifestCount = manifestCount,
        arscCount = arscCount,
        binaryXmlCount = binaryXmlCount,
    )

internal fun MethodDetail.toView(brief: Boolean = false): MethodDetailView =
    if (brief) {
        MethodDetailView(
            method = method.toView(),
            counts = MethodDetailCountsView(
                usingFields = usingFields?.size,
                callers = callers?.size,
                invokes = invokes?.size,
                strings = strings?.size,
                annotations = annotations?.size,
            ),
        )
    } else {
        MethodDetailView(
            method = method.toView(),
            usingFields = usingFields?.map(MethodFieldUsage::toView),
            callers = callers?.map(MethodHit::toView),
            invokes = invokes?.map(MethodHit::toView),
            strings = strings,
            annotations = annotations,
        )
    }

internal fun MethodHit.toView(): MethodHitView =
    MethodHitView(
        className = className,
        methodName = methodName,
        descriptor = descriptor,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )

internal fun ClassHit.toView(): ClassHitView =
    ClassHitView(
        className = className,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )

internal fun FieldHit.toView(): FieldHitView =
    FieldHitView(
        className = className,
        fieldName = fieldName,
        descriptor = descriptor,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )

internal fun MethodFieldUsage.toView(): MethodFieldUsageView =
    MethodFieldUsageView(
        usingType = usingType,
        field = field.toView(),
    )

internal fun ManifestInspectionResult.toView(): ManifestInspectionView =
    ManifestInspectionView(
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
        packageName = packageName,
        versionCode = versionCode,
        versionName = versionName,
        sharedUserId = sharedUserId,
        usesSdk = usesSdk?.toView(),
        application = application?.toView(),
        usesPermissions = usesPermissions,
        definedPermissions = definedPermissions,
        usesFeatures = usesFeatures?.map(ManifestUsesFeature::toView),
        queriesPackages = queriesPackages,
        queriesProviders = queriesProviders,
        queriesIntents = queriesIntents?.map(ManifestIntentFilter::toView),
        activities = activities?.map(ManifestComponentInfo::toView),
        activityAliases = activityAliases?.map(ManifestComponentInfo::toView),
        services = services?.map(ManifestComponentInfo::toView),
        receivers = receivers?.map(ManifestComponentInfo::toView),
        providers = providers?.map(ManifestComponentInfo::toView),
        text = text,
    )

internal fun ManifestUsesSdk.toView(): ManifestUsesSdkView =
    ManifestUsesSdkView(
        minSdkVersion = minSdkVersion,
        targetSdkVersion = targetSdkVersion,
        maxSdkVersion = maxSdkVersion,
    )

internal fun ManifestApplicationInfo.toView(): ManifestApplicationView =
    ManifestApplicationView(
        name = name,
        rawName = rawName,
        label = label,
        icon = icon,
        debuggable = debuggable,
        allowBackup = allowBackup,
        usesCleartextTraffic = usesCleartextTraffic,
        networkSecurityConfig = networkSecurityConfig,
        metaData = metaData.map(ManifestMetaData::toView),
    )

internal fun ManifestComponentInfo.toView(): ManifestComponentView =
    ManifestComponentView(
        name = name,
        rawName = rawName,
        exported = exported,
        enabled = enabled,
        permission = permission,
        process = process,
        authorities = authorities,
        targetActivity = targetActivity,
        intentFilters = intentFilters.map(ManifestIntentFilter::toView),
        metaData = metaData.map(ManifestMetaData::toView),
    )

internal fun ManifestIntentFilter.toView(): ManifestIntentFilterView =
    ManifestIntentFilterView(
        actions = actions,
        categories = categories,
        data = data.map(ManifestIntentData::toView),
    )

internal fun ManifestIntentData.toView(): ManifestIntentDataView =
    ManifestIntentDataView(
        scheme = scheme,
        host = host,
        port = port,
        path = path,
        pathPrefix = pathPrefix,
        pathPattern = pathPattern,
        pathSuffix = pathSuffix,
        mimeType = mimeType,
    )

internal fun ManifestMetaData.toView(): ManifestMetaDataView =
    ManifestMetaDataView(
        name = name,
        value = value,
        resource = resource,
    )

internal fun ManifestUsesFeature.toView(): ManifestUsesFeatureView =
    ManifestUsesFeatureView(
        name = name,
        required = required,
        glEsVersion = glEsVersion,
    )

internal fun buildFindMethodsUsingStringsRequest(
    strings: List<String>,
    requireAll: Boolean,
): FindMethodsUsingStringsRequest =
    FindMethodsUsingStringsRequest(
        queryText = Json.encodeToString(
            BatchFindMethodUsingStrings.serializer(),
            buildStringMatcherQuery(BatchFindMethodUsingStrings(), strings, requireAll),
        ),
        window = PageWindow(),
    )

internal fun buildFindMethodsRequest(
    classNameContains: String? = null,
    methodNameContains: String? = null,
): FindMethodsRequest {
    val normalizedClassName = classNameContains?.trim()?.ifEmpty { null }
    val normalizedMethodName = methodNameContains?.trim()?.ifEmpty { null }
    require(normalizedClassName != null || normalizedMethodName != null) {
        "At least one of class_name_contains or method_name_contains is required"
    }

    val matcher = MethodMatcher().apply {
        if (normalizedMethodName != null) {
            name = containsMatcher(normalizedMethodName)
        }
        if (normalizedClassName != null) {
            declaredClass = ClassMatcher(
                className = containsMatcher(normalizedClassName),
            )
        }
    }

    return FindMethodsRequest(
        queryText = Json.encodeToString(
            FindMethod.serializer(),
            FindMethod(matcher = matcher),
        ),
        window = PageWindow(),
    )
}

internal fun buildFindClassesUsingStringsRequest(
    strings: List<String>,
    requireAll: Boolean,
): FindClassesUsingStringsRequest =
    FindClassesUsingStringsRequest(
        queryText = Json.encodeToString(
            BatchFindClassUsingStrings.serializer(),
            buildStringMatcherQuery(BatchFindClassUsingStrings(), strings, requireAll),
        ),
        window = PageWindow(),
    )

internal fun buildFindResourcesRequest(
    type: String,
    value: String,
    contains: Boolean,
    ignoreCase: Boolean,
): FindResourcesRequest =
    FindResourcesRequest(
        queryText = buildJsonObject {
            put("type", type)
            put("value", value)
            put("contains", contains)
            put("ignoreCase", ignoreCase)
        }.toString(),
        window = PageWindow(),
    )

private fun buildStringMatcherQuery(
    query: BatchFindMethodUsingStrings,
    strings: List<String>,
    requireAll: Boolean,
): BatchFindMethodUsingStrings = query.apply {
    populateStringMatcherGroups(groups, strings, requireAll)
}

private fun buildStringMatcherQuery(
    query: BatchFindClassUsingStrings,
    strings: List<String>,
    requireAll: Boolean,
): BatchFindClassUsingStrings = query.apply {
    populateStringMatcherGroups(groups, strings, requireAll)
}

private fun populateStringMatcherGroups(
    groups: MutableMap<String, List<StringMatcher>>,
    strings: List<String>,
    requireAll: Boolean,
) {
    val normalized = strings.filter { it.isNotBlank() }
    if (requireAll) {
        if (normalized.isNotEmpty()) {
            groups["all"] = normalized.map(::containsMatcher)
        }
    } else {
        normalized.forEachIndexed { index, value ->
            groups["any-$index"] = listOf(containsMatcher(value))
        }
    }

    if (groups.isEmpty()) {
        throw IllegalArgumentException("At least one non-blank string filter is required")
    }
}

private fun containsMatcher(value: String): StringMatcher =
    StringMatcher(value = value, matchType = StringMatchType.Contains)

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

private fun effectiveMethodFields(fields: Set<String>?, brief: Boolean, handleEnabled: Boolean): Set<String> =
    fields ?: if (brief) {
        if (handleEnabled) setOf("descriptor", "sourcePath", "sourceEntry", "methodHandle")
        else setOf("descriptor", "sourcePath", "sourceEntry")
    } else {
        if (handleEnabled) methodFieldNamesWithHandle else methodFieldNames
    }

private fun effectiveClassFields(fields: Set<String>?, brief: Boolean, handleEnabled: Boolean): Set<String> =
    fields ?: if (brief) {
        if (handleEnabled) setOf("className", "classHandle")
        else setOf("className")
    } else {
        if (handleEnabled) classFieldNamesWithHandle else classFieldNames
    }

private fun effectiveResourceEntryFields(fields: Set<String>?, brief: Boolean): Set<String> =
    fields ?: if (brief) setOf("resourceId", "type", "name") else resourceEntryFieldNames

private fun effectiveResourceValueFields(fields: Set<String>?, brief: Boolean): Set<String> =
    fields ?: if (brief) setOf("resourceId", "type", "name", "value") else resourceValueFieldNames

private fun MethodHit.toProjectedJson(fields: Set<String>, handleProvider: ((MethodHit) -> String)?): JsonObject =
    buildJsonObject {
        if ("className" in fields) put("className", className)
        if ("methodName" in fields) put("methodName", methodName)
        if ("descriptor" in fields) put("descriptor", descriptor)
        if ("sourcePath" in fields && sourcePath != null) put("sourcePath", sourcePath)
        if ("sourceEntry" in fields && sourceEntry != null) put("sourceEntry", sourceEntry)
        if ("methodHandle" in fields) put("methodHandle", requireNotNull(handleProvider) { "methodHandle requires session_id" }(this@toProjectedJson))
    }

private fun ClassHit.toProjectedJson(fields: Set<String>, handleProvider: ((ClassHit) -> String)?): JsonObject =
    buildJsonObject {
        if ("className" in fields) put("className", className)
        if ("sourcePath" in fields && sourcePath != null) put("sourcePath", sourcePath)
        if ("sourceEntry" in fields && sourceEntry != null) put("sourceEntry", sourceEntry)
        if ("classHandle" in fields) put("classHandle", requireNotNull(handleProvider) { "classHandle requires session_id" }(this@toProjectedJson))
    }

private fun TargetSession.sessionIdOrNull(): String? = sessionId.takeIf(String::isNotBlank)

private fun ResourceEntry.toProjectedJson(fields: Set<String>): JsonObject =
    buildJsonObject {
        if ("resourceId" in fields && resourceId != null) put("resourceId", resourceId)
        if ("type" in fields && type != null) put("type", type)
        if ("name" in fields && name != null) put("name", name)
        if ("filePath" in fields && filePath != null) put("filePath", filePath)
        if ("sourcePath" in fields && sourcePath != null) put("sourcePath", sourcePath)
        if ("sourceEntry" in fields && sourceEntry != null) put("sourceEntry", sourceEntry)
        if ("resolution" in fields) put("resolution", resolution.toMcpValue())
    }

private fun ResourceEntryValueHit.toProjectedJson(fields: Set<String>): JsonObject =
    buildJsonObject {
        if ("resourceId" in fields && resourceId != null) put("resourceId", resourceId)
        if ("type" in fields && type != null) put("type", type)
        if ("name" in fields && name != null) put("name", name)
        if ("value" in fields && value != null) put("value", value)
        if ("sourcePath" in fields && sourcePath != null) put("sourcePath", sourcePath)
        if ("sourceEntry" in fields && sourceEntry != null) put("sourceEntry", sourceEntry)
    }

internal fun applyClassWindow(items: List<ClassHit>, offset: Int? = null, limit: Int? = null): WindowedClassHits =
    applyWindowSlice(items, offset, limit) { total, slice ->
        WindowedItems(
            total = total.total,
            offset = total.offset,
            limit = total.limit,
            hasMore = total.hasMore,
            items = slice,
        )
    }

internal fun applyMethodWindow(items: List<MethodHit>, offset: Int? = null, limit: Int? = null): WindowedMethodHits =
    applyWindowSlice(items, offset, limit) { total, slice ->
        WindowedItems(
            total = total.total,
            offset = total.offset,
            limit = total.limit,
            hasMore = total.hasMore,
            items = slice,
        )
    }

internal fun applyResourceEntryWindow(items: List<ResourceEntry>, offset: Int? = null, limit: Int? = null): WindowedResourceEntries =
    applyWindowSlice(items, offset, limit) { total, slice ->
        WindowedItems(
            total = total.total,
            offset = total.offset,
            limit = total.limit,
            hasMore = total.hasMore,
            items = slice,
        )
    }

internal fun applyResourceValueWindow(
    items: List<ResourceEntryValueHit>,
    offset: Int? = null,
    limit: Int? = null,
): WindowedResourceValueHits =
    applyWindowSlice(items, offset, limit) { total, slice ->
        WindowedItems(
            total = total.total,
            offset = total.offset,
            limit = total.limit,
            hasMore = total.hasMore,
            items = slice,
        )
    }

private fun ResourceResolution.toMcpValue(): String =
    when (this) {
        ResourceResolution.TableBacked -> "table-backed"
        ResourceResolution.PathInferred -> "path-inferred"
        ResourceResolution.Unresolved -> "unresolved"
    }

private data class WindowSliceMeta(
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
)

private fun <T, R> applyWindowSlice(
    items: List<T>,
    offset: Int?,
    limit: Int?,
    builder: (WindowSliceMeta, List<T>) -> R,
): R {
    val normalizedOffset = offset ?: 0
    require(normalizedOffset >= 0) { "offset must be non-negative" }
    require(limit == null || limit > 0) { "limit must be positive when specified" }
    if (normalizedOffset >= items.size) {
        val effectiveLimit = limit ?: 0
        return builder(
            WindowSliceMeta(
                total = items.size,
                offset = normalizedOffset,
                limit = effectiveLimit,
                hasMore = false,
            ),
            emptyList(),
        )
    }
    val toIndex = if (limit == null) items.size else minOf(items.size, normalizedOffset + limit)
    val slice = items.subList(normalizedOffset, toIndex)
    return builder(
        WindowSliceMeta(
            total = items.size,
            offset = normalizedOffset,
            limit = limit ?: slice.size,
            hasMore = toIndex < items.size,
        ),
        slice,
    )
}
