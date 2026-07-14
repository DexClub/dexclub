package io.github.dexclub.mcp

import io.github.dexclub.core.app.contract.ManifestApplicationInfo
import io.github.dexclub.core.app.contract.ManifestComponentInfo
import io.github.dexclub.core.app.contract.ManifestInspectionResult
import io.github.dexclub.core.app.contract.ManifestIntentData
import io.github.dexclub.core.app.contract.ManifestIntentFilter
import io.github.dexclub.core.app.contract.ManifestMetaData
import io.github.dexclub.core.app.contract.ManifestUsesFeature
import io.github.dexclub.core.app.contract.ManifestUsesSdk
import io.github.dexclub.core.app.contract.ResourceResolution
import io.github.dexclub.core.app.contract.CapabilitySet
import io.github.dexclub.core.app.contract.InventoryCounts
import io.github.dexclub.core.app.projection.ClassHitProjection
import io.github.dexclub.core.app.projection.DecodedXmlProjection
import io.github.dexclub.core.app.projection.FieldHitProjection
import io.github.dexclub.core.app.projection.MethodDetailProjection
import io.github.dexclub.core.app.projection.MethodFieldUsageProjection
import io.github.dexclub.core.app.projection.MethodHitProjection
import io.github.dexclub.core.app.projection.ResourceEntryValueHitProjection
import io.github.dexclub.core.app.projection.ResourcePluralItemProjection
import io.github.dexclub.core.app.projection.ResourceValueProjection
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.contract.TargetHandle
import io.github.dexclub.core.app.contract.TargetSnapshotSummary
import io.github.dexclub.core.app.contract.WorkspaceContext

internal fun TargetSession.toView(): TargetSessionView =
    TargetSessionView(
        sessionId = sessionId,
        createdAt = createdAt,
        workspace = workspace.toView(),
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

internal fun MethodDetailProjection.toView(brief: Boolean = false): MethodDetailView =
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
            usingFields = usingFields?.map(MethodFieldUsageProjection::toView),
            callers = callers?.map(MethodHitProjection::toView),
            invokes = invokes?.map(MethodHitProjection::toView),
            strings = strings,
            annotations = annotations,
        )
    }

internal fun MethodHitProjection.toView(): MethodHitView =
    MethodHitView(
        className = className,
        methodName = methodName,
        descriptor = descriptor,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )

internal fun ClassHitProjection.toView(): ClassHitView =
    ClassHitView(
        className = className,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )

internal fun FieldHitProjection.toView(): FieldHitView =
    FieldHitView(
        className = className,
        fieldName = fieldName,
        descriptor = descriptor,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )

internal fun MethodFieldUsageProjection.toView(): MethodFieldUsageView =
    MethodFieldUsageView(
        usingType = usingType,
        field = field.toView(),
    )

internal fun ResourceValueProjection.toView(): ResourceValueView =
    ResourceValueView(
        resourceId = resourceId,
        type = type,
        name = name,
        value = value,
        pluralItems = pluralItems?.map(ResourcePluralItemProjection::toView),
    )

internal fun ResourcePluralItemProjection.toView(): ResourcePluralItemView =
    ResourcePluralItemView(
        quantity = quantity,
        value = value,
    )

internal fun ResourceEntryValueHitProjection.toView(): ResourceEntryValueHitView =
    ResourceEntryValueHitView(
        resourceId = resourceId,
        type = type,
        name = name,
        value = value,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )

internal fun DecodedXmlProjection.toView(): DecodedXmlView =
    DecodedXmlView(
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
        text = text,
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

internal fun ResourceResolution.toMcpValue(): String =
    when (this) {
        ResourceResolution.TableBacked -> "table-backed"
        ResourceResolution.PathInferred -> "path-inferred"
        ResourceResolution.TableValue -> "table-value"
        ResourceResolution.Unresolved -> "unresolved"
        ResourceResolution.TableHole -> "table-hole"
    }

