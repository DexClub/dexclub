package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.ExportMethodDexRequest
import io.github.dexclub.core.api.dex.ExportMethodJavaRequest
import io.github.dexclub.core.api.dex.ExportMethodSmaliRequest
import io.github.dexclub.core.api.dex.ExportResult
import io.github.dexclub.core.api.shared.MethodSmaliMode
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.workspace.WorkspaceContext

enum class ExportMethodArtifactView {
    Dex,
    Java,
    Smali,
}

data class ExportMethodArtifactUseCaseRequest(
    val workspace: WorkspaceContext,
    val methodSignature: String,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val outputPath: String,
    val autoUnicodeDecode: Boolean = true,
    val mode: MethodSmaliMode = MethodSmaliMode.Snippet,
    val view: ExportMethodArtifactView,
)

data class ExportMethodArtifactUseCaseResult(
    val result: ExportResult,
)

class ExportMethodArtifactUseCase(
    private val dexService: DexAnalysisService,
) {
    fun execute(request: ExportMethodArtifactUseCaseRequest): ExportMethodArtifactUseCaseResult {
        val source = SourceLocator(
            sourcePath = request.sourcePath,
            sourceEntry = request.sourceEntry,
        )
        val result = when (request.view) {
            ExportMethodArtifactView.Dex ->
                dexService.exportMethodDex(
                    workspace = request.workspace,
                    request = ExportMethodDexRequest(
                        methodSignature = request.methodSignature,
                        source = source,
                        outputPath = request.outputPath,
                    ),
                )

            ExportMethodArtifactView.Java ->
                dexService.exportMethodJava(
                    workspace = request.workspace,
                    request = ExportMethodJavaRequest(
                        methodSignature = request.methodSignature,
                        source = source,
                        outputPath = request.outputPath,
                    ),
                )

            ExportMethodArtifactView.Smali ->
                dexService.exportMethodSmali(
                    workspace = request.workspace,
                    request = ExportMethodSmaliRequest(
                        methodSignature = request.methodSignature,
                        source = source,
                        outputPath = request.outputPath,
                        autoUnicodeDecode = request.autoUnicodeDecode,
                        mode = request.mode,
                    ),
                )
        }
        return ExportMethodArtifactUseCaseResult(result = result)
    }
}
