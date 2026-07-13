package io.github.dexclub.mcp

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class McpSkillContractTest {
    @Test
    fun dexclubAnalysisSkillKeepsUsefulMcpSurfaceAlignedWithCatalog() {
        val skillPath = resolveRepoRoot()
            .resolve("skills/dexclub-analysis/SKILL.md")
        val lines = Files.readAllLines(skillPath)
        val toolNames = extractUsefulMcpSurface(lines)

        assertEquals(
            McpToolCatalogs.tools.map(McpToolMetadata::name).sorted(),
            toolNames.sorted(),
        )
    }

    private fun resolveRepoRoot(): Path {
        val configured = System.getProperty("dexclub.repo.root")
            ?: error("missing dexclub.repo.root test system property")
        return Path.of(configured).toAbsolutePath().normalize()
    }

    private fun extractUsefulMcpSurface(lines: List<String>): List<String> {
        val headerIndex = lines.indexOfFirst { it.trim() == "## Useful MCP Surface" }
        require(headerIndex >= 0) { "missing '## Useful MCP Surface' section in SKILL.md" }

        val tools = mutableListOf<String>()
        for (index in (headerIndex + 1) until lines.size) {
            val line = lines[index].trim()
            if (line.startsWith("## ")) break
            if (!line.startsWith("- ")) continue
            tools += line.removePrefix("- ").trim().removeSurrounding("`")
        }
        require(tools.isNotEmpty()) { "Useful MCP Surface section is empty in SKILL.md" }
        return tools
    }
}
