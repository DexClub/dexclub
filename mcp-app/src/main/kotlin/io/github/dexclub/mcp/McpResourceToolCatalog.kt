package io.github.dexclub.mcp

internal object McpResourceToolCatalog {
    val tools: List<McpToolMetadata> = listOf(
        McpToolMetadata(
            name = "manifest",
            description = "Return the structured manifest view for the current target. Prefer structured fields first; only set include_text=true when raw evidence is actually needed.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.enumStringArray("include", manifestInspectionSectionNames),
                McpToolInputProperties.boolean("include_text"),
            ),
        ),
        McpToolMetadata(
            name = "list_res",
            description = "List visible resource entries for the current target. Prefer brief=true and fields to narrow results first.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.string("type"),
                McpToolInputProperties.integer("offset"),
                McpToolInputProperties.integer("limit"),
                McpToolInputProperties.enumStringArray("fields", resourceEntryFieldNames),
                McpToolInputProperties.boolean("brief"),
            ),
        ),
        McpToolMetadata(
            name = "find_resource_values",
            description = "Search resource candidates by resolved value. Only string, integer, bool, and color are supported. Prefer brief=true and fields to narrow results before confirming with get_resource_value.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.string("type"),
                McpToolInputProperties.string("value"),
                McpToolInputProperties.boolean("contains"),
                McpToolInputProperties.boolean("ignore_case"),
                McpToolInputProperties.integer("offset"),
                McpToolInputProperties.integer("limit"),
                McpToolInputProperties.enumStringArray("fields", resourceValueFieldNames),
                McpToolInputProperties.boolean("brief"),
            ),
            required = setOf("type", "value"),
        ),
        McpToolMetadata(
            name = "get_resource_value",
            description = "Resolve a resource ID or type/name pair to a structured resource value.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.string("resource_id"),
                McpToolInputProperties.string("type"),
                McpToolInputProperties.string("name"),
            ),
        ),
        McpToolMetadata(
            name = "decode_xml",
            description = "Decode binary or text XML from the current target. Commonly used for APK res/layout, res/xml, and other packaged XML resources.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.string("path"),
            ),
            required = setOf("path"),
        ),
    )

    private val toolsByName = tools.associateBy(McpToolMetadata::name)

    fun require(name: String): McpToolMetadata =
        toolsByName[name] ?: error("unknown resource tool: $name")
}
