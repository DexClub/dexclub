package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.ExportClassDexRequest
import io.github.dexclub.core.api.dex.ExportClassJavaRequest
import io.github.dexclub.core.api.dex.ExportClassSmaliRequest
import io.github.dexclub.core.api.dex.ExportResult
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.workspace.WorkspaceContext

enum class ExportClassArtifactView {
    Dex,
    Java,
    Smali,
}

data class ExportClassArtifactUseCaseRequest(
    val workspace: WorkspaceContext,
    val className: String,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val outputPath: String,
    val autoUnicodeDecode: Boolean = true,
    val view: ExportClassArtifactView,
)

data class ExportClassArtifactUseCaseResult(
    val result: ExportResult,
)

class ExportClassArtifactUseCase(
    private val dexService: DexAnalysisService,
) {
    fun execute(request: ExportClassArtifactUseCaseRequest): ExportClassArtifactUseCaseResult {
        val source = SourceLocator(
            sourcePath = request.sourcePath,
            sourceEntry = request.sourceEntry,
        )
        val result = when (request.view) {
            ExportClassArtifactView.Dex ->
                dexService.exportClassDex(
                    workspace = request.workspace,
                    request = ExportClassDexRequest(
                        className = request.className,
                        source = source,
                        outputPath = request.outputPath,
                    ),
                )

            ExportClassArtifactView.Java ->
                dexService.exportClassJava(
                    workspace = request.workspace,
                    request = ExportClassJavaRequest(
                        className = request.className,
                        source = source,
                        outputPath = request.outputPath,
                    ),
                )

            ExportClassArtifactView.Smali ->
                dexService.exportClassSmali(
                    workspace = request.workspace,
                    request = ExportClassSmaliRequest(
                        className = request.className,
                        source = source,
                        outputPath = request.outputPath,
                        autoUnicodeDecode = request.autoUnicodeDecode,
                    ),
                )
        }
        return ExportClassArtifactUseCaseResult(result = result)
    }
}
