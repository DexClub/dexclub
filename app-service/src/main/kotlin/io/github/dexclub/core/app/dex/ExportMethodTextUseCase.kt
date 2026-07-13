package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.ExportMethodJavaRequest
import io.github.dexclub.core.api.dex.ExportMethodSmaliRequest
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService

enum class ExportMethodTextView {
    Java,
    Smali,
}

data class ExportMethodTextUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val methodHandle: String? = null,
    val descriptor: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val view: ExportMethodTextView,
    val mode: String? = null,
)

data class ExportMethodTextUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val descriptor: String,
    val text: String,
)

class ExportMethodTextUseCase(
    private val workspaceService: WorkspaceService,
    private val dexService: DexAnalysisService,
    private val sessionService: TargetSessionService,
) {
    private val support = DexUseCaseSupport(workspaceService, sessionService)

    fun execute(request: ExportMethodTextUseCaseRequest): ExportMethodTextUseCaseResult {
        val executionContext = support.resolveExecutionContext(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        val methodRef = support.resolveMethodReference(
            session = executionContext.session,
            methodHandle = request.methodHandle,
            descriptor = request.descriptor,
            sourcePath = request.sourcePath,
            sourceEntry = request.sourceEntry,
        )
        val descriptor = methodRef?.descriptor.orEmpty()
        require(descriptor.isNotEmpty()) { "method_handle or descriptor is required" }
        val locator = buildSourceLocator(
            ref = methodRef,
            sourcePath = request.sourcePath,
            sourceEntry = request.sourceEntry,
        )
        val text = support.exportTextFile(executionContext.workspace) { output ->
            when (request.view) {
                ExportMethodTextView.Java -> {
                    require(request.mode.isNullOrBlank()) { "mode is only supported for export_method_smali" }
                    dexService.exportMethodJava(
                        workspace = executionContext.workspace,
                        request = ExportMethodJavaRequest(
                            methodSignature = descriptor,
                            source = locator,
                            outputPath = output.toString(),
                        ),
                    )
                }

                ExportMethodTextView.Smali -> {
                    dexService.exportMethodSmali(
                        workspace = executionContext.workspace,
                        request = ExportMethodSmaliRequest(
                            methodSignature = descriptor,
                            source = locator,
                            outputPath = output.toString(),
                            mode = request.mode.toMethodSmaliMode(),
                        ),
                    )
                }
            }
        }
        return ExportMethodTextUseCaseResult(
            session = executionContext.session,
            workspace = executionContext.workspace,
            descriptor = descriptor,
            text = text,
        )
    }
}
