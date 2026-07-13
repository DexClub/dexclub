package io.github.dexclub.mcp

internal object McpDexToolCatalog {
    val tools: List<McpToolMetadata> = listOf(
        McpToolMetadata(
            name = "inspect_method",
            description = "Inspect a method within an open target session. Prefer method_handle; include only supports using-fields, callers, invokes, strings, and annotations; brief=true returns counts only.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.string("method_handle"),
                McpToolInputProperties.string("descriptor"),
                McpToolInputProperties.stringArray("include"),
                McpToolInputProperties.boolean("brief"),
            ),
        ),
        McpToolMetadata(
            name = "export_method_java",
            description = "Export the Java semantic view for a single method. Prefer method_handle and narrow candidates with find or inspect before exporting small snippets.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.string("method_handle"),
                McpToolInputProperties.string("descriptor"),
                McpToolInputProperties.string("source_path"),
                McpToolInputProperties.string("source_entry"),
            ),
        ),
        McpToolMetadata(
            name = "export_method_smali",
            description = "Export the raw smali evidence view for a single method. Prefer method_handle and narrow candidates with find or inspect before exporting small snippets.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.string("method_handle"),
                McpToolInputProperties.string("descriptor"),
                McpToolInputProperties.string("source_path"),
                McpToolInputProperties.string("source_entry"),
                McpToolInputProperties.enumString("mode", setOf("snippet", "class")),
            ),
        ),
        McpToolMetadata(
            name = "export_class_java",
            description = "Export the Java semantic view for a full class. Prefer class_handle and confirm the class candidate before exporting full-class text.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.string("class_handle"),
                McpToolInputProperties.string("descriptor"),
                McpToolInputProperties.string("source_path"),
                McpToolInputProperties.string("source_entry"),
            ),
        ),
        McpToolMetadata(
            name = "export_class_smali",
            description = "Export the raw smali evidence view for a full class. Prefer class_handle and confirm the class candidate before exporting full-class text.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.string("class_handle"),
                McpToolInputProperties.string("descriptor"),
                McpToolInputProperties.string("source_path"),
                McpToolInputProperties.string("source_entry"),
            ),
        ),
        McpToolMetadata(
            name = "find_methods",
            description = "Find method candidates by class name or method name, with optional descriptor_contains filtering applied afterward. Prefer brief=true and fields to narrow results before inspect or export. methodHandle is only available with session_id.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.string("class_name_contains"),
                McpToolInputProperties.string("method_name_contains"),
                McpToolInputProperties.string("descriptor_contains"),
                McpToolInputProperties.integer("offset"),
                McpToolInputProperties.integer("limit"),
                McpToolInputProperties.enumStringArray("fields", methodFieldNamesWithHandle),
                McpToolInputProperties.boolean("brief"),
            ),
        ),
        McpToolMetadata(
            name = "find_classes_using_strings",
            description = "Find class candidates using string anchors. Prefer brief=true and fields to narrow results before export_class_* or find_methods. classHandle is only available with session_id.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.stringArray("contains_any_strings"),
                McpToolInputProperties.stringArray("contains_all_strings"),
                McpToolInputProperties.integer("offset"),
                McpToolInputProperties.integer("limit"),
                McpToolInputProperties.enumStringArray("fields", classFieldNamesWithHandle),
                McpToolInputProperties.boolean("brief"),
            ),
        ),
        McpToolMetadata(
            name = "find_methods_using_strings",
            description = "Find method candidates using string anchors. Prefer brief=true and fields to narrow results before inspect_method or export_method_*. methodHandle is only available with session_id.",
            inputProperties = contextualInputProperties(
                McpToolInputProperties.stringArray("contains_any_strings"),
                McpToolInputProperties.stringArray("contains_all_strings"),
                McpToolInputProperties.integer("offset"),
                McpToolInputProperties.integer("limit"),
                McpToolInputProperties.enumStringArray("fields", methodFieldNamesWithHandle),
                McpToolInputProperties.boolean("brief"),
            ),
        ),
    )

    private val toolsByName = tools.associateBy(McpToolMetadata::name)

    fun require(name: String): McpToolMetadata =
        toolsByName[name] ?: error("unknown dex tool: $name")
}
