package io.github.dexclub.mcp

import io.github.dexclub.core.app.contract.ClassHit
import io.github.dexclub.core.app.contract.MethodHit
import io.github.dexclub.core.app.projection.toProjection
import io.github.dexclub.core.app.dex.ExportClassTextUseCaseResult
import io.github.dexclub.core.app.dex.ExportMethodTextUseCaseResult
import io.github.dexclub.core.app.dex.FindClassesUsingStringsUseCaseResult
import io.github.dexclub.core.app.dex.FindMethodsUseCaseResult
import io.github.dexclub.core.app.dex.FindMethodsUsingStringsUseCaseResult
import io.github.dexclub.core.app.dex.InspectMethodUseCaseResult
import io.github.dexclub.core.app.resource.DecodeXmlUseCaseResult
import io.github.dexclub.core.app.resource.FindResourceValuesUseCaseResult
import io.github.dexclub.core.app.resource.GetResourceValueUseCaseResult
import io.github.dexclub.core.app.resource.InspectManifestUseCaseResult
import io.github.dexclub.core.app.resource.ListResourcesUseCaseResult
import io.github.dexclub.core.app.session.SessionStoreSnapshot
import io.github.dexclub.core.app.session.TargetSession
import java.time.Duration

internal fun TargetSession.toResult(): OpenTargetSessionResult =
    OpenTargetSessionResult(
        sessionId = sessionId,
        createdAt = createdAt,
        workspace = workspace.toView(),
    )

internal fun SessionStoreSnapshot.toView(): DiagnoseTargetSessionsResult =
    DiagnoseTargetSessionsResult(
        now = now.toString(),
        idleTimeoutSeconds = idleTimeout?.seconds,
        maxSessions = maxSessions,
        maxHandlesPerSession = maxHandlesPerSession,
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

internal fun InspectMethodUseCaseResult.toInspectMethodResult(brief: Boolean): InspectMethodResult =
    InspectMethodResult(
        sessionId = session?.sessionIdOrNull(),
        detail = detail.toProjection().toView(brief = brief),
    )

internal fun InspectManifestUseCaseResult.toManifestDecodeResult(): ManifestDecodeResult =
    ManifestDecodeResult(
        sessionId = session?.sessionIdOrNull(),
        manifest = manifest.toView(),
    )

internal fun FindClassesUsingStringsUseCaseResult.toFindClassesUsingStringsResult(
    handleProvider: ((ClassHit) -> String)? = null,
    fields: Set<String>? = null,
    brief: Boolean = false,
): FindClassesUsingStringsResult =
    FindClassesUsingStringsResult(
        sessionId = session?.sessionIdOrNull(),
        total = total,
        offset = offset,
        limit = limit,
        hasMore = hasMore,
        items = items.map { it.toProjectedJson(effectiveClassFields(fields, brief, handleProvider != null), handleProvider) },
    )

internal fun ExportMethodTextUseCaseResult.toExportTextResult(
    view: String,
): ExportTextResult = ExportTextResult(
    sessionId = session?.sessionIdOrNull(),
    descriptor = descriptor,
    view = view,
    text = text,
)

internal fun ExportClassTextUseCaseResult.toExportTextResult(
    view: String,
): ExportTextResult = ExportTextResult(
    sessionId = session?.sessionIdOrNull(),
    descriptor = descriptor,
    view = view,
    text = text,
)

internal fun FindMethodsUsingStringsUseCaseResult.toFindMethodsUsingStringsResult(
    handleProvider: ((MethodHit) -> String)? = null,
    fields: Set<String>? = null,
    brief: Boolean = false,
): FindMethodsUsingStringsResult =
    FindMethodsUsingStringsResult(
        sessionId = session?.sessionIdOrNull(),
        total = total,
        offset = offset,
        limit = limit,
        hasMore = hasMore,
        items = items.map { it.toProjectedJson(effectiveMethodFields(fields, brief, handleProvider != null), handleProvider) },
    )

internal fun FindMethodsUseCaseResult.toFindMethodsResult(
    handleProvider: ((MethodHit) -> String)? = null,
    fields: Set<String>? = null,
    brief: Boolean = false,
): FindMethodsResult =
    FindMethodsResult(
        sessionId = session?.sessionIdOrNull(),
        total = total,
        offset = offset,
        limit = limit,
        hasMore = hasMore,
        items = items.map { it.toProjectedJson(effectiveMethodFields(fields, brief, handleProvider != null), handleProvider) },
    )

internal fun GetResourceValueUseCaseResult.toResolveResourceResult(): ResolveResourceResult =
    ResolveResourceResult(
        sessionId = session?.sessionIdOrNull(),
        resource = resource.toProjection().toView(),
    )

internal fun ListResourcesUseCaseResult.toListResourcesResult(
    fields: Set<String>? = null,
    brief: Boolean = false,
): ListResourcesResult =
    ListResourcesResult(
        sessionId = session?.sessionIdOrNull(),
        total = total,
        offset = offset,
        limit = limit,
        hasMore = hasMore,
        items = items.map { it.toProjectedJson(effectiveResourceEntryFields(fields, brief)) },
    )

internal fun FindResourceValuesUseCaseResult.toFindResourcesResult(
    fields: Set<String>? = null,
    brief: Boolean = false,
): FindResourcesResult =
    FindResourcesResult(
        sessionId = session?.sessionIdOrNull(),
        total = total,
        offset = offset,
        limit = limit,
        hasMore = hasMore,
        items = items.map { it.toProjectedJson(effectiveResourceValueFields(fields, brief)) },
    )

internal fun DecodeXmlUseCaseResult.toDecodeXmlResult(): DecodeXmlResult =
    DecodeXmlResult(
        sessionId = session?.sessionIdOrNull(),
        xml = xml.toProjection().toView(),
    )

private fun TargetSession.sessionIdOrNull(): String? = sessionId.takeIf(String::isNotBlank)

