package io.github.dexclub.cli

import io.github.dexclub.core.app.contract.MethodSmaliMode
import io.github.dexclub.core.app.AppUseCases

internal class ExportCommandAdapter(
    private val targetWorkspaceRuntime: CliTargetWorkspaceRuntime,
    private val appUseCases: AppUseCases,
) {
    fun exportClassDex(request: CliRequest.ExportClassDex): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.dex.exportClassArtifactUseCase.execute(
            io.github.dexclub.core.app.dex.ExportClassArtifactUseCaseRequest(
                workspace = workspace,
                className = request.className,
                sourcePath = request.sourcePath,
                sourceEntry = request.sourceEntry,
                outputPath = request.output,
                view = io.github.dexclub.core.app.dex.ExportClassArtifactView.Dex,
            ),
        )
        return CommandResult(
            payload = RenderPayload.Export(ExportView.from(result.result)),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun exportClassJava(request: CliRequest.ExportClassJava): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.dex.exportClassArtifactUseCase.execute(
            io.github.dexclub.core.app.dex.ExportClassArtifactUseCaseRequest(
                workspace = workspace,
                className = request.className,
                sourcePath = request.sourcePath,
                sourceEntry = request.sourceEntry,
                outputPath = request.output,
                view = io.github.dexclub.core.app.dex.ExportClassArtifactView.Java,
            ),
        )
        return CommandResult(
            payload = RenderPayload.Export(ExportView.from(result.result)),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun exportClassSmali(request: CliRequest.ExportClassSmali): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.dex.exportClassArtifactUseCase.execute(
            io.github.dexclub.core.app.dex.ExportClassArtifactUseCaseRequest(
                workspace = workspace,
                className = request.className,
                sourcePath = request.sourcePath,
                sourceEntry = request.sourceEntry,
                outputPath = request.output,
                autoUnicodeDecode = request.autoUnicodeDecode,
                view = io.github.dexclub.core.app.dex.ExportClassArtifactView.Smali,
            ),
        )
        return CommandResult(
            payload = RenderPayload.Export(ExportView.from(result.result)),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun exportMethodSmali(request: CliRequest.ExportMethodSmali): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.dex.exportMethodArtifactUseCase.execute(
            io.github.dexclub.core.app.dex.ExportMethodArtifactUseCaseRequest(
                workspace = workspace,
                methodSignature = request.methodSignature,
                sourcePath = request.sourcePath,
                sourceEntry = request.sourceEntry,
                outputPath = request.output,
                autoUnicodeDecode = request.autoUnicodeDecode,
                mode = when (request.mode) {
                    "snippet" -> MethodSmaliMode.Snippet
                    "class" -> MethodSmaliMode.Class
                    else -> error("unsupported method smali mode: ${request.mode}")
                },
                view = io.github.dexclub.core.app.dex.ExportMethodArtifactView.Smali,
            ),
        )
        return CommandResult(
            payload = RenderPayload.Export(ExportView.from(result.result)),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun exportMethodDex(request: CliRequest.ExportMethodDex): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.dex.exportMethodArtifactUseCase.execute(
            io.github.dexclub.core.app.dex.ExportMethodArtifactUseCaseRequest(
                workspace = workspace,
                methodSignature = request.methodSignature,
                sourcePath = request.sourcePath,
                sourceEntry = request.sourceEntry,
                outputPath = request.output,
                view = io.github.dexclub.core.app.dex.ExportMethodArtifactView.Dex,
            ),
        )
        return CommandResult(
            payload = RenderPayload.Export(ExportView.from(result.result)),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun exportMethodJava(request: CliRequest.ExportMethodJava): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.dex.exportMethodArtifactUseCase.execute(
            io.github.dexclub.core.app.dex.ExportMethodArtifactUseCaseRequest(
                workspace = workspace,
                methodSignature = request.methodSignature,
                sourcePath = request.sourcePath,
                sourceEntry = request.sourceEntry,
                outputPath = request.output,
                view = io.github.dexclub.core.app.dex.ExportMethodArtifactView.Java,
            ),
        )
        return CommandResult(
            payload = RenderPayload.Export(ExportView.from(result.result)),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }
}

