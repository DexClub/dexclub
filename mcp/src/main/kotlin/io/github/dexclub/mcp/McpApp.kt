package io.github.dexclub.mcp

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.ExportClassJavaRequest
import io.github.dexclub.core.api.dex.ExportClassSmaliRequest
import io.github.dexclub.core.api.dex.ExportMethodJavaRequest
import io.github.dexclub.core.api.dex.ExportMethodSmaliRequest
import io.github.dexclub.core.api.dex.InspectMethodRequest
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.resource.DecodeXmlRequest
import io.github.dexclub.core.api.resource.InspectManifestRequest
import io.github.dexclub.core.api.resource.ResolveResourceRequest
import io.github.dexclub.core.api.shared.MethodSmaliMode
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.shared.Services
import io.github.dexclub.core.api.shared.createDefaultServices
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceRef
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText

internal class McpApp(
    private val services: Services = createDefaultServices(),
    private val sessionStore: McpSessionStore = McpSessionStore(),
) {
    private val sessionIdOnlySchema = ToolSchema(
        properties = buildJsonObject {
            put("session_id", buildJsonObject { put("type", "string") })
        },
        required = listOf("session_id"),
    )

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        explicitNulls = false
    }

    fun createServer(): Server {
        val server = Server(
            serverInfo = Implementation(
                name = "dexclub-mcp",
                version = "dev",
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = false),
                ),
            ),
        )

        server.addLoggedTool(
            name = "open_target_session",
            description = "初始化目标输入并创建可复用的 DexClub target session。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("input", buildJsonObject { put("type", "string") })
                },
                required = listOf("input"),
            ),
        ) { request ->
            val input = request.arguments?.get("input")?.jsonPrimitive?.content?.trim().orEmpty()
            if (input.isEmpty()) {
                return@addLoggedTool CallToolResult(
                    content = listOf(TextContent("""{"error":"input is required"}""")),
                    isError = true,
                )
            }

            val targetSession = openTargetSession(input)
            CallToolResult(
                content = listOf(
                    TextContent(json.encodeToString(OpenTargetSessionResult.serializer(), targetSession.toResult())),
                ),
            )
        }

        server.addLoggedTool(
            name = "list_target_sessions",
            description = "列出当前 MCP 进程中已打开的 target session，用于在同一 server 内切换不同工作区或目标。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {},
            ),
        ) {
            val sessions = listTargetSessions()
            CallToolResult(
                content = listOf(
                    TextContent(
                        json.encodeToString(
                            ListTargetSessionsResult.serializer(),
                            ListTargetSessionsResult(
                                total = sessions.size,
                                items = sessions.map { it.toView() },
                            ),
                        ),
                    ),
                ),
            )
        }

        server.addLoggedTool(
            name = "get_target_session",
            description = "读取单个 target session 的当前绑定工作区与 active target 信息。",
            inputSchema = sessionIdOnlySchema,
        ) { request ->
            val sessionId = request.requiredStringArgument("session_id")
            if (sessionId.isEmpty()) {
                return@addLoggedTool errorResult("session_id is required")
            }
            val session = getTargetSession(sessionId)
                ?: return@addLoggedTool errorResult("session_id not found")
            CallToolResult(
                content = listOf(
                    TextContent(
                        json.encodeToString(
                            TargetSessionView.serializer(),
                            session.toView(),
                        ),
                    ),
                ),
            )
        }

        server.addLoggedTool(
            name = "close_target_session",
            description = "关闭一个 target session，并清理该 session 下的 method_handle / class_handle。",
            inputSchema = sessionIdOnlySchema,
        ) { request ->
            val sessionId = request.requiredStringArgument("session_id")
            if (sessionId.isEmpty()) {
                return@addLoggedTool errorResult("session_id is required")
            }
            val session = closeTargetSession(sessionId)
            CallToolResult(
                content = listOf(
                    TextContent(
                        json.encodeToString(
                            CloseTargetSessionResult.serializer(),
                            CloseTargetSessionResult(
                                closed = session != null,
                                session = session?.toView(),
                            ),
                        ),
                    ),
                ),
            )
        }

        server.addLoggedTool(
            name = "diagnose_target_sessions",
            description = "返回当前 MCP 进程内 target session 与 handle 的运行态摘要，用于降低黑盒感并辅助判断空闲自动回收是否生效。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {},
            ),
        ) {
            val snapshot = diagnoseTargetSessions()
            CallToolResult(
                content = listOf(
                    TextContent(
                        json.encodeToString(
                            DiagnoseTargetSessionsResult.serializer(),
                            snapshot.toView(),
                        ),
                    ),
                ),
            )
        }

        server.addLoggedTool(
            name = "inspect_method",
            description = "基于已打开的 target session 检查方法的一层事实视图。优先传 method_handle；include 仅支持 using-fields、callers、invokes、strings、annotations；brief=true 时只返回计数摘要。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("workdir", buildJsonObject { put("type", "string") })
                    put("method_handle", buildJsonObject { put("type", "string") })
                    put("descriptor", buildJsonObject { put("type", "string") })
                    put(
                        "include",
                        buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        },
                    )
                    put("brief", buildJsonObject { put("type", "boolean") })
                },
            ),
        ) { request ->
            val context = resolveExecutionContext(request) ?: return@addLoggedTool missingSessionOrWorkdirResult()
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
                return@addLoggedTool CallToolResult(
                    content = listOf(TextContent("""{"error":"${cause.message}"}""")),
                    isError = true,
                )
            }
            val brief = request.booleanArgument("brief") ?: false

            val detail = inspectMethod(
                workspace = context.workspace,
                descriptor = methodRef!!.descriptor,
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
            name = "manifest",
            description = "返回当前 target 的结构化 manifest 视图。默认先只取结构化字段；仅在确实需要原始证据时才传 include_text=true。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("workdir", buildJsonObject { put("type", "string") })
                    put(
                        "include",
                        buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        },
                    )
                    put("include_text", buildJsonObject { put("type", "boolean") })
                },
            ),
        ) { request ->
            val context = resolveExecutionContext(request) ?: return@addLoggedTool missingSessionOrWorkdirResult()
            val includes = try {
                parseManifestInspectionSections(
                    (request.arguments?.get("include") as? JsonArray)
                        ?.jsonArray
                        ?.map { it.jsonPrimitive.content },
                )
            } catch (cause: IllegalArgumentException) {
                return@addLoggedTool CallToolResult(
                    content = listOf(TextContent("""{"error":"${cause.message}"}""")),
                    isError = true,
                )
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
            name = "export_method_java",
            description = "导出单方法的 Java 语义视图。优先传 method_handle；通常应先用 find/inspect 缩小候选，再导出少量方法文本。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("workdir", buildJsonObject { put("type", "string") })
                    put("method_handle", buildJsonObject { put("type", "string") })
                    put("descriptor", buildJsonObject { put("type", "string") })
                    put("source_path", buildJsonObject { put("type", "string") })
                    put("source_entry", buildJsonObject { put("type", "string") })
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
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("workdir", buildJsonObject { put("type", "string") })
                    put("method_handle", buildJsonObject { put("type", "string") })
                    put("descriptor", buildJsonObject { put("type", "string") })
                    put("source_path", buildJsonObject { put("type", "string") })
                    put("source_entry", buildJsonObject { put("type", "string") })
                    put("mode", buildJsonObject { put("type", "string") })
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
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("workdir", buildJsonObject { put("type", "string") })
                    put("class_handle", buildJsonObject { put("type", "string") })
                    put("descriptor", buildJsonObject { put("type", "string") })
                    put("source_path", buildJsonObject { put("type", "string") })
                    put("source_entry", buildJsonObject { put("type", "string") })
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
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("workdir", buildJsonObject { put("type", "string") })
                    put("class_handle", buildJsonObject { put("type", "string") })
                    put("descriptor", buildJsonObject { put("type", "string") })
                    put("source_path", buildJsonObject { put("type", "string") })
                    put("source_entry", buildJsonObject { put("type", "string") })
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
            description = "按类名、方法名或 descriptor 片段定位方法候选。建议配合 brief=true 和 fields 收窄返回，再继续 inspect 或 export。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("workdir", buildJsonObject { put("type", "string") })
                    put("class_name_contains", buildJsonObject { put("type", "string") })
                    put("method_name_contains", buildJsonObject { put("type", "string") })
                    put("descriptor_contains", buildJsonObject { put("type", "string") })
                    put("offset", buildJsonObject { put("type", "integer") })
                    put("limit", buildJsonObject { put("type", "integer") })
                    put(
                        "fields",
                        buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        },
                    )
                    put("brief", buildJsonObject { put("type", "boolean") })
                },
            ),
        ) { request ->
            val context = resolveExecutionContext(request) ?: return@addLoggedTool missingSessionOrWorkdirResult()
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
            description = "使用字符串锚点定位类候选。建议优先用 brief=true 和 fields 收窄候选，再继续 export_class_* 或 find_methods。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("workdir", buildJsonObject { put("type", "string") })
                    put(
                        "contains_any_strings",
                        buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        },
                    )
                    put(
                        "contains_all_strings",
                        buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        },
                    )
                    put("offset", buildJsonObject { put("type", "integer") })
                    put("limit", buildJsonObject { put("type", "integer") })
                    put(
                        "fields",
                        buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        },
                    )
                    put("brief", buildJsonObject { put("type", "boolean") })
                },
            ),
        ) { request ->
            findClassesUsingStringsTool(request)
        }

        server.addLoggedTool(
            name = "find_methods_using_strings",
            description = "使用字符串锚点定位方法候选。建议优先用 brief=true 和 fields 收窄候选，再继续 inspect_method 或 export_method_*。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("workdir", buildJsonObject { put("type", "string") })
                    put(
                        "contains_any_strings",
                        buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        },
                    )
                    put(
                        "contains_all_strings",
                        buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        },
                    )
                    put("offset", buildJsonObject { put("type", "integer") })
                    put("limit", buildJsonObject { put("type", "integer") })
                    put(
                        "fields",
                        buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        },
                    )
                    put("brief", buildJsonObject { put("type", "boolean") })
                },
            ),
        ) { request ->
            findMethodsUsingStringsTool(request)
        }

        server.addLoggedTool(
            name = "list_res",
            description = "列出当前 target 可见的资源条目索引。建议优先用 brief=true 和 fields 收窄结果。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("workdir", buildJsonObject { put("type", "string") })
                    put("type", buildJsonObject { put("type", "string") })
                    put("offset", buildJsonObject { put("type", "integer") })
                    put("limit", buildJsonObject { put("type", "integer") })
                    put(
                        "fields",
                        buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        },
                    )
                    put("brief", buildJsonObject { put("type", "boolean") })
                },
            ),
        ) { request ->
            val context = resolveExecutionContext(request) ?: return@addLoggedTool missingSessionOrWorkdirResult()
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
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("workdir", buildJsonObject { put("type", "string") })
                    put("type", buildJsonObject { put("type", "string") })
                    put("value", buildJsonObject { put("type", "string") })
                    put("contains", buildJsonObject { put("type", "boolean") })
                    put("ignore_case", buildJsonObject { put("type", "boolean") })
                    put("offset", buildJsonObject { put("type", "integer") })
                    put("limit", buildJsonObject { put("type", "integer") })
                    put(
                        "fields",
                        buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        },
                    )
                    put("brief", buildJsonObject { put("type", "boolean") })
                },
                required = listOf("session_id", "type", "value"),
            ),
        ) { request ->
            val context = resolveExecutionContext(request) ?: return@addLoggedTool missingSessionOrWorkdirResult()
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
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("workdir", buildJsonObject { put("type", "string") })
                    put("resource_id", buildJsonObject { put("type", "string") })
                    put("type", buildJsonObject { put("type", "string") })
                    put("name", buildJsonObject { put("type", "string") })
                },
            ),
        ) { request ->
            val context = resolveExecutionContext(request) ?: return@addLoggedTool missingSessionOrWorkdirResult()
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
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("workdir", buildJsonObject { put("type", "string") })
                    put("path", buildJsonObject { put("type", "string") })
                },
                required = listOf("path"),
            ),
        ) { request ->
            val context = resolveExecutionContext(request) ?: return@addLoggedTool missingSessionOrWorkdirResult()
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

        return server
    }

    private fun Server.addLoggedTool(
        name: String,
        description: String,
        inputSchema: ToolSchema,
        handler: suspend (CallToolRequest) -> CallToolResult,
    ) {
        addTool(
            name = name,
            description = description,
            inputSchema = inputSchema,
        ) { request ->
            val summary = summarizeToolArguments(request.arguments)
            McpRuntimeDiagnostics.toolStarted(name, summary)
            try {
                handler(request).also { result ->
                    McpRuntimeDiagnostics.toolFinished(name, result.isError == true)
                }
            } catch (cause: Throwable) {
                McpRuntimeDiagnostics.toolFailed(name, cause)
                throw cause
            }
        }
    }

    private fun summarizeToolArguments(arguments: JsonObject?): String {
        if (arguments == null || arguments.isEmpty()) return ""
        val parts = mutableListOf<String>()
        val keys = arguments.keys.sorted()
        parts += "keys=${keys.joinToString(prefix = "[", postfix = "]")}"
        appendToolSummary(parts, arguments, "session_id")
        appendToolSummary(parts, arguments, "workdir")
        appendToolSummary(parts, arguments, "descriptor")
        appendToolSummary(parts, arguments, "method_handle")
        appendToolSummary(parts, arguments, "class_handle")
        appendToolSummary(parts, arguments, "class_name_contains")
        appendToolSummary(parts, arguments, "method_name_contains")
        appendToolSummary(parts, arguments, "descriptor_contains")
        appendToolSummary(parts, arguments, "type")
        appendToolSummary(parts, arguments, "name")
        appendToolSummary(parts, arguments, "resource_id")
        appendToolSummary(parts, arguments, "value")
        appendArraySummary(parts, arguments, "contains_any_strings")
        appendArraySummary(parts, arguments, "contains_all_strings")
        appendArraySummary(parts, arguments, "include")
        appendArraySummary(parts, arguments, "fields")
        return parts.joinToString(separator = " ")
    }

    private fun appendToolSummary(parts: MutableList<String>, arguments: JsonObject, key: String) {
        val value = arguments[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() } ?: return
        parts += "$key=${value.take(120)}"
    }

    private fun appendArraySummary(parts: MutableList<String>, arguments: JsonObject, key: String) {
        val array = arguments[key] as? JsonArray ?: return
        parts += "$key#=${array.size}"
    }

    fun close() {
        (services.dex as? AutoCloseable)?.close()
    }

    private fun exportMethodTextTool(
        request: CallToolRequest,
        view: String,
        exporter: (WorkspaceContext, String, SourceLocator, String?) -> String,
    ): CallToolResult {
        val context = resolveExecutionContext(request) ?: return missingSessionOrWorkdirResult()
        val session = context.session
        val methodRef = try {
            resolveMethodReference(request, session)
        } catch (cause: IllegalArgumentException) {
            return errorResult(cause.message.orEmpty())
        }
        val descriptor = methodRef?.descriptor.orEmpty()
        if (descriptor.isEmpty()) {
            return errorResult("method_handle or descriptor is required")
        }
        val locator = request.toSourceLocator(methodRef)
        val mode = request.arguments?.get("mode")?.jsonPrimitive?.content?.trim()

        return try {
            val text = exporter(context.workspace, descriptor, locator, mode)
            CallToolResult(
                content = listOf(
                    TextContent(
                        json.encodeToString(
                            ExportTextResult.serializer(),
                            context.toExportTextResult(descriptor = descriptor, view = view, text = text),
                        ),
                    ),
                ),
            )
        } catch (cause: IllegalArgumentException) {
            CallToolResult(
                content = listOf(TextContent("""{"error":"${cause.message}"}""")),
                isError = true,
            )
        }
    }

    private fun exportClassTextTool(
        request: CallToolRequest,
        view: String,
        exporter: (WorkspaceContext, String, SourceLocator) -> String,
    ): CallToolResult {
        val context = resolveExecutionContext(request) ?: return missingSessionOrWorkdirResult()
        val session = context.session
        val classRef = try {
            resolveClassReference(request, session)
        } catch (cause: IllegalArgumentException) {
            return errorResult(cause.message.orEmpty())
        }
        val descriptor = classRef?.descriptor.orEmpty()
        if (descriptor.isEmpty()) {
            return errorResult("class_handle or descriptor is required")
        }
        val locator = request.toSourceLocator(classRef)

        return try {
            val text = exporter(context.workspace, descriptor, locator)
            CallToolResult(
                content = listOf(
                    TextContent(
                        json.encodeToString(
                            ExportTextResult.serializer(),
                            context.toExportTextResult(descriptor = descriptor, view = view, text = text),
                        ),
                    ),
                ),
            )
        } catch (cause: IllegalArgumentException) {
            CallToolResult(
                content = listOf(TextContent("""{"error":"${cause.message}"}""")),
                isError = true,
            )
        }
    }

    private fun findClassesUsingStringsTool(request: CallToolRequest): CallToolResult =
        findStringAnchoredItems(
            request = request,
            finder = ::findClassesUsingStrings,
            supportedFields = classFieldNamesWithHandle,
            renderer = { context, items, fields, brief ->
                FindClassesUsingStringsResult.serializer() to context.toFindClassesUsingStringsResult(
                    items,
                    handleProvider = context.session?.let { activeSession ->
                        { hit: ClassHit ->
                            sessionStore.putClassHandle(activeSession.sessionId, hit.className, hit.sourcePath, hit.sourceEntry)
                        }
                    },
                    fields = fields,
                    brief = brief,
                )
            },
        )

    private fun findMethodsUsingStringsTool(request: CallToolRequest): CallToolResult =
        findStringAnchoredItems(
            request = request,
            finder = ::findMethodsUsingStrings,
            supportedFields = methodFieldNamesWithHandle,
            renderer = { context, items, fields, brief ->
                FindMethodsUsingStringsResult.serializer() to context.toFindMethodsUsingStringsResult(
                    items,
                    handleProvider = context.session?.let { activeSession ->
                        { hit: MethodHit ->
                            sessionStore.putMethodHandle(activeSession.sessionId, hit.descriptor, hit.sourcePath, hit.sourceEntry)
                        }
                    },
                    fields = fields,
                    brief = brief,
                )
            },
        )

    private fun <T, S> findStringAnchoredItems(
        request: CallToolRequest,
        finder: (WorkspaceContext, List<String>, List<String>, Int?, Int?) -> T,
        supportedFields: Set<String>,
        renderer: (ExecutionContext, T, Set<String>?, Boolean) -> Pair<kotlinx.serialization.KSerializer<S>, S>,
    ): CallToolResult {
        val context = resolveExecutionContext(request) ?: return missingSessionOrWorkdirResult()
        val session = context.session
        val containsAnyStrings = request.stringArrayArgument("contains_any_strings")
        val containsAllStrings = request.stringArrayArgument("contains_all_strings")
        val offset = request.intArgument("offset")
        val limit = request.intArgument("limit")
        val brief = request.booleanArgument("brief") ?: false
        val fields = try {
            parseRequestedFields(
                request.stringArrayArgument("fields"),
                supported = if (session != null) supportedFields else supportedFields - setOf("methodHandle", "classHandle"),
            )
        } catch (cause: IllegalArgumentException) {
            return errorResult(cause.message.orEmpty())
        }

        val items = try {
            finder(context.workspace, containsAnyStrings, containsAllStrings, offset, limit)
        } catch (cause: IllegalArgumentException) {
            return errorResult(cause.message.orEmpty())
        }

        val (serializer, payload) = renderer(context, items, fields, brief)
        return CallToolResult(
            content = listOf(TextContent(json.encodeToString(serializer, payload))),
        )
    }

    private fun resolveExecutionContext(
        request: CallToolRequest,
    ): ExecutionContext? {
        val sessionId = request.optionalStringArgument("session_id")
        if (sessionId != null) {
            val session = sessionStore.getTargetSession(sessionId) ?: return null
            return ExecutionContext(session = session, workspace = session.workspace)
        }
        val workdir = request.optionalStringArgument("workdir") ?: return null
        val workspace = services.workspace.open(WorkspaceRef(workdir))
        return ExecutionContext(session = null, workspace = workspace)
    }

    private fun missingSessionOrWorkdirResult(): CallToolResult =
        errorResult("session_id or workdir is required")

    private fun errorResult(message: String): CallToolResult =
        CallToolResult(
            content = listOf(TextContent("""{"error":"$message"}""")),
            isError = true,
        )

    private fun CallToolRequest.toSourceLocator(ref: SourceBackedHandleRef? = null): SourceLocator =
        SourceLocator(
            sourcePath = arguments?.get("source_path")?.jsonPrimitive?.content?.trim()?.ifEmpty { null } ?: ref?.sourcePath,
            sourceEntry = arguments?.get("source_entry")?.jsonPrimitive?.content?.trim()?.ifEmpty { null } ?: ref?.sourceEntry,
        )

    private fun CallToolRequest.requiredStringArgument(name: String): String =
        arguments?.get(name)?.jsonPrimitive?.content?.trim().orEmpty()

    private fun CallToolRequest.optionalStringArgument(name: String): String? =
        arguments?.get(name)?.jsonPrimitive?.content?.trim()?.ifEmpty { null }

    private fun CallToolRequest.stringArrayArgument(name: String): List<String> =
        (arguments?.get(name) as? JsonArray)
            ?.jsonArray
            ?.map { it.jsonPrimitive.content }
            .orEmpty()

    private fun CallToolRequest.intArgument(name: String): Int? =
        arguments?.get(name)?.jsonPrimitive?.content?.toIntOrNull()

    private fun CallToolRequest.booleanArgument(name: String): Boolean? =
        arguments?.get(name)?.jsonPrimitive?.content?.toBooleanStrictOrNull()

    private fun resolveMethodReference(request: CallToolRequest, session: TargetSession?): MethodHandleRef? {
        val handle = request.optionalStringArgument("method_handle")
        if (handle != null) {
            requireNotNull(session) { "method_handle requires session_id" }
            return sessionStore.getMethodHandle(session.sessionId, handle)
                ?: throw IllegalArgumentException(
                    "method_handle not found. Handles must come from a previous dexclub result in the same session; do not construct placeholder handles manually",
                )
        }
        val descriptor = request.optionalStringArgument("descriptor") ?: return null
        return MethodHandleRef(
            sessionId = session?.sessionId.orEmpty(),
            descriptor = descriptor,
            sourcePath = request.optionalStringArgument("source_path"),
            sourceEntry = request.optionalStringArgument("source_entry"),
        )
    }

    private fun resolveClassReference(request: CallToolRequest, session: TargetSession?): ClassHandleRef? {
        val handle = request.optionalStringArgument("class_handle")
        if (handle != null) {
            requireNotNull(session) { "class_handle requires session_id" }
            return sessionStore.getClassHandle(session.sessionId, handle)
                ?: throw IllegalArgumentException(
                    "class_handle not found. Handles must come from a previous dexclub result in the same session; do not construct placeholder handles manually",
                )
        }
        val descriptor = request.optionalStringArgument("descriptor") ?: return null
        return ClassHandleRef(
            sessionId = session?.sessionId.orEmpty(),
            descriptor = descriptor,
            sourcePath = request.optionalStringArgument("source_path"),
            sourceEntry = request.optionalStringArgument("source_entry"),
        )
    }

    internal fun openTargetSession(input: String): TargetSession {
        val workspace = services.workspace.initialize(input)
        return sessionStore.openTargetSession(workspace)
    }

    internal fun listTargetSessions(): List<TargetSession> = sessionStore.listTargetSessions()

    internal fun inspectMethod(
        workspace: WorkspaceContext,
        descriptor: String,
        includes: Set<io.github.dexclub.core.api.dex.MethodDetailSection>,
    ) = services.dex.inspectMethod(
        workspace = workspace,
        request = InspectMethodRequest(
            descriptor = descriptor,
            includes = includes,
        ),
    )

    internal fun inspectManifest(
        workspace: WorkspaceContext,
        includes: Set<io.github.dexclub.core.api.resource.ManifestInspectionSection>,
        includeText: Boolean = false,
    ) = services.resource.inspectManifest(
        workspace = workspace,
        request = InspectManifestRequest(
            includes = includes,
            includeText = includeText,
        ),
    )

    internal fun getTargetSession(sessionId: String): TargetSession? = sessionStore.getTargetSession(sessionId)

    internal fun closeTargetSession(sessionId: String): TargetSession? = sessionStore.closeTargetSession(sessionId)

    internal fun diagnoseTargetSessions(): SessionStoreSnapshot = sessionStore.snapshot()

    internal fun getResourceValue(
        workspace: WorkspaceContext,
        resourceId: String? = null,
        type: String? = null,
        name: String? = null,
    ) = services.resource.getResourceValue(
        workspace = workspace,
        request = ResolveResourceRequest(
            resourceId = resourceId,
            type = type,
            name = name,
        ),
    )

    internal fun decodeXml(
        workspace: WorkspaceContext,
        path: String,
    ) = services.resource.decodeXml(
        workspace = workspace,
        request = DecodeXmlRequest(path = path),
    )

    internal fun exportMethodJavaText(
        workspace: WorkspaceContext,
        descriptor: String,
        source: SourceLocator = SourceLocator(),
        mode: String? = null,
    ): String {
        require(mode.isNullOrBlank()) { "mode is only supported for export_method_smali" }
        return exportTextFile(workspace) {
            services.dex.exportMethodJava(
                workspace = workspace,
                request = ExportMethodJavaRequest(
                    methodSignature = descriptor,
                    source = source,
                    outputPath = it.toString(),
                ),
            )
        }
    }

    internal fun exportMethodSmaliText(
        workspace: WorkspaceContext,
        descriptor: String,
        source: SourceLocator = SourceLocator(),
        mode: String? = null,
    ): String {
        val smaliMode = when (mode?.trim()?.lowercase()) {
            null, "", "snippet" -> MethodSmaliMode.Snippet
            "class" -> MethodSmaliMode.Class
            else -> throw IllegalArgumentException("Unsupported smali mode: $mode")
        }
        return exportTextFile(workspace) {
            services.dex.exportMethodSmali(
                workspace = workspace,
                request = ExportMethodSmaliRequest(
                    methodSignature = descriptor,
                    source = source,
                    outputPath = it.toString(),
                    mode = smaliMode,
                ),
            )
        }
    }

    internal fun exportClassJavaText(
        workspace: WorkspaceContext,
        descriptor: String,
        source: SourceLocator = SourceLocator(),
    ): String = exportTextFile(workspace) {
        services.dex.exportClassJava(
            workspace = workspace,
            request = ExportClassJavaRequest(
                className = descriptor,
                source = source,
                outputPath = it.toString(),
            ),
        )
    }

    internal fun listResources(
        workspace: WorkspaceContext,
        type: String? = null,
        offset: Int? = null,
        limit: Int? = null,
    ): WindowedResourceEntries {
        val normalizedType = type?.trim()?.ifEmpty { null }
        val filtered = services.resource.listResourceEntries(workspace)
            .asSequence()
            .filter { normalizedType == null || it.type == normalizedType }
            .toList()
        return applyResourceEntryWindow(filtered, offset, limit)
    }

    internal fun findResourceValues(
        workspace: WorkspaceContext,
        type: String,
        value: String,
        contains: Boolean = false,
        ignoreCase: Boolean = false,
        offset: Int? = null,
        limit: Int? = null,
    ): WindowedResourceValueHits {
        require(type.isNotBlank()) { "type must not be blank" }
        require(value.isNotBlank()) { "value must not be blank" }
        val hits = services.resource.findResourceValues(
            workspace = workspace,
            request = buildFindResourcesRequest(
                type = type.trim(),
                value = value,
                contains = contains,
                ignoreCase = ignoreCase,
            ),
        )
        return applyResourceValueWindow(hits, offset, limit)
    }

    internal fun exportClassSmaliText(
        workspace: WorkspaceContext,
        descriptor: String,
        source: SourceLocator = SourceLocator(),
    ): String = exportTextFile(workspace) {
        services.dex.exportClassSmali(
            workspace = workspace,
            request = ExportClassSmaliRequest(
                className = descriptor,
                source = source,
                outputPath = it.toString(),
            ),
        )
    }

    private inline fun exportTextFile(workspace: WorkspaceContext, block: (Path) -> Unit): String {
        val exportTempRoot = Paths.get(
            workspace.dexclubDir,
            "targets",
            workspace.activeTargetId,
            "cache",
            "exports",
            "tmp",
        )
        Files.createDirectories(exportTempRoot)
        val output = Files.createTempFile(exportTempRoot, "mcp-export-", ".txt")
        return try {
            block(output)
            output.readText()
        } finally {
            output.deleteIfExists()
        }
    }

    internal fun findClassesUsingStrings(
        workspace: WorkspaceContext,
        containsAnyStrings: List<String>,
        containsAllStrings: List<String>,
        offset: Int? = null,
        limit: Int? = null,
    ): WindowedClassHits {
        if (containsAnyStrings.isEmpty() && containsAllStrings.isEmpty()) {
            throw IllegalArgumentException("At least one string filter is required")
        }

        val anyHits = if (containsAnyStrings.isNotEmpty()) {
            services.dex.findClassesUsingStrings(
                workspace = workspace,
                request = buildFindClassesUsingStringsRequest(
                    strings = containsAnyStrings,
                    requireAll = false,
                ),
            )
        } else {
            emptyList()
        }

        val allHits = if (containsAllStrings.isNotEmpty()) {
            services.dex.findClassesUsingStrings(
                workspace = workspace,
                request = buildFindClassesUsingStringsRequest(
                    strings = containsAllStrings,
                    requireAll = true,
                ),
            )
        } else {
            emptyList()
        }

        val combined = when {
            containsAnyStrings.isNotEmpty() && containsAllStrings.isNotEmpty() ->
                anyHits.intersect(allHits.toSet()).toList()
            containsAnyStrings.isNotEmpty() -> anyHits
            else -> allHits
        }.sortedWith(
            compareBy<ClassHit>(
                { it.className },
                { it.sourcePath.orEmpty() },
                { it.sourceEntry.orEmpty() },
            ),
        )

        return applyClassWindow(
            items = combined,
            offset = offset,
            limit = limit,
        )
    }

    internal fun findMethodsUsingStrings(
        workspace: WorkspaceContext,
        containsAnyStrings: List<String>,
        containsAllStrings: List<String>,
        offset: Int? = null,
        limit: Int? = null,
    ): WindowedMethodHits {
        if (containsAnyStrings.isEmpty() && containsAllStrings.isEmpty()) {
            throw IllegalArgumentException("At least one string filter is required")
        }

        val anyHits = if (containsAnyStrings.isNotEmpty()) {
            services.dex.findMethodsUsingStrings(
                workspace = workspace,
                request = buildFindMethodsUsingStringsRequest(
                    strings = containsAnyStrings,
                    requireAll = false,
                ),
            )
        } else {
            emptyList()
        }

        val allHits = if (containsAllStrings.isNotEmpty()) {
            services.dex.findMethodsUsingStrings(
                workspace = workspace,
                request = buildFindMethodsUsingStringsRequest(
                    strings = containsAllStrings,
                    requireAll = true,
                ),
            )
        } else {
            emptyList()
        }

        val combined = when {
            containsAnyStrings.isNotEmpty() && containsAllStrings.isNotEmpty() ->
                anyHits.intersect(allHits.toSet()).toList()
            containsAnyStrings.isNotEmpty() -> anyHits
            else -> allHits
        }.sortedWith(
            compareBy<MethodHit>(
                { it.className },
                { it.methodName },
                { it.descriptor },
                { it.sourcePath.orEmpty() },
                { it.sourceEntry.orEmpty() },
            ),
        )

        return applyMethodWindow(
            items = combined,
            offset = offset,
            limit = limit,
        )
    }

    internal fun findMethods(
        workspace: WorkspaceContext,
        classNameContains: String? = null,
        methodNameContains: String? = null,
        descriptorContains: String? = null,
        offset: Int? = null,
        limit: Int? = null,
    ): WindowedMethodHits {
        val normalizedDescriptor = descriptorContains?.trim()?.ifEmpty { null }
        val request = buildFindMethodsRequest(
            classNameContains = classNameContains,
            methodNameContains = methodNameContains,
        )
        val baseHits = services.dex.findMethods(
            workspace = workspace,
            request = request,
        )
        val filtered = if (normalizedDescriptor == null) {
            baseHits
        } else {
            baseHits.filter { it.descriptor.contains(normalizedDescriptor, ignoreCase = false) }
        }.sortedWith(
            compareBy<MethodHit>(
                { it.className },
                { it.methodName },
                { it.descriptor },
                { it.sourcePath.orEmpty() },
                { it.sourceEntry.orEmpty() },
            ),
        )
        return applyMethodWindow(
            items = filtered,
            offset = offset,
            limit = limit,
        )
    }
}
