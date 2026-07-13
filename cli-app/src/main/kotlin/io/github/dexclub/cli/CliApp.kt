package io.github.dexclub.cli

import io.github.dexclub.core.app.contract.DexQueryError
import io.github.dexclub.core.app.contract.DexInspectError
import io.github.dexclub.core.app.contract.DexExportError
import io.github.dexclub.core.app.contract.CapabilityError
import io.github.dexclub.core.app.contract.Operation
import io.github.dexclub.core.app.contract.Services
import io.github.dexclub.core.app.contract.ResourceDecodeError
import io.github.dexclub.core.app.contract.WorkspaceInitError
import io.github.dexclub.core.app.contract.WorkspaceResolveError
import io.github.dexclub.core.app.contract.WorkspaceResolveErrorReason
import io.github.dexclub.core.app.AppRuntime
import io.github.dexclub.core.app.createAppRuntime
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Paths

class CliApp(
    private val runtime: AppRuntime = createAppRuntime(),
    private val cwdProvider: () -> String = ::defaultWorkingDirectory,
    private val debugStacktraceEnabled: Boolean = defaultDebugStacktraceEnabled(),
) {
    constructor(
        services: Services,
        cwdProvider: () -> String = ::defaultWorkingDirectory,
        debugStacktraceEnabled: Boolean = defaultDebugStacktraceEnabled(),
    ) : this(
        runtime = createAppRuntime(services),
        cwdProvider = cwdProvider,
        debugStacktraceEnabled = debugStacktraceEnabled,
    )

    private val parser = CliParser()
    private val workdirResolver = WorkdirResolver(cwdProvider)
    private val targetWorkspaceRuntime = CliTargetWorkspaceRuntime(
        workdirResolver = workdirResolver,
        workspaceRuntime = runtime.workspaceRuntime,
    )
    private val appUseCases = runtime.appUseCases
    private val queryTextLoader = QueryTextLoader()
    private val dispatcher = CommandDispatcher(
        workspace = WorkspaceCommandAdapter(targetWorkspaceRuntime, appUseCases),
        inspect = InspectCommandAdapter(targetWorkspaceRuntime, appUseCases),
        resource = ResourceCommandAdapter(queryTextLoader, targetWorkspaceRuntime, appUseCases),
        dexSearch = DexSearchCommandAdapter(queryTextLoader, targetWorkspaceRuntime, appUseCases),
        export = ExportCommandAdapter(targetWorkspaceRuntime, appUseCases),
    )
    private val renderer = Renderer()
    private val outputWriter = OutputWriter()

    fun run(argv: List<String>, stdout: Appendable, stderr: Appendable): Int {
        var parsedRequest: CliRequest? = null
        val rendered = try {
            parsedRequest = parser.parse(argv)
            val commandResult = dispatcher.dispatch(parsedRequest)
            renderer.render(commandResult)
        } catch (error: Throwable) {
            renderFailure(parsedRequest, error)
        }

        outputWriter.write(rendered, stdout, stderr)
        return rendered.exitCode
    }

    private fun renderFailure(parsedRequest: CliRequest?, error: Throwable): RenderedOutput =
        when (error) {
            is CliUsageError -> renderer.renderCliUsageError(error)
            is WorkspaceResolveError -> workspaceError(
                message = error.message,
                defaultMessage = "Workspace operation failed",
                hint = error.hint(),
            )
            is WorkspaceInitError -> workspaceError(
                message = error.message,
                defaultMessage = "Workspace initialization failed",
            )
            is CapabilityError -> workspaceError(
                message = capabilityErrorMessage(parsedRequest, error),
                defaultMessage = "Capability check failed",
            )
            is DexQueryError -> workspaceError(
                message = error.message,
                defaultMessage = "Dex query failed",
            )
            is DexInspectError -> workspaceError(
                message = error.message,
                defaultMessage = "Method inspection failed",
            )
            is DexExportError -> workspaceError(
                message = error.message,
                defaultMessage = "Dex export failed",
            )
            is ResourceDecodeError -> workspaceError(
                message = error.message,
                defaultMessage = "Resource decode failed",
            )
            else -> workspaceError(
                message = error.message,
                defaultMessage = "Unexpected failure",
                hint = unexpectedFailureHint(),
                details = error.stackTraceText().takeIf { debugStacktraceEnabled },
            )
        }

    private fun workspaceError(
        message: String?,
        defaultMessage: String,
        hint: String? = null,
        details: String? = null,
    ): RenderedOutput =
        renderer.renderWorkspaceError(
            message = message ?: defaultMessage,
            hint = hint,
            details = details,
        )

    private fun capabilityErrorMessage(parsedRequest: CliRequest?, error: CapabilityError): String =
        "command '${parsedRequest?.toCommandName() ?: error.operation.toCommandName()}' is not supported by the current workspace (kind=${error.kind})"

    private fun unexpectedFailureHint(): String =
        if (debugStacktraceEnabled) {
            "Unexpected internal error. Disable $DEBUG_STACKTRACE_ENV or unset it to hide stack traces."
        } else {
            "Set $DEBUG_STACKTRACE_ENV=true to print the stack trace."
        }
}

private fun Throwable.stackTraceText(): String =
    StringWriter().use { buffer ->
        PrintWriter(buffer).use { writer ->
            printStackTrace(writer)
        }
        buffer.toString().trimEnd()
    }

private const val DEBUG_STACKTRACE_ENV = "DEXCLUB_CLI_DEBUG_STACKTRACE"

private fun defaultWorkingDirectory(): String =
    Paths.get("").toAbsolutePath().normalize().toString()

private fun defaultDebugStacktraceEnabled(): Boolean =
    System.getenv(DEBUG_STACKTRACE_ENV)
        ?.trim()
        ?.equals("true", ignoreCase = true)
        ?: false

private fun WorkspaceResolveError.hint(): String? =
    when (reason) {
        WorkspaceResolveErrorReason.NotInitialized ->
            "Run 'cli init <input>' to create a workspace."

        WorkspaceResolveErrorReason.MissingBoundInput ->
            "Run 'cli status' for workspace details."

        else -> null
    }

private fun CliRequest.toCommandName(): String =
    when (this) {
        is CliRequest.Help -> "help"
        is CliRequest.Version -> "version"
        is CliRequest.Init -> "init"
        is CliRequest.Switch -> "switch"
        is CliRequest.Targets -> "targets"
        is CliRequest.Status -> "status"
        is CliRequest.Gc -> "gc"
        is CliRequest.Refresh -> "refresh"
        is CliRequest.Inspect -> "inspect"
        is CliRequest.InspectMethod -> "inspect-method"
        is CliRequest.Manifest -> "manifest"
        is CliRequest.ResTable -> "res-table"
        is CliRequest.DecodeXml -> "decode-xml"
        is CliRequest.ListRes -> "list-res"
        is CliRequest.GetResValue -> "get-res-value"
        is CliRequest.FindResValues -> "find-res-values"
        is CliRequest.FindClass -> "find-class"
        is CliRequest.FindMethod -> "find-method"
        is CliRequest.FindField -> "find-field"
        is CliRequest.FindClassUsingStrings -> "find-class-using-strings"
        is CliRequest.FindMethodUsingStrings -> "find-method-using-strings"
        is CliRequest.ExportClassDex -> "export-class-dex"
        is CliRequest.ExportClassJava -> "export-class-java"
        is CliRequest.ExportClassSmali -> "export-class-smali"
        is CliRequest.ExportMethodSmali -> "export-method-smali"
        is CliRequest.ExportMethodDex -> "export-method-dex"
        is CliRequest.ExportMethodJava -> "export-method-java"
    }

private fun Operation.toCommandName(): String =
    when (this) {
        Operation.Inspect -> "inspect"
        Operation.FindClass -> "find-class"
        Operation.FindMethod -> "find-method"
        Operation.FindField -> "find-field"
        Operation.ExportDex -> "export-class-dex"
        Operation.ExportSmali -> "export-class-smali"
        Operation.ExportJava -> "export-class-java"
        Operation.ManifestDecode -> "manifest"
        Operation.ResourceTableDecode -> "res-table"
        Operation.XmlDecode -> "decode-xml"
        Operation.ResourceEntryList -> "list-res"
    }

