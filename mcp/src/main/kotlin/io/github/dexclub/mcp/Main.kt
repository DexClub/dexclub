package io.github.dexclub.mcp

import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.createDirectories

fun main() {
    configureSlf4jSimpleDefaults()
    disableKotlinLoggingStartupMessage()
    DexKitMcpBootstrap.configureNativeLibraryDir()

    val config = loadHttpServerConfig()
    McpRuntimeDiagnostics.install(config)
    val app = McpApp()
    val server = app.createServer()

    System.err.println(
        "DexClub MCP listening on http://${config.host}:${config.port}${config.path} " +
            "(stateless streamable HTTP, trace=${config.traceEnabled})",
    )

    createHttpServer(config, server)
        .start(wait = true)
}

internal data class HttpServerConfig(
    val host: String,
    val port: Int,
    val path: String,
    val traceEnabled: Boolean,
    val traceLogFile: Path?,
)

private fun loadHttpServerConfig(): HttpServerConfig {
    val traceEnabled = System.getenv("DEXCLUB_MCP_TRACE")
        ?.trim()
        ?.equals("false", ignoreCase = true)
        ?.not()
        ?: true
    return HttpServerConfig(
        host = System.getenv("DEXCLUB_MCP_HOST")?.trim()?.ifEmpty { null } ?: "127.0.0.1",
        port = System.getenv("DEXCLUB_MCP_PORT")?.trim()?.toIntOrNull() ?: 8787,
        path = normalizePath(System.getenv("DEXCLUB_MCP_PATH")),
        traceEnabled = traceEnabled,
        traceLogFile = if (traceEnabled) Paths.get("logs", "mcp.log") else null,
    )
}

private fun normalizePath(rawPath: String?): String {
    val path = rawPath?.trim()?.ifEmpty { null } ?: "/mcp"
    return if (path.startsWith('/')) path else "/$path"
}

private fun configureSlf4jSimpleDefaults() {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error")
}

private fun disableKotlinLoggingStartupMessage() {
    runCatching {
        val configClass = Class.forName("io.github.oshai.kotlinlogging.KotlinLoggingConfiguration")
        val instance = configClass.getField("INSTANCE").get(null)
        configClass.getMethod("setLogStartupMessage", Boolean::class.javaPrimitiveType)
            .invoke(instance, false)
    }
}

internal fun createHttpServer(
    config: HttpServerConfig,
    server: Server,
): EmbeddedServer<*, *> =
    embeddedServer(Netty, host = config.host, port = config.port) {
        if (config.traceEnabled) {
            install(DoubleReceive)
            installHttpTraceLogging()
        }
        install(ContentNegotiation) {
            json(McpJson)
        }
        routing {
            post(config.path) {
                val transport = StreamableHttpServerTransport(
                    StreamableHttpServerTransport.Configuration(enableJsonResponse = true),
                ).also { it.setSessionIdGenerator(null) }
                server.createSession(transport)
                transport.handlePostRequest(session = null, call = CompatibleAcceptingCall(call))
            }
        }
    }

private fun Application.installHttpTraceLogging() {
    intercept(ApplicationCallPipeline.Monitoring) {
        val request = context.request
        val startedAt = System.nanoTime()
        val info = readTraceRequestInfo(context)
        McpRuntimeDiagnostics.httpRequest(
            requestMethod = request.httpMethod.value,
            uri = request.uri,
            info = info,
            accept = request.headers[HttpHeaders.Accept],
            contentType = request.headers[HttpHeaders.ContentType],
            protocol = request.headers["Mcp-Protocol-Version"],
            session = request.headers["Mcp-Session-Id"],
        )
        var failure: Throwable? = null
        try {
            proceed()
        } catch (cause: Throwable) {
            failure = cause
            throw cause
        } finally {
            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
            if (failure == null) {
                McpRuntimeDiagnostics.httpResponse(
                    requestMethod = request.httpMethod.value,
                    uri = request.uri,
                    info = info,
                    status = context.response.status()?.value ?: 200,
                    elapsedMs = elapsedMs,
                )
            } else {
                McpRuntimeDiagnostics.httpFailure(
                    requestMethod = request.httpMethod.value,
                    uri = request.uri,
                    info = info,
                    cause = failure,
                    elapsedMs = elapsedMs,
                )
            }
        }
    }
}

private suspend fun readTraceRequestInfo(call: ApplicationCall): TraceRequestInfo {
    val body = runCatching { call.receiveText() }.getOrNull()?.takeIf { it.isNotBlank() } ?: return TraceRequestInfo()
    val root = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return TraceRequestInfo()
    val requestId = root["id"]?.jsonPrimitive?.contentOrNull
    val rpcMethod = root["method"]?.jsonPrimitive?.contentOrNull
    val toolName = root["params"]
        ?.jsonObject
        ?.get("name")
        ?.jsonPrimitive
        ?.contentOrNull
        ?.takeIf { rpcMethod == "tools/call" }
    return TraceRequestInfo(requestId = requestId, rpcMethod = rpcMethod, toolName = toolName)
}

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

private class CompatibleAcceptingCall(
    private val delegate: ApplicationCall,
) : ApplicationCall by delegate {
    override val request: ApplicationRequest = CompatibleAcceptingRequest(delegate.request, this)
}

private class CompatibleAcceptingRequest(
    private val delegate: ApplicationRequest,
    private val callDelegate: ApplicationCall,
) : ApplicationRequest by delegate {
    override val headers: Headers = CompatibleAcceptingHeaders(delegate.headers)
    override val call: ApplicationCall
        get() = callDelegate
}

private class CompatibleAcceptingHeaders(
    private val delegate: Headers,
) : Headers {
    override val caseInsensitiveName: Boolean
        get() = delegate.caseInsensitiveName

    override fun get(name: String): String? =
        if (name.equals(HttpHeaders.Accept, ignoreCase = true)) {
            compatibleAccept(delegate[HttpHeaders.Accept])
        } else {
            delegate[name]
        }

    override fun getAll(name: String): List<String>? =
        if (name.equals(HttpHeaders.Accept, ignoreCase = true)) {
            listOf(compatibleAccept(delegate[HttpHeaders.Accept]))
        } else {
            delegate.getAll(name)
        }

    override fun names(): Set<String> = delegate.names()

    override fun entries(): Set<Map.Entry<String, List<String>>> =
        delegate.entries().mapTo(linkedSetOf()) { entry ->
            if (entry.key.equals(HttpHeaders.Accept, ignoreCase = true)) {
                object : Map.Entry<String, List<String>> {
                    override val key: String = entry.key
                    override val value: List<String> = listOf(compatibleAccept(entry.value.firstOrNull()))
                }
            } else {
                entry
            }
        }

    override fun isEmpty(): Boolean = delegate.isEmpty()

    override fun contains(name: String): Boolean =
        name.equals(HttpHeaders.Accept, ignoreCase = true) || delegate.contains(name)

    override fun contains(name: String, value: String): Boolean =
        getAll(name)?.any { it.equals(value, ignoreCase = true) } == true

    override fun forEach(body: (String, List<String>) -> Unit) {
        entries().forEach { body(it.key, it.value) }
    }
}

private fun compatibleAccept(original: String?): String =
    buildString {
        val base = original?.trim().orEmpty()
        if (base.isNotEmpty()) {
            append(base)
        }
        if (!base.contains("application/json", ignoreCase = true)) {
            if (isNotEmpty()) append(", ")
            append("application/json")
        }
        if (!base.contains("text/event-stream", ignoreCase = true)) {
            if (isNotEmpty()) append(", ")
            append("text/event-stream")
        }
    }
