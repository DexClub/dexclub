package io.github.dexclub.mcp

internal object McpResourceToolCatalog {
    val tools: List<McpToolMetadata> = listOf(
        McpToolMetadata(
            name = "manifest",
            description = "Return the structured manifest view for the current target. Prefer structured fields first; only set include_text=true when raw evidence is actually needed.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.enumStringArray("include", manifestInspectionSectionNames),
                McpToolInputProperties.boolean("include_text"),
                McpToolInputProperties.string("component_name"),
                McpToolInputProperties.enumString(
                    "component_type",
                    setOf("activity", "activity-alias", "service", "receiver", "provider"),
                ),
            ),
        ),
        McpToolMetadata(
            name = "list_res",
            description = "List visible resource entries for the current target. Prefer brief=true and fields to narrow results first.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.string("resource_id"),
                McpToolInputProperties.string("package_name"),
                McpToolInputProperties.string("resource_type"),
                McpToolInputProperties.string("name"),
                McpToolInputProperties.string("file_path"),
                McpToolInputProperties.enumString("resolution", setOf("table-backed", "table-value", "unresolved", "table-hole")),
                McpToolInputProperties.integer("offset"),
                McpToolInputProperties.integer("limit"),
                McpToolInputProperties.enumStringArray("fields", resourceEntryFieldNames),
                McpToolInputProperties.boolean("brief"),
            ),
        ),
        McpToolMetadata(
            name = "find_resource_values",
            description = "Search every resource variant and bag item by decoded value, raw data, reference, or bag key. Confirm candidates with get_resource_value.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.string("resource_type"),
                McpToolInputProperties.string("value"),
                McpToolInputProperties.string("package_name"),
                McpToolInputProperties.string("qualifier"),
                McpToolInputProperties.string("value_kind"),
                McpToolInputProperties.enumString("match_target", setOf("decoded_value", "raw_data", "reference", "bag_key", "any")),
                McpToolInputProperties.boolean("contains"),
                McpToolInputProperties.boolean("ignore_case"),
                McpToolInputProperties.integer("offset"),
                McpToolInputProperties.integer("limit"),
                McpToolInputProperties.enumStringArray("fields", resourceValueFieldNames),
                McpToolInputProperties.boolean("brief"),
            ),
            required = setOf("resource_type", "value"),
        ),
        McpToolMetadata(
            name = "get_resource_value",
            description = "Resolve a resource ID or resource_type/name pair. Select one qualifier or set include_all_variants=true for the complete configuration set.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.string("resource_id"),
                McpToolInputProperties.string("package_name"),
                McpToolInputProperties.string("resource_type"),
                McpToolInputProperties.string("name"),
                McpToolInputProperties.string("qualifier"),
                McpToolInputProperties.boolean("include_all_variants"),
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
