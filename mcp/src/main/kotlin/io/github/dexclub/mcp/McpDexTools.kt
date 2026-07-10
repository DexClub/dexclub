package io.github.dexclub.mcp

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.MethodHit
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal fun McpApp.registerDexTools(server: Server) {
    server.addLoggedTool(
        name = "inspect_method",
        description = "基于已打开的 target session 检查方法的一层事实视图。优先传 method_handle；include 仅支持 using-fields、callers、invokes、strings、annotations；brief=true 时只返回计数摘要。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("session_id", stringSchema())
                put("workdir", stringSchema())
                put("method_handle", stringSchema())
                put("descriptor", stringSchema())
                put("include", stringArraySchema())
                put("brief", booleanSchema())
            },
        ),
    ) { request ->
        val context = when (val resolution = executionContextOrFailureResult(request)) {
            is ExecutionContextResolution.Ready -> resolution.context
            is ExecutionContextResolution.Failed -> return@addLoggedTool resolution.result
        }
        val session = context.session
        val methodRef = try {
            resolveMethodReference(request, session)
        } catch (cause: IllegalArgumentException) {
            return@addLoggedTool errorResult(cause.message.orEmpty())
        }
        val descriptor = methodRef?.descriptor.orEmpty()
        if (descriptor.isEmpty()) {
            return@addLoggedTool errorResult("method_handle or descriptor is required")
        }

        val includes = try {
            parseMethodDetailSections(
                (request.arguments?.get("include") as? JsonArray)
                    ?.jsonArray
                    ?.map { it.jsonPrimitive.content },
            )
        } catch (cause: IllegalArgumentException) {
            return@addLoggedTool errorResult(cause.message.orEmpty())
        }
        val brief = request.booleanArgument("brief") ?: false

        val detail = inspectMethod(
            workspace = context.workspace,
            descriptor = methodRef!!.descriptor,
            source = request.toSourceLocator(methodRef),
            includes = includes,
        )
        CallToolResult(
            content = listOf(
                TextContent(
                    json.encodeToString(
                        InspectMethodResult.serializer(),
                        context.toInspectMethodResult(detail, brief = brief),
                    ),
                ),
            ),
        )
    }

    server.addLoggedTool(
        name = "export_method_java",
        description = "导出单方法的 Java 语义视图。优先传 method_handle；通常应先用 find/inspect 缩小候选，再导出少量方法文本。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("session_id", stringSchema())
                put("workdir", stringSchema())
                put("method_handle", stringSchema())
                put("descriptor", stringSchema())
                put("source_path", stringSchema())
                put("source_entry", stringSchema())
            },
        ),
    ) { request ->
        exportMethodTextTool(
            request = request,
            view = "java",
            exporter = ::exportMethodJavaText,
        )
    }

    server.addLoggedTool(
        name = "export_method_smali",
        description = "导出单方法的 smali 原始证据视图。优先传 method_handle；通常应先用 find/inspect 缩小候选，再导出少量方法文本。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("session_id", stringSchema())
                put("workdir", stringSchema())
                put("method_handle", stringSchema())
                put("descriptor", stringSchema())
                put("source_path", stringSchema())
                put("source_entry", stringSchema())
                put("mode", smaliModeSchema)
            },
        ),
    ) { request ->
        exportMethodTextTool(
            request = request,
            view = "smali",
            exporter = ::exportMethodSmaliText,
        )
    }

    server.addLoggedTool(
        name = "export_class_java",
        description = "导出整类的 Java 语义视图。优先传 class_handle；通常应先确认类候选，再导出整类文本。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("session_id", stringSchema())
                put("workdir", stringSchema())
                put("class_handle", stringSchema())
                put("descriptor", stringSchema())
                put("source_path", stringSchema())
                put("source_entry", stringSchema())
            },
        ),
    ) { request ->
        exportClassTextTool(
            request = request,
            view = "java",
            exporter = ::exportClassJavaText,
        )
    }

    server.addLoggedTool(
        name = "export_class_smali",
        description = "导出整类的 smali 原始证据视图。优先传 class_handle；通常应先确认类候选，再导出整类文本。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("session_id", stringSchema())
                put("workdir", stringSchema())
                put("class_handle", stringSchema())
                put("descriptor", stringSchema())
                put("source_path", stringSchema())
                put("source_entry", stringSchema())
            },
        ),
    ) { request ->
        exportClassTextTool(
            request = request,
            view = "smali",
            exporter = ::exportClassSmaliText,
        )
    }

    server.addLoggedTool(
        name = "find_methods",
        description = "按类名、方法名或 descriptor 片段定位方法候选。建议配合 brief=true 和 fields 收窄返回，再继续 inspect 或 export。仅在 session_id 存在时支持请求 methodHandle。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("session_id", stringSchema())
                put("workdir", stringSchema())
                put("class_name_contains", stringSchema())
                put("method_name_contains", stringSchema())
                put("descriptor_contains", stringSchema())
                put("offset", integerSchema())
                put("limit", integerSchema())
                put("fields", methodFieldsSchema)
                put("brief", booleanSchema())
            },
        ),
    ) { request ->
        val context = when (val resolution = executionContextOrFailureResult(request)) {
            is ExecutionContextResolution.Ready -> resolution.context
            is ExecutionContextResolution.Failed -> return@addLoggedTool resolution.result
        }
        val classNameContains = request.optionalStringArgument("class_name_contains")
        val methodNameContains = request.optionalStringArgument("method_name_contains")
        val descriptorContains = request.optionalStringArgument("descriptor_contains")
        val offset = request.intArgument("offset")
        val limit = request.intArgument("limit")
        val brief = request.booleanArgument("brief") ?: false
        val fields = try {
            parseRequestedFields(
                request.stringArrayArgument("fields"),
                supported = if (context.session != null) methodFieldNamesWithHandle else methodFieldNames,
                sessionRequiredFields = setOf("methodHandle"),
                hasSession = context.session != null,
            )
        } catch (cause: IllegalArgumentException) {
            return@addLoggedTool errorResult(cause.message.orEmpty())
        }
        val hits = try {
            findMethods(
                workspace = context.workspace,
                classNameContains = classNameContains,
                methodNameContains = methodNameContains,
                descriptorContains = descriptorContains,
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
                        FindMethodsResult.serializer(),
                        context.toFindMethodsResult(
                            hits,
                            handleProvider = context.session?.let { activeSession ->
                                { hit: MethodHit ->
                                    sessionStore.putMethodHandle(activeSession.sessionId, hit.descriptor, hit.sourcePath, hit.sourceEntry)
                                }
                            },
                            fields = fields,
                            brief = brief,
                        ),
                    ),
                ),
            ),
        )
    }

    server.addLoggedTool(
        name = "find_classes_using_strings",
        description = "使用字符串锚点定位类候选。建议优先用 brief=true 和 fields 收窄候选，再继续 export_class_* 或 find_methods。仅在 session_id 存在时支持请求 classHandle。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("session_id", stringSchema())
                put("workdir", stringSchema())
                put("contains_any_strings", stringArraySchema())
                put("contains_all_strings", stringArraySchema())
                put("offset", integerSchema())
                put("limit", integerSchema())
                put("fields", classFieldsSchema)
                put("brief", booleanSchema())
            },
        ),
    ) { request ->
        findClassesUsingStringsTool(request)
    }

    server.addLoggedTool(
        name = "find_methods_using_strings",
        description = "使用字符串锚点定位方法候选。建议优先用 brief=true 和 fields 收窄候选，再继续 inspect_method 或 export_method_*。仅在 session_id 存在时支持请求 methodHandle。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("session_id", stringSchema())
                put("workdir", stringSchema())
                put("contains_any_strings", stringArraySchema())
                put("contains_all_strings", stringArraySchema())
                put("offset", integerSchema())
                put("limit", integerSchema())
                put("fields", methodFieldsSchema)
                put("brief", booleanSchema())
            },
        ),
    ) { request ->
        findMethodsUsingStringsTool(request)
    }
}
