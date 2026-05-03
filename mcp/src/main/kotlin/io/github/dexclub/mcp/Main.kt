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
import io.ktor.server.request.receiveText
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun main() {
    configureSlf4jSimpleDefaults()
    disableKotlinLoggingStartupMessage()
    DexKitMcpBootstrap.configureNativeLibraryDir()

    val config = loadHttpServerConfig()
    val app = McpApp()
    val server = app.createServer()

    System.err.println(
        "DexClub MCP listening on http://${config.host}:${config.port}${config.path} " +
            "(stateless streamable HTTP, debug=${config.debugHttp})",
    )

    createHttpServer(config, app, server)
        .start(wait = true)
}

internal fun createHttpServer(
    config: HttpServerConfig,
    app: McpApp,
    server: io.modelcontextprotocol.kotlin.sdk.server.Server = app.createServer(),
): EmbeddedServer<*, *> =
    embeddedServer(Netty, host = config.host, port = config.port) {
        if (config.debugHttp) {
            install(DoubleReceive)
            installHttpDebugLogging()
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

internal data class HttpServerConfig(
    val host: String,
    val port: Int,
    val path: String,
    val debugHttp: Boolean,
)

private fun loadHttpServerConfig(): HttpServerConfig =
    HttpServerConfig(
        host = System.getenv("DEXCLUB_MCP_HOST")?.trim()?.ifEmpty { null } ?: "127.0.0.1",
        port = System.getenv("DEXCLUB_MCP_PORT")?.trim()?.toIntOrNull() ?: 8787,
        path = normalizePath(System.getenv("DEXCLUB_MCP_PATH")),
        debugHttp = System.getenv("DEXCLUB_MCP_DEBUG")
            ?.trim()
            ?.equals("true", ignoreCase = true)
            ?: false,
    )

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

private fun Application.installHttpDebugLogging() {
    intercept(ApplicationCallPipeline.Monitoring) {
        val request = context.request
        val startedAt = System.nanoTime()
        val debugInfo = readDebugRequestInfo(context)
        System.err.println(
            "HTTP MCP request: method=${request.httpMethod.value} uri=${request.uri} " +
                "${debugInfo.toLogFields()}" +
                "accept=${request.headers[HttpHeaders.Accept] ?: ""} contentType=${request.headers[HttpHeaders.ContentType] ?: ""} " +
                "protocol=${request.headers["Mcp-Protocol-Version"] ?: ""} session=${request.headers["Mcp-Session-Id"] ?: ""}",
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
                System.err.println(
                    "HTTP MCP response: method=${request.httpMethod.value} uri=${request.uri} " +
                        "${debugInfo.toLogFields()}" +
                        "status=${context.response.status()?.value ?: 200} elapsedMs=$elapsedMs",
                )
            } else {
                System.err.println(
                    "HTTP MCP failure: method=${request.httpMethod.value} uri=${request.uri} " +
                        "${debugInfo.toLogFields()}" +
                        "error=${failure::class.qualifiedName}: ${failure.message.orEmpty()} elapsedMs=$elapsedMs",
                )
            }
        }
    }
}

private suspend fun readDebugRequestInfo(call: ApplicationCall): DebugRequestInfo {
    val body = runCatching { call.receiveText() }.getOrNull()?.takeIf { it.isNotBlank() } ?: return DebugRequestInfo()
    val root = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return DebugRequestInfo()
    val requestId = root["id"]?.jsonPrimitive?.contentOrNull
    val rpcMethod = root["method"]?.jsonPrimitive?.contentOrNull
    val toolName = root["params"]
        ?.jsonObject
        ?.get("name")
        ?.jsonPrimitive
        ?.contentOrNull
        ?.takeIf { rpcMethod == "tools/call" }
    return DebugRequestInfo(requestId = requestId, rpcMethod = rpcMethod, toolName = toolName)
}

private data class DebugRequestInfo(
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
