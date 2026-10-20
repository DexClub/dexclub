package io.github.dexclub.mcp

import java.io.BufferedReader
import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class McpStdioSmokeTest {
    @Test
    fun stdioSupportsInitializeToolsListAndSystemToolCalls() {
        startServer().use { server ->
            server.send("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"dexclub-stdio-smoke","version":"1"}}}""")
            val initialized = server.response(1)["result"]!!.jsonObject
            assertEquals("dexclub-mcp", initialized["serverInfo"]!!.jsonObject["name"]!!.jsonPrimitive.content)
            server.send("""{"jsonrpc":"2.0","method":"notifications/initialized"}""")

            server.send("""{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}""")
            val tools = server.response(2)["result"]!!.jsonObject["tools"]!!.jsonArray
            val names = tools.map { it.jsonObject["name"]!!.jsonPrimitive.content }.toSet()
            assertTrue(names.containsAll(setOf("get_server_info", "open_target_session", "find_methods", "manifest")))

            server.send("""{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"get_server_info","arguments":{}}}""")
            val result = server.response(3)["result"]!!.jsonObject
            val text = result["content"]!!.jsonArray.single().jsonObject["text"]!!.jsonPrimitive.content
            val info = Json.parseToJsonElement(text).jsonObject
            assertEquals(McpBuildInfo.VERSION, info["version"]!!.jsonPrimitive.content)
            assertEquals(McpBuildInfo.MCP_CONTRACT_VERSION.toString(), info["mcp_contract_version"]!!.jsonPrimitive.content)

            val stderr = server.finish()
            assertTrue(stderr.contains("ready on stdio"))
            assertFalse(stderr.contains("listening on http"))
            assertFalse(stderr.contains("non-loopback host"))
        }
    }

    @Test
    fun stdioExitsCleanlyWhenInputClosesBeforeInitialize() {
        startServer().use { server ->
            server.finish()
        }
    }

    private fun startServer(): StdioProcess {
        val javaName = if (System.getProperty("os.name").startsWith("Windows")) "java.exe" else "java"
        val java = Paths.get(System.getProperty("java.home"), "bin", javaName).toString()
        val classpath = checkNotNull(System.getProperty("dexclub.mcp.test.runtime.classpath"))
        val runtimeRoot = Paths.get("build", "tmp", "stdio-smoke").toAbsolutePath()
        Files.createDirectories(runtimeRoot)
        val runtimeDir = Files.createTempDirectory(runtimeRoot, "runtime-")
        val builder = ProcessBuilder(
            java,
            "-Ddexclub.mcp.runtime.files.dir=$runtimeDir",
            "-cp", classpath,
            "io.github.dexclub.mcp.MainKt",
            "--transport=stdio",
        )
        builder.environment()["DEXCLUB_MCP_TRACE"] = "false"
        builder.environment()["DEXCLUB_MCP_STDERR"] = "true"
        builder.environment()["DEXCLUB_MCP_HOST"] = "0.0.0.0"
        return StdioProcess(builder.start())
    }

    private class StdioProcess(private val process: Process) : AutoCloseable {
        private val input: BufferedWriter = process.outputStream.bufferedWriter(Charsets.UTF_8)
        private val output: BufferedReader = process.inputStream.bufferedReader(Charsets.UTF_8)
        private val stderr = CompletableFuture.supplyAsync {
            process.errorStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }

        fun send(message: String) {
            input.write(message)
            input.newLine()
            input.flush()
        }

        fun response(id: Int): JsonObject {
            val line = CompletableFuture.supplyAsync { output.readLine() }.get(20, TimeUnit.SECONDS)
            checkNotNull(line) { "MCP process exited before response $id" }
            val response = Json.parseToJsonElement(line).jsonObject
            assertEquals(id.toString(), response["id"]!!.jsonPrimitive.content)
            assertFalse(response.containsKey("error"), line)
            return response
        }

        fun finish(): String {
            input.close()
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "MCP process did not exit after stdin EOF")
            val diagnostics = stderr.get(5, TimeUnit.SECONDS)
            assertEquals(0, process.exitValue(), diagnostics)
            assertEquals("", output.readText(), "Unexpected stdout after the last MCP response")
            return diagnostics
        }

        override fun close() {
            if (process.isAlive) {
                process.destroyForcibly()
                process.waitFor(5, TimeUnit.SECONDS)
            }
            input.close()
            output.close()
        }
    }
}
