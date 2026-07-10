package io.github.dexclub.mcp

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
            accept = request.headers[io.ktor.http.HttpHeaders.Accept],
            contentType = request.headers[io.ktor.http.HttpHeaders.ContentType],
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
