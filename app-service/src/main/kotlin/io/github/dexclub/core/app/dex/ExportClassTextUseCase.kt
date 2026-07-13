package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.ExportClassJavaRequest
import io.github.dexclub.core.api.dex.ExportClassSmaliRequest
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService

enum class ExportClassTextView {
    Java,
    Smali,
}

data class ExportClassTextUseCaseRequest(
    val workspace: WorkspaceContext? = null,
    val sessionId: String? = null,
    val workdir: String? = null,
    val classHandle: String? = null,
    val descriptor: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val view: ExportClassTextView,
)

data class ExportClassTextUseCaseResult(
    val session: TargetSession?,
    val workspace: WorkspaceContext,
    val descriptor: String,
    val text: String,
)

class ExportClassTextUseCase(
    private val workspaceService: WorkspaceService,
    private val dexService: DexAnalysisService,
    private val sessionService: TargetSessionService,
) {
    private val support = DexUseCaseSupport(workspaceService, sessionService)

    fun execute(request: ExportClassTextUseCaseRequest): ExportClassTextUseCaseResult {
        val executionContext = support.resolveExecutionContext(
            workspace = request.workspace,
            sessionId = request.sessionId,
            workdir = request.workdir,
        )
        val classRef = support.resolveClassReference(
            session = executionContext.session,
            classHandle = request.classHandle,
            descriptor = request.descriptor,
            sourcePath = request.sourcePath,
            sourceEntry = request.sourceEntry,
        )
        val descriptor = classRef?.descriptor.orEmpty()
        require(descriptor.isNotEmpty()) { "class_handle or descriptor is required" }
        val locator = buildSourceLocator(
            ref = classRef,
            sourcePath = request.sourcePath,
            sourceEntry = request.sourceEntry,
        )
        val text = support.exportTextFile(executionContext.workspace) { output ->
            when (request.view) {
                ExportClassTextView.Java ->
                    dexService.exportClassJava(
                        workspace = executionContext.workspace,
                        request = ExportClassJavaRequest(
                            className = descriptor,
                            source = locator,
                            outputPath = output.toString(),
                        ),
                    )

                ExportClassTextView.Smali ->
                    dexService.exportClassSmali(
                        workspace = executionContext.workspace,
                        request = ExportClassSmaliRequest(
                            className = descriptor,
                            source = locator,
                            outputPath = output.toString(),
                        ),
                    )
            }
        }
        return ExportClassTextUseCaseResult(
            session = executionContext.session,
            workspace = executionContext.workspace,
            descriptor = descriptor,
            text = text,
        )
    }
}
