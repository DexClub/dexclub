package io.github.dexclub.mcp

import java.nio.file.Path
import java.nio.file.Paths

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

internal fun loadHttpServerConfig(): HttpServerConfig {
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
