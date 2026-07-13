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
        val result = appUseCases.dex.findClassesByQueryUseCase.execute(
            io.github.dexclub.core.app.dex.FindClassesByQueryUseCaseRequest(
                workspace = workspace,
                queryText = queryText,
                window = request.window,
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
        val result = appUseCases.dex.findMethodsByQueryUseCase.execute(
            io.github.dexclub.core.app.dex.FindMethodsByQueryUseCaseRequest(
                workspace = workspace,
                queryText = queryText,
                window = request.window,
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
        val result = appUseCases.dex.findFieldsByQueryUseCase.execute(
            io.github.dexclub.core.app.dex.FindFieldsByQueryUseCaseRequest(
                workspace = workspace,
                queryText = queryText,
                window = request.window,
            ),
        )
        return CommandResult(
            payload = RenderPayload.FieldHits(result.items.map { FieldHitView.from(it.toProjection()) }),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun findClassUsingStrings(request: CliRequest.FindClassUsingStrings): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val queryText = queryTextLoader.load(request.query, CliUsages.findClassUsingStrings)
        val result = appUseCases.dex.findClassesUsingStringsByQueryUseCase.execute(
            io.github.dexclub.core.app.dex.FindClassesUsingStringsByQueryUseCaseRequest(
                workspace = workspace,
                queryText = queryText,
                window = request.window,
            ),
        )
        return CommandResult(
            payload = RenderPayload.ClassHits(result.items.map { ClassHitView.from(it.toProjection()) }),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun findMethodUsingStrings(request: CliRequest.FindMethodUsingStrings): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val queryText = queryTextLoader.load(request.query, CliUsages.findMethodUsingStrings)
        val result = appUseCases.dex.findMethodsUsingStringsByQueryUseCase.execute(
            io.github.dexclub.core.app.dex.FindMethodsUsingStringsByQueryUseCaseRequest(
                workspace = workspace,
                queryText = queryText,
                window = request.window,
            ),
        )
        return CommandResult(
            payload = RenderPayload.MethodHits(result.items.map { MethodHitView.from(it.toProjection()) }),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }
}
