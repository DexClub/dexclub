package io.github.dexclub.mcp

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.createDirectories

internal data class TraceRequestInfo(
    val requestId: String? = null,
    val rpcMethod: String? = null,
    val toolName: String? = null,
) {
    fun toLogFields(): String = buildString {
        requestId?.let { append("id=$it ") }
        rpcMethod?.let { append("rpcMethod=$it ") }
        toolName?.let { append("tool=$it ") }
    }
}

private class StartupArchivingTraceLogger(
    private val logFile: Path,
) {
    private val lock = Any()
    private val archiveTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        .withZone(ZoneId.systemDefault())

    init {
        archiveExistingLog()
    }

    fun append(event: String) {
        synchronized(lock) {
            val normalizedEvent = ensureTrailingLineBreak(event)
            createLogDirectory()
            Files.writeString(
                logFile,
                normalizedEvent,
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND,
            )
        }
    }

    private fun archiveExistingLog() {
        synchronized(lock) {
            if (!Files.exists(logFile)) return
            if (Files.size(logFile) == 0L) return

            createLogDirectory()
            val archiveDir = archiveDir()
            archiveDir.createDirectories()
            Files.move(logFile, nextArchiveFile(archiveDir))
        }
    }

    private fun nextArchiveFile(archiveDir: Path): Path {
        val baseName = logFile.fileName.toString().substringBeforeLast('.', logFile.fileName.toString())
        val extension = logFile.fileName.toString().substringAfterLast('.', "").let {
            if (it.isEmpty() || it == logFile.fileName.toString()) "" else ".$it"
        }
        val timestamp = archiveTimestampFormatter.format(Instant.now())
        var candidate = archiveDir.resolve("${baseName}_$timestamp$extension")
        var suffix = 1
        while (Files.exists(candidate)) {
            candidate = archiveDir.resolve("${baseName}_${timestamp}_$suffix$extension")
            suffix += 1
        }
        return candidate
    }

    private fun createLogDirectory() {
        logFile.parent?.createDirectories()
    }

    private fun archiveDir(): Path =
        logFile.parent?.resolve("archive") ?: Paths.get("archive")

    private fun ensureTrailingLineBreak(event: String): String =
        if (event.endsWith(System.lineSeparator())) event else event + System.lineSeparator()
}

internal object McpRuntimeDiagnostics {
    private val currentOperation = AtomicReference<String?>(null)
    private val traceLogger = AtomicReference<StartupArchivingTraceLogger?>(null)
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    fun install(config: HttpServerConfig) {
        traceLogger.set(
            config.traceLogFile?.let {
                StartupArchivingTraceLogger(
                    logFile = it,
                )
            },
        )

        val pid = runCatching { ProcessHandle.current().pid() }.getOrNull()
        val startup = buildString {
            append("DexClub MCP process started")
            pid?.let { append(" pid=$it") }
            append(" crashFiles=./hs_err_pid%p.log heapDumpPath=./")
            config.traceLogFile?.let {
                append(" traceFile=${it.toDisplayPath()}")
            }
        }
        System.err.println(startup)
        trace("PROCESS START: $startup")

        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val message =
                "DexClub MCP uncaught exception: thread=${thread.name} " +
                    "currentOperation=${currentOperation.get().orEmpty()} " +
                    "error=${error::class.qualifiedName}: ${error.message.orEmpty()}"
            System.err.println(message)
            error.printStackTrace(System.err)
            traceStack("UNCAUGHT EXCEPTION: $message", error)
        }
        Runtime.getRuntime().addShutdownHook(
            Thread {
                val message = "DexClub MCP shutdown: currentOperation=${currentOperation.get().orEmpty()}"
                System.err.println(message)
                trace("SHUTDOWN: $message")
            },
        )
    }

    fun toolStarted(toolName: String, summary: String) {
        currentOperation.set("$toolName $summary".trim())
        trace("MCP tool begin: tool=$toolName ${summary.trim()}".trim())
    }

    fun toolFinished(toolName: String, isError: Boolean) {
        trace("MCP tool end: tool=$toolName isError=$isError")
        currentOperation.set(null)
    }

    fun toolFailed(toolName: String, cause: Throwable) {
        val message =
            "MCP tool failure: tool=$toolName " +
                "error=${cause::class.qualifiedName}: ${cause.message.orEmpty()}"
        System.err.println(message)
        cause.printStackTrace(System.err)
        traceStack(message, cause)
        currentOperation.set(null)
    }

    fun httpRequest(
        requestMethod: String,
        uri: String,
        info: TraceRequestInfo,
        accept: String?,
        contentType: String?,
        protocol: String?,
        session: String?,
    ) {
        trace(
            "HTTP MCP request: method=$requestMethod uri=$uri ${info.toLogFields()}" +
                "accept=${accept.orEmpty()} contentType=${contentType.orEmpty()} " +
                "protocol=${protocol.orEmpty()} session=${session.orEmpty()}",
        )
    }

    fun httpResponse(
        requestMethod: String,
        uri: String,
        info: TraceRequestInfo,
        status: Int,
        elapsedMs: Long,
    ) {
        trace(
            "HTTP MCP response: method=$requestMethod uri=$uri ${info.toLogFields()}" +
                "status=$status elapsedMs=$elapsedMs",
        )
    }

    fun httpFailure(
        requestMethod: String,
        uri: String,
        info: TraceRequestInfo,
        cause: Throwable,
        elapsedMs: Long,
    ) {
        traceStack(
            "HTTP MCP failure: method=$requestMethod uri=$uri ${info.toLogFields()}" +
                "error=${cause::class.qualifiedName}: ${cause.message.orEmpty()} elapsedMs=$elapsedMs",
            cause,
        )
    }

    private fun trace(message: String) {
        val logger = traceLogger.get() ?: return
        logger.append("${timestamp()} $message")
    }

    private fun traceStack(message: String, cause: Throwable) {
        val logger = traceLogger.get() ?: return
        val payload = buildString {
            append(timestamp())
            append(' ')
            append(message)
            appendLine()
            append(cause.stackTraceToString())
        }
        logger.append(payload)
    }

    private fun timestamp(): String = formatter.format(Instant.now())
}

private fun Path.toDisplayPath(): String {
    val normalized = toAbsolutePath().normalize()
    val cwd = Paths.get("").toAbsolutePath().normalize()
    return normalized
        .takeIf { it.startsWith(cwd) }
        ?.let { cwd.relativize(it).toString().replace('\\', '/') }
        ?.let { "./$it" }
        ?: normalized.toString()
}
