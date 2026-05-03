package io.github.dexclub.mcp

import io.github.dexclub.core.api.shared.Services
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.EmbeddedServer
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.ServerSocket
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class McpHttpSmokeTest {
    private val json = Json { ignoreUnknownKeys = true }
    private var engine: EmbeddedServer<*, *>? = null

    @AfterTest
    fun tearDown() {
        engine?.stop(0, 0)
        engine = null
    }

    @Test
    fun httpServerSupportsInitializeToolsListAndOpenTargetSession() = runBlocking {
        smoke(debugHttp = false)
    }

    @Test
    fun httpServerSupportsInitializeToolsListAndOpenTargetSessionWithDebugEnabled() = runBlocking {
        smoke(debugHttp = true)
    }

    private suspend fun smoke(debugHttp: Boolean) {
        val app = McpApp(
            services = Services(
                workspace = FakeWorkspaceService(fakeWorkspaceContext()),
                dex = FakeDexAnalysisService(),
                resource = FakeResourceService(),
            ),
            sessionStore = McpSessionStore(),
        )
        val port = allocateTestPort()
        val server = app.createServer()
        engine = createHttpServer(
            config = HttpServerConfig(
                host = "127.0.0.1",
                port = port,
                path = "/mcp",
                debugHttp = debugHttp,
            ),
            app = app,
            server = server,
        ).also { it.start(wait = false) }

        val baseUrl = "http://127.0.0.1:$port/mcp"
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        client.use {
            val initializeResponse = it.post(baseUrl) {
                headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                accept(ContentType.Application.Json)
                headers.append("Mcp-Protocol-Version", "2025-11-25")
                setBody(
                    """
                    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"smoke-test","version":"dev"}}}
                    """.trimIndent(),
                )
            }
            assertEquals(HttpStatusCode.OK, initializeResponse.status)
            assertEquals(ContentType.Application.Json.toString(), initializeResponse.headers[HttpHeaders.ContentType])
            val initializeBody = json.parseToJsonElement(initializeResponse.bodyAsText()).jsonObject
            assertEquals("2.0", initializeBody["jsonrpc"]!!.jsonPrimitive.content)
            assertEquals("dexclub-mcp", initializeBody["result"]!!.jsonObject["serverInfo"]!!.jsonObject["name"]!!.jsonPrimitive.content)

            val listToolsResponse = it.post(baseUrl) {
                headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                accept(ContentType.Application.Json)
                headers.append("Mcp-Protocol-Version", "2025-11-25")
                setBody("""{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}""")
            }
            assertEquals(HttpStatusCode.OK, listToolsResponse.status)
            val listToolsText = listToolsResponse.bodyAsText()
            assertTrue(listToolsText.contains("open_target_session"))
            assertTrue(listToolsText.contains("inspect_method"))

            val openSessionResponse = it.post(baseUrl) {
                headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                accept(ContentType.Application.Json)
                headers.append("Mcp-Protocol-Version", "2025-11-25")
                setBody(
                    buildJsonObject {
                        put("jsonrpc", "2.0")
                        put("id", 3)
                        put("method", "tools/call")
                        put(
                            "params",
                            buildJsonObject {
                                put("name", "open_target_session")
                                put(
                                    "arguments",
                                    buildJsonObject {
                                        put("input", "sample.apk")
                                    },
                                )
                            },
                        )
                    }.toString(),
                )
            }
            assertEquals(HttpStatusCode.OK, openSessionResponse.status)
            val openSessionText = openSessionResponse.bodyAsText()
            assertTrue(openSessionText.contains("sessionId"))
            assertTrue(openSessionText.contains("workspaceId"))
            assertTrue(openSessionText.contains("sample.apk"))
        }
    }

    private fun allocateTestPort(): Int =
        ServerSocket(0).use { it.localPort }

}
