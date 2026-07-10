package io.github.dexclub.mcp

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.FieldHit
import io.github.dexclub.core.api.dex.MethodDetail
import io.github.dexclub.core.api.dex.MethodFieldUsage
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.resource.DecodedXmlResult
import io.github.dexclub.core.api.resource.ManifestApplicationInfo
import io.github.dexclub.core.api.resource.ManifestComponentInfo
import io.github.dexclub.core.api.resource.ManifestInspectionResult
import io.github.dexclub.core.api.resource.ManifestIntentData
import io.github.dexclub.core.api.resource.ManifestIntentFilter
import io.github.dexclub.core.api.resource.ManifestMetaData
import io.github.dexclub.core.api.resource.ManifestUsesFeature
import io.github.dexclub.core.api.resource.ManifestUsesSdk
import io.github.dexclub.core.api.resource.ResourceResolution
import io.github.dexclub.core.api.shared.CapabilitySet
import io.github.dexclub.core.api.shared.InventoryCounts
import io.github.dexclub.core.api.workspace.TargetHandle
import io.github.dexclub.core.api.workspace.TargetSnapshotSummary
import io.github.dexclub.core.api.workspace.WorkspaceContext
import java.time.Duration

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

private fun TargetSession.sessionIdOrNull(): String? = sessionId.takeIf(String::isNotBlank)

internal fun ResourceResolution.toMcpValue(): String =
    when (this) {
        ResourceResolution.TableBacked -> "table-backed"
        ResourceResolution.PathInferred -> "path-inferred"
        ResourceResolution.Unresolved -> "unresolved"
    }
