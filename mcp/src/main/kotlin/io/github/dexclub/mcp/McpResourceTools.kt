package io.github.dexclub.mcp

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal fun McpApp.registerResourceTools(server: Server) {
    server.addLoggedTool(
        name = "manifest",
        description = "返回当前 target 的结构化 manifest 视图。默认先只取结构化字段；仅在确实需要原始证据时才传 include_text=true。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("session_id", stringSchema())
                put("workdir", stringSchema())
                put(
                    "include",
                    manifestIncludeSchema,
                )
                put("include_text", booleanSchema())
            },
        ),
    ) { request ->
        val context = when (val resolution = executionContextOrFailureResult(request)) {
            is ExecutionContextResolution.Ready -> resolution.context
            is ExecutionContextResolution.Failed -> return@addLoggedTool resolution.result
        }
        val includes = try {
            parseManifestInspectionSections(
                (request.arguments?.get("include") as? JsonArray)
                    ?.jsonArray
                    ?.map { it.jsonPrimitive.content },
            )
        } catch (cause: IllegalArgumentException) {
            return@addLoggedTool errorResult(cause.message.orEmpty())
        }
        val includeText = request.arguments?.get("include_text")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        val manifest = inspectManifest(context.workspace, includes, includeText)
        CallToolResult(
            content = listOf(
                TextContent(
                    json.encodeToString(
                        ManifestDecodeResult.serializer(),
                        context.toManifestDecodeResult(manifest),
                    ),
                ),
            ),
        )
    }

    server.addLoggedTool(
        name = "list_res",
        description = "列出当前 target 可见的资源条目索引。建议优先用 brief=true 和 fields 收窄结果。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("session_id", stringSchema())
                put("workdir", stringSchema())
                put("type", stringSchema())
                put("offset", integerSchema())
                put("limit", integerSchema())
                put(
                    "fields",
                    resourceEntryFieldsSchema,
                )
                put("brief", booleanSchema())
            },
        ),
    ) { request ->
        val context = when (val resolution = executionContextOrFailureResult(request)) {
            is ExecutionContextResolution.Ready -> resolution.context
            is ExecutionContextResolution.Failed -> return@addLoggedTool resolution.result
        }
        val type = request.optionalStringArgument("type")
        val offset = request.intArgument("offset")
        val limit = request.intArgument("limit")
        val brief = request.booleanArgument("brief") ?: false
        val fields = try {
            parseRequestedFields(
                request.stringArrayArgument("fields"),
                supported = resourceEntryFieldNames,
            )
        } catch (cause: IllegalArgumentException) {
            return@addLoggedTool errorResult(cause.message.orEmpty())
        }
        val entries = try {
            listResources(
                workspace = context.workspace,
                type = type,
                offset = offset,
                limit = limit,
            )
        } catch (cause: IllegalArgumentException) {
            return@addLoggedTool errorResult(cause.message.orEmpty())
        }
        CallToolResult(
            content = listOf(
                TextContent(
                    json.encodeToString(
                        ListResourcesResult.serializer(),
                        context.toListResourcesResult(entries, fields = fields, brief = brief),
                    ),
                ),
            ),
        )
    }

    server.addLoggedTool(
        name = "find_resource_values",
        description = "按资源值搜索资源候选，仅支持 string/integer/bool/color。建议优先用 brief=true 和 fields 收窄结果，再用 get_resource_value 精确确认。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("session_id", stringSchema())
                put("workdir", stringSchema())
                put("type", stringSchema())
                put("value", stringSchema())
                put("contains", booleanSchema())
                put("ignore_case", booleanSchema())
                put("offset", integerSchema())
                put("limit", integerSchema())
                put(
                    "fields",
                    resourceValueFieldsSchema,
                )
                put("brief", booleanSchema())
            },
            required = listOf("type", "value"),
        ),
    ) { request ->
        val context = when (val resolution = executionContextOrFailureResult(request)) {
            is ExecutionContextResolution.Ready -> resolution.context
            is ExecutionContextResolution.Failed -> return@addLoggedTool resolution.result
        }
        val type = request.requiredStringArgument("type")
        val value = request.requiredStringArgument("value")
        if (type.isEmpty() || value.isEmpty()) {
            return@addLoggedTool errorResult("type and value are required")
        }
        val contains = request.booleanArgument("contains") ?: false
        val ignoreCase = request.booleanArgument("ignore_case") ?: false
        val offset = request.intArgument("offset")
        val limit = request.intArgument("limit")
        val brief = request.booleanArgument("brief") ?: false
        val fields = try {
            parseRequestedFields(
                request.stringArrayArgument("fields"),
                supported = resourceValueFieldNames,
            )
        } catch (cause: IllegalArgumentException) {
            return@addLoggedTool errorResult(cause.message.orEmpty())
        }
        val hits = try {
            findResourceValues(
                workspace = context.workspace,
                type = type,
                value = value,
                contains = contains,
                ignoreCase = ignoreCase,
                offset = offset,
                limit = limit,
            )
        } catch (cause: IllegalArgumentException) {
            return@addLoggedTool errorResult(cause.message.orEmpty())
        }
        CallToolResult(
            content = listOf(
                TextContent(
                    json.encodeToString(
                        FindResourcesResult.serializer(),
                        context.toFindResourcesResult(hits, fields = fields, brief = brief),
                    ),
                ),
            ),
        )
    }

    server.addLoggedTool(
        name = "get_resource_value",
        description = "将资源 id 或 type/name 解析为结构化资源值。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("session_id", stringSchema())
                put("workdir", stringSchema())
                put("resource_id", stringSchema())
                put("type", stringSchema())
                put("name", stringSchema())
            },
        ),
    ) { request ->
        val context = when (val resolution = executionContextOrFailureResult(request)) {
            is ExecutionContextResolution.Ready -> resolution.context
            is ExecutionContextResolution.Failed -> return@addLoggedTool resolution.result
        }
        val resourceId = request.optionalStringArgument("resource_id")
        val type = request.optionalStringArgument("type")
        val name = request.optionalStringArgument("name")
        if (resourceId == null && (type == null || name == null)) {
            return@addLoggedTool errorResult("resource_id or type+name is required")
        }
        val resource = getResourceValue(
            workspace = context.workspace,
            resourceId = resourceId,
            type = type,
            name = name,
        )
        CallToolResult(
            content = listOf(
                TextContent(
                    json.encodeToString(
                        ResolveResourceResult.serializer(),
                        context.toResolveResourceResult(resource),
                    ),
                ),
            ),
        )
    }

    server.addLoggedTool(
        name = "decode_xml",
        description = "解码当前 target 中的二进制或文本 XML。常用于读取 APK 内的 res/layout、res/xml 等安装包布局与资源 XML。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("session_id", stringSchema())
                put("workdir", stringSchema())
                put("path", stringSchema())
            },
            required = listOf("path"),
        ),
    ) { request ->
        val context = when (val resolution = executionContextOrFailureResult(request)) {
            is ExecutionContextResolution.Ready -> resolution.context
            is ExecutionContextResolution.Failed -> return@addLoggedTool resolution.result
        }
        val path = request.requiredStringArgument("path")
        if (path.isEmpty()) {
            return@addLoggedTool errorResult("path is required")
        }
        val xml = decodeXml(
            workspace = context.workspace,
            path = path,
        )
        CallToolResult(
            content = listOf(
                TextContent(
                    json.encodeToString(
                        DecodeXmlResult.serializer(),
                        context.toDecodeXmlResult(xml),
                    ),
                ),
            ),
        )
    }
}
