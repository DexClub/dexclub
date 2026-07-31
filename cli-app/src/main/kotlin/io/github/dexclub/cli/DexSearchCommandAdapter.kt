package io.github.dexclub.cli

import io.github.dexclub.core.app.AppUseCases
import io.github.dexclub.core.app.projection.toProjection

internal class DexSearchCommandAdapter(
    private val queryTextLoader: QueryTextLoader,
    private val targetWorkspaceRuntime: CliTargetWorkspaceRuntime,
    private val appUseCases: AppUseCases,
) {
    fun findClass(request: CliRequest.FindClass): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val queryText = queryTextLoader.load(request.query, CliUsages.findClass)
        val result = appUseCases.dex.findClassesUseCase.execute(
            io.github.dexclub.core.app.dex.FindClassesUseCaseRequest(
                workspace = workspace,
                queryText = queryText,
                offset = request.window.offset,
                limit = request.window.limit,
            ),
        )
        return CommandResult(
            payload = RenderPayload.ClassHits(result.items.map { ClassHitView.from(it.toProjection()) }),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun findMethod(request: CliRequest.FindMethod): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val queryText = queryTextLoader.load(request.query, CliUsages.findMethod)
        val result = appUseCases.dex.findMethodsUseCase.execute(
            io.github.dexclub.core.app.dex.FindMethodsUseCaseRequest(
                workspace = workspace,
                queryText = queryText,
                offset = request.window.offset,
                limit = request.window.limit,
            ),
        )
        return CommandResult(
            payload = RenderPayload.MethodHits(result.items.map { MethodHitView.from(it.toProjection()) }),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun findField(request: CliRequest.FindField): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val queryText = queryTextLoader.load(request.query, CliUsages.findField)
        val result = appUseCases.dex.findFieldsUseCase.execute(
            io.github.dexclub.core.app.dex.FindFieldsUseCaseRequest(
                workspace = workspace,
                queryText = queryText,
                offset = request.window.offset,
                limit = request.window.limit,
            ),
        )
        return CommandResult(
            payload = RenderPayload.FieldHits(result.items.map { FieldHitView.from(it.toProjection()) }),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

}
