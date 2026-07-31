package io.github.dexclub.mcp

import io.github.dexclub.core.api.dex.FindClassQuery
import io.github.dexclub.core.api.dex.FindFieldQuery
import io.github.dexclub.core.api.dex.FindMethodQuery

internal object McpDexToolCatalog {
    private val findClassSchema = jsonSchemaFor(FindClassQuery.serializer().descriptor)
    private val findMethodSchema = jsonSchemaFor(FindMethodQuery.serializer().descriptor)
    private val findFieldSchema = jsonSchemaFor(FindFieldQuery.serializer().descriptor)

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
        findTool(
            name = "find_classes",
            description = "Find class candidates with the complete public FindClassQuery and Matcher JSON structure. classHandle is only available with session_id.",
            schema = findClassSchema,
            fields = classFieldNamesWithHandle,
        ),
        findTool(
            name = "find_methods",
            description = "Find method candidates with the complete public FindMethodQuery and Matcher JSON structure. methodHandle is only available with session_id.",
            schema = findMethodSchema,
            fields = methodFieldNamesWithHandle,
        ),
        findTool(
            name = "find_fields",
            description = "Find field candidates with the complete public FindFieldQuery and Matcher JSON structure.",
            schema = findFieldSchema,
            fields = fieldFieldNames,
        ),
    )

    private val toolsByName = tools.associateBy(McpToolMetadata::name)

    fun require(name: String): McpToolMetadata =
        toolsByName[name] ?: error("unknown dex tool: $name")

    private fun findTool(
        name: String,
        description: String,
        schema: JsonSchemaBundle,
        fields: Set<String>,
    ) = McpToolMetadata(
        name = name,
        description = description,
        inputProperties = contextualInputProperties(
            McpToolInputProperties.jsonObject("query", schema.root),
            McpToolInputProperties.integer("offset", minimum = 0),
            McpToolInputProperties.integer("limit", minimum = 1, maximum = MCP_FIND_MAX_LIMIT),
            McpToolInputProperties.enumStringArray("fields", fields),
            McpToolInputProperties.boolean("brief"),
        ),
        required = setOf("query"),
        defs = schema.defs,
    )
}

internal const val MCP_FIND_DEFAULT_LIMIT = 50
internal const val MCP_FIND_MAX_LIMIT = 200
