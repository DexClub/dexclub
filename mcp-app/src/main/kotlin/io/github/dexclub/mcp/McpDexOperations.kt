package io.github.dexclub.mcp

import io.github.dexclub.core.app.contract.ClassHit
import io.github.dexclub.core.app.contract.MethodDetailSection
import io.github.dexclub.core.app.contract.MethodHit
import io.github.dexclub.core.app.contract.SourceLocator
import io.github.dexclub.core.app.contract.WorkspaceContext
import io.github.dexclub.core.app.dex.FindClassesUseCaseResult
import io.github.dexclub.core.app.dex.FindFieldsUseCaseResult
import io.github.dexclub.core.app.dex.FindMethodsUseCaseResult
import io.github.dexclub.core.app.dex.InspectMethodUseCaseResult
import kotlinx.serialization.json.JsonObject

internal fun McpApp.inspectMethod(
    workspace: WorkspaceContext,
    descriptor: String,
    source: SourceLocator = SourceLocator(),
    includes: Set<MethodDetailSection>,
) = appUseCases.dex.inspectMethodUseCase.execute(
    io.github.dexclub.core.app.dex.InspectMethodUseCaseRequest(
        workspace = workspace,
        descriptor = descriptor,
        sourcePath = source.sourcePath,
        sourceEntry = source.sourceEntry,
        includes = includes,
    ),
).detail

internal fun McpApp.inspectMethodExecution(
    sessionId: String?,
    workdir: String?,
    methodHandle: String?,
    descriptor: String?,
    sourcePath: String?,
    sourceEntry: String?,
    includes: Set<MethodDetailSection>,
): InspectMethodUseCaseResult =
    appUseCases.dex.inspectMethodUseCase.execute(
        io.github.dexclub.core.app.dex.InspectMethodUseCaseRequest(
            sessionId = sessionId,
            workdir = workdir,
            methodHandle = methodHandle,
            descriptor = descriptor,
            sourcePath = sourcePath,
            sourceEntry = sourceEntry,
            includes = includes,
        ),
    )

internal fun McpApp.exportMethodJavaText(
    workspace: WorkspaceContext,
    descriptor: String,
    source: SourceLocator = SourceLocator(),
    mode: String? = null,
): String =
    appUseCases.dex.exportMethodTextUseCase.execute(
        io.github.dexclub.core.app.dex.ExportMethodTextUseCaseRequest(
            workspace = workspace,
            descriptor = descriptor,
            sourcePath = source.sourcePath,
            sourceEntry = source.sourceEntry,
            view = io.github.dexclub.core.app.dex.ExportMethodTextView.Java,
            mode = mode,
        ),
    ).text

internal fun McpApp.exportMethodSmaliText(
    workspace: WorkspaceContext,
    descriptor: String,
    source: SourceLocator = SourceLocator(),
    mode: String? = null,
): String =
    appUseCases.dex.exportMethodTextUseCase.execute(
        io.github.dexclub.core.app.dex.ExportMethodTextUseCaseRequest(
            workspace = workspace,
            descriptor = descriptor,
            sourcePath = source.sourcePath,
            sourceEntry = source.sourceEntry,
            view = io.github.dexclub.core.app.dex.ExportMethodTextView.Smali,
            mode = mode,
        ),
    ).text

internal fun McpApp.exportClassJavaText(
    workspace: WorkspaceContext,
    descriptor: String,
    source: SourceLocator = SourceLocator(),
): String =
    appUseCases.dex.exportClassTextUseCase.execute(
        io.github.dexclub.core.app.dex.ExportClassTextUseCaseRequest(
            workspace = workspace,
            descriptor = descriptor,
            sourcePath = source.sourcePath,
            sourceEntry = source.sourceEntry,
            view = io.github.dexclub.core.app.dex.ExportClassTextView.Java,
        ),
    ).text

internal fun McpApp.exportClassSmaliText(
    workspace: WorkspaceContext,
    descriptor: String,
    source: SourceLocator = SourceLocator(),
): String =
    appUseCases.dex.exportClassTextUseCase.execute(
        io.github.dexclub.core.app.dex.ExportClassTextUseCaseRequest(
            workspace = workspace,
            descriptor = descriptor,
            sourcePath = source.sourcePath,
            sourceEntry = source.sourceEntry,
            view = io.github.dexclub.core.app.dex.ExportClassTextView.Smali,
        ),
    ).text

internal fun McpApp.findClasses(
    workspace: WorkspaceContext,
    query: JsonObject,
    offset: Int? = null,
    limit: Int? = null,
): FindClassesUseCaseResult =
    appUseCases.dex.findClassesUseCase.execute(
        io.github.dexclub.core.app.dex.FindClassesUseCaseRequest(
            workspace = workspace,
            queryText = query.toString(),
            offset = offset,
            limit = limit,
        ),
    )

internal fun McpApp.findMethods(
    workspace: WorkspaceContext,
    query: JsonObject,
    offset: Int? = null,
    limit: Int? = null,
): FindMethodsUseCaseResult =
    appUseCases.dex.findMethodsUseCase.execute(
        io.github.dexclub.core.app.dex.FindMethodsUseCaseRequest(
            workspace = workspace,
            queryText = query.toString(),
            offset = offset,
            limit = limit,
        ),
    )

internal fun McpApp.findFields(
    workspace: WorkspaceContext,
    query: JsonObject,
    offset: Int? = null,
    limit: Int? = null,
): FindFieldsUseCaseResult =
    appUseCases.dex.findFieldsUseCase.execute(
        io.github.dexclub.core.app.dex.FindFieldsUseCaseRequest(
            workspace = workspace,
            queryText = query.toString(),
            offset = offset,
            limit = limit,
        ),
    )

internal fun McpApp.findClassesExecution(
    sessionId: String?,
    workdir: String?,
    query: JsonObject,
    offset: Int? = null,
    limit: Int? = null,
): FindClassesUseCaseResult =
    appUseCases.dex.findClassesUseCase.execute(
        io.github.dexclub.core.app.dex.FindClassesUseCaseRequest(
            sessionId = sessionId,
            workdir = workdir,
            queryText = query.toString(),
            offset = offset,
            limit = limit,
        ),
    )

internal fun McpApp.findMethodsExecution(
    sessionId: String?,
    workdir: String?,
    query: JsonObject,
    offset: Int? = null,
    limit: Int? = null,
): FindMethodsUseCaseResult =
    appUseCases.dex.findMethodsUseCase.execute(
        io.github.dexclub.core.app.dex.FindMethodsUseCaseRequest(
            sessionId = sessionId,
            workdir = workdir,
            queryText = query.toString(),
            offset = offset,
            limit = limit,
        ),
    )

internal fun McpApp.findFieldsExecution(
    sessionId: String?,
    workdir: String?,
    query: JsonObject,
    offset: Int? = null,
    limit: Int? = null,
): FindFieldsUseCaseResult =
    appUseCases.dex.findFieldsUseCase.execute(
        io.github.dexclub.core.app.dex.FindFieldsUseCaseRequest(
            sessionId = sessionId,
            workdir = workdir,
            queryText = query.toString(),
            offset = offset,
            limit = limit,
        ),
    )

