package io.github.dexclub.mcp

object McpBuildInfo {
    val version: String by lazy {
        McpBuildInfo::class.java.classLoader
            .getResourceAsStream("dexclub-mcp-version.txt")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText().trim() }
            ?.ifBlank { "dev" }
            ?: "dev"
    }
}
