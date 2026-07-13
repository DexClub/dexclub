package io.github.dexclub.mcp

internal object McpSessionToolCatalog {
    val tools: List<McpToolMetadata> = listOf(
        McpToolMetadata(
            name = "open_target_session",
            description = "Initialize a target input and create a reusable DexClub target session. input must be an absolute path to an existing target file; relative paths are resolved against the MCP process working directory.",
            inputProperties = listOf(McpToolInputProperties.string("input")),
            required = setOf("input"),
        ),
        McpToolMetadata(
            name = "list_target_sessions",
            description = "List open target sessions in the current MCP process so the same server can switch between workspaces or targets.",
        ),
        McpToolMetadata(
            name = "get_target_session",
            description = "Read the current bound workspace and active target for a single target session.",
            inputProperties = listOf(McpToolInputProperties.string("session_id")),
            required = setOf("session_id"),
        ),
        McpToolMetadata(
            name = "close_target_session",
            description = "Close a target session and clear the method_handle and class_handle state owned by that session.",
            inputProperties = listOf(McpToolInputProperties.string("session_id")),
            required = setOf("session_id"),
        ),
        McpToolMetadata(
            name = "refresh_target_session",
            description = "Explicitly refresh the workspace snapshot bound to a target session and clear its existing method_handle and class_handle state.",
            inputProperties = listOf(McpToolInputProperties.string("session_id")),
            required = setOf("session_id"),
        ),
        McpToolMetadata(
            name = "diagnose_target_sessions",
            description = "Return a runtime summary of target sessions and handles in the current MCP process to make state visible and verify idle cleanup behavior.",
        ),
    )

    private val toolsByName = tools.associateBy(McpToolMetadata::name)

    fun require(name: String): McpToolMetadata =
        toolsByName[name] ?: error("unknown session tool: $name")
}
