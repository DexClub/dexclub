package io.github.dexclub.mcp

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.ExportClassJavaRequest
import io.github.dexclub.core.api.dex.ExportClassSmaliRequest
import io.github.dexclub.core.api.dex.ExportMethodJavaRequest
import io.github.dexclub.core.api.dex.ExportMethodSmaliRequest
import io.github.dexclub.core.api.dex.InspectMethodRequest
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.resource.InspectManifestRequest
import io.github.dexclub.core.api.shared.MethodSmaliMode
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.shared.Services
import io.github.dexclub.core.api.shared.createDefaultServices
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
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
    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
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

        server.addTool(
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
                return@addTool CallToolResult(
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

        server.addTool(
            name = "inspect_method",
            description = "基于已打开的 target session 检查方法的一层事实视图。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("descriptor", buildJsonObject { put("type", "string") })
                    put(
                        "include",
                        buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        },
                    )
                },
                required = listOf("session_id", "descriptor"),
            ),
        ) { request ->
            val session = resolveRequiredSession(request) ?: return@addTool missingSessionResult(request)
            val descriptor = request.requiredStringArgument("descriptor")
            if (descriptor.isEmpty()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent("""{"error":"descriptor is required"}""")),
                    isError = true,
                )
            }

            val includes = try {
                parseMethodDetailSections(
                    (request.arguments?.get("include") as? JsonArray)
                        ?.jsonArray
                        ?.map { it.jsonPrimitive.content },
                )
            } catch (cause: IllegalArgumentException) {
                return@addTool CallToolResult(
                    content = listOf(TextContent("""{"error":"${cause.message}"}""")),
                    isError = true,
                )
            }

            val detail = inspectMethod(
                session = session,
                descriptor = descriptor,
                includes = includes,
            )
            CallToolResult(
                content = listOf(
                    TextContent(
                        json.encodeToString(
                            InspectMethodResult.serializer(),
                            session.toInspectMethodResult(detail),
                        ),
                    ),
                ),
            )
        }

        server.addTool(
            name = "manifest",
            description = "返回当前 target 的结构化 manifest 视图，可按需附带原始 XML。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
                    put(
                        "include",
                        buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        },
                    )
                    put("include_text", buildJsonObject { put("type", "boolean") })
                },
                required = listOf("session_id"),
            ),
        ) { request ->
            val session = resolveRequiredSession(request) ?: return@addTool missingSessionResult(request)
            val includes = try {
                parseManifestInspectionSections(
                    (request.arguments?.get("include") as? JsonArray)
                        ?.jsonArray
                        ?.map { it.jsonPrimitive.content },
                )
            } catch (cause: IllegalArgumentException) {
                return@addTool CallToolResult(
                    content = listOf(TextContent("""{"error":"${cause.message}"}""")),
                    isError = true,
                )
            }
            val includeText = request.arguments?.get("include_text")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
            val manifest = inspectManifest(session, includes, includeText)
            CallToolResult(
                content = listOf(
                    TextContent(
                        json.encodeToString(
                            ManifestDecodeResult.serializer(),
                            session.toManifestDecodeResult(manifest),
                        ),
                    ),
                ),
            )
        }

        server.addTool(
            name = "export_method_java",
            description = "导出单方法的 Java 语义视图。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("descriptor", buildJsonObject { put("type", "string") })
                    put("source_path", buildJsonObject { put("type", "string") })
                    put("source_entry", buildJsonObject { put("type", "string") })
                },
                required = listOf("session_id", "descriptor"),
            ),
        ) { request ->
            exportMethodTextTool(
                request = request,
                view = "java",
                exporter = ::exportMethodJavaText,
            )
        }

        server.addTool(
            name = "export_method_smali",
            description = "导出单方法的 smali 原始证据视图。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("descriptor", buildJsonObject { put("type", "string") })
                    put("source_path", buildJsonObject { put("type", "string") })
                    put("source_entry", buildJsonObject { put("type", "string") })
                    put("mode", buildJsonObject { put("type", "string") })
                },
                required = listOf("session_id", "descriptor"),
            ),
        ) { request ->
            exportMethodTextTool(
                request = request,
                view = "smali",
                exporter = ::exportMethodSmaliText,
            )
        }

        server.addTool(
            name = "export_class_java",
            description = "导出整类的 Java 语义视图。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("descriptor", buildJsonObject { put("type", "string") })
                    put("source_path", buildJsonObject { put("type", "string") })
                    put("source_entry", buildJsonObject { put("type", "string") })
                },
                required = listOf("session_id", "descriptor"),
            ),
        ) { request ->
            exportClassTextTool(
                request = request,
                view = "java",
                exporter = ::exportClassJavaText,
            )
        }

        server.addTool(
            name = "export_class_smali",
            description = "导出整类的 smali 原始证据视图。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
                    put("descriptor", buildJsonObject { put("type", "string") })
                    put("source_path", buildJsonObject { put("type", "string") })
                    put("source_entry", buildJsonObject { put("type", "string") })
                },
                required = listOf("session_id", "descriptor"),
            ),
        ) { request ->
            exportClassTextTool(
                request = request,
                view = "smali",
                exporter = ::exportClassSmaliText,
            )
        }

        server.addTool(
            name = "find_classes_using_strings",
            description = "使用字符串锚点定位类候选。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
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
                },
                required = listOf("session_id"),
            ),
        ) { request ->
            findClassesUsingStringsTool(request)
        }

        server.addTool(
            name = "find_methods_using_strings",
            description = "使用字符串锚点定位方法候选。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("session_id", buildJsonObject { put("type", "string") })
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
                },
                required = listOf("session_id"),
            ),
        ) { request ->
            findMethodsUsingStringsTool(request)
        }

        return server
    }

    fun createTransport(): StdioServerTransport =
        StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered(),
        )

    fun close() {
        (services.dex as? AutoCloseable)?.close()
    }

    private fun exportMethodTextTool(
        request: CallToolRequest,
        view: String,
        exporter: (TargetSession, String, SourceLocator, String?) -> String,
    ): CallToolResult {
        val session = resolveRequiredSession(request) ?: return missingSessionResult(request)
        val descriptor = request.requiredStringArgument("descriptor")
        if (descriptor.isEmpty()) {
            return CallToolResult(
                content = listOf(TextContent("""{"error":"descriptor is required"}""")),
                isError = true,
            )
        }
        val locator = request.toSourceLocator()
        val mode = request.arguments?.get("mode")?.jsonPrimitive?.content?.trim()

        return try {
            val text = exporter(session, descriptor, locator, mode)
            CallToolResult(
                content = listOf(
                    TextContent(
                        json.encodeToString(
                            ExportTextResult.serializer(),
                            session.toExportTextResult(descriptor = descriptor, view = view, text = text),
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
        exporter: (TargetSession, String, SourceLocator) -> String,
    ): CallToolResult {
        val session = resolveRequiredSession(request) ?: return missingSessionResult(request)
        val descriptor = request.requiredStringArgument("descriptor")
        if (descriptor.isEmpty()) {
            return CallToolResult(
                content = listOf(TextContent("""{"error":"descriptor is required"}""")),
                isError = true,
            )
        }
        val locator = request.toSourceLocator()

        return try {
            val text = exporter(session, descriptor, locator)
            CallToolResult(
                content = listOf(
                    TextContent(
                        json.encodeToString(
                            ExportTextResult.serializer(),
                            session.toExportTextResult(descriptor = descriptor, view = view, text = text),
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
            renderer = { session, items ->
                FindClassesUsingStringsResult.serializer() to session.toFindClassesUsingStringsResult(items)
            },
        )

    private fun findMethodsUsingStringsTool(request: CallToolRequest): CallToolResult =
        findStringAnchoredItems(
            request = request,
            finder = ::findMethodsUsingStrings,
            renderer = { session, items ->
                FindMethodsUsingStringsResult.serializer() to session.toFindMethodsUsingStringsResult(items)
            },
        )

    private fun <T, S> findStringAnchoredItems(
        request: CallToolRequest,
        finder: (TargetSession, List<String>, List<String>, Int?, Int?) -> T,
        renderer: (TargetSession, T) -> Pair<kotlinx.serialization.KSerializer<S>, S>,
    ): CallToolResult {
        val session = resolveRequiredSession(request) ?: return missingSessionResult(request)
        val containsAnyStrings = request.stringArrayArgument("contains_any_strings")
        val containsAllStrings = request.stringArrayArgument("contains_all_strings")
        val offset = request.intArgument("offset")
        val limit = request.intArgument("limit")

        val items = try {
            finder(session, containsAnyStrings, containsAllStrings, offset, limit)
        } catch (cause: IllegalArgumentException) {
            return errorResult(cause.message.orEmpty())
        }

        val (serializer, payload) = renderer(session, items)
        return CallToolResult(
            content = listOf(TextContent(json.encodeToString(serializer, payload))),
        )
    }

    private fun resolveRequiredSession(
        request: CallToolRequest,
    ): TargetSession? {
        val sessionId = request.requiredStringArgument("session_id")
        if (sessionId.isEmpty()) return null
        return sessionStore.getTargetSession(sessionId)
    }

    private fun missingSessionResult(
        request: CallToolRequest,
    ): CallToolResult {
        val sessionId = request.requiredStringArgument("session_id")
        val error = if (sessionId.isEmpty()) {
            "session_id is required"
        } else {
            "target session not found"
        }
        return errorResult(error)
    }

    private fun errorResult(message: String): CallToolResult =
        CallToolResult(
            content = listOf(TextContent("""{"error":"$message"}""")),
            isError = true,
        )

    private fun CallToolRequest.toSourceLocator(): SourceLocator =
        SourceLocator(
            sourcePath = arguments?.get("source_path")?.jsonPrimitive?.content?.trim()?.ifEmpty { null },
            sourceEntry = arguments?.get("source_entry")?.jsonPrimitive?.content?.trim()?.ifEmpty { null },
        )

    private fun CallToolRequest.requiredStringArgument(name: String): String =
        arguments?.get(name)?.jsonPrimitive?.content?.trim().orEmpty()

    private fun CallToolRequest.stringArrayArgument(name: String): List<String> =
        (arguments?.get(name) as? JsonArray)
            ?.jsonArray
            ?.map { it.jsonPrimitive.content }
            .orEmpty()

    private fun CallToolRequest.intArgument(name: String): Int? =
        arguments?.get(name)?.jsonPrimitive?.content?.toIntOrNull()

    internal fun openTargetSession(input: String): TargetSession {
        val workspace = services.workspace.initialize(input)
        return sessionStore.openTargetSession(workspace)
    }

    internal fun inspectMethod(
        session: TargetSession,
        descriptor: String,
        includes: Set<io.github.dexclub.core.api.dex.MethodDetailSection>,
    ) = services.dex.inspectMethod(
        workspace = session.workspace,
        request = InspectMethodRequest(
            descriptor = descriptor,
            includes = includes,
        ),
    )

    internal fun inspectManifest(
        session: TargetSession,
        includes: Set<io.github.dexclub.core.api.resource.ManifestInspectionSection>,
        includeText: Boolean = false,
    ) = services.resource.inspectManifest(
        workspace = session.workspace,
        request = InspectManifestRequest(
            includes = includes,
            includeText = includeText,
        ),
    )

    internal fun getTargetSession(sessionId: String): TargetSession? = sessionStore.getTargetSession(sessionId)

    internal fun exportMethodJavaText(
        session: TargetSession,
        descriptor: String,
        source: SourceLocator = SourceLocator(),
        mode: String? = null,
    ): String {
        require(mode.isNullOrBlank()) { "mode is only supported for export_method_smali" }
        return exportTextFile(session) {
            services.dex.exportMethodJava(
                workspace = session.workspace,
                request = ExportMethodJavaRequest(
                    methodSignature = descriptor,
                    source = source,
                    outputPath = it.toString(),
                ),
            )
        }
    }

    internal fun exportMethodSmaliText(
        session: TargetSession,
        descriptor: String,
        source: SourceLocator = SourceLocator(),
        mode: String? = null,
    ): String {
        val smaliMode = when (mode?.trim()?.lowercase()) {
            null, "", "snippet" -> MethodSmaliMode.Snippet
            "class" -> MethodSmaliMode.Class
            else -> throw IllegalArgumentException("Unsupported smali mode: $mode")
        }
        return exportTextFile(session) {
            services.dex.exportMethodSmali(
                workspace = session.workspace,
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
        session: TargetSession,
        descriptor: String,
        source: SourceLocator = SourceLocator(),
    ): String = exportTextFile(session) {
        services.dex.exportClassJava(
            workspace = session.workspace,
            request = ExportClassJavaRequest(
                className = descriptor,
                source = source,
                outputPath = it.toString(),
            ),
        )
    }

    internal fun exportClassSmaliText(
        session: TargetSession,
        descriptor: String,
        source: SourceLocator = SourceLocator(),
    ): String = exportTextFile(session) {
        services.dex.exportClassSmali(
            workspace = session.workspace,
            request = ExportClassSmaliRequest(
                className = descriptor,
                source = source,
                outputPath = it.toString(),
            ),
        )
    }

    private inline fun exportTextFile(session: TargetSession, block: (Path) -> Unit): String {
        val exportTempRoot = Paths.get(
            session.workspace.dexclubDir,
            "targets",
            session.workspace.activeTargetId,
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
        session: TargetSession,
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
                workspace = session.workspace,
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
                workspace = session.workspace,
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

        return applyWindow(
            items = combined,
            offset = offset,
            limit = limit,
        )
    }

    internal fun findMethodsUsingStrings(
        session: TargetSession,
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
                workspace = session.workspace,
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
                workspace = session.workspace,
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

        return applyWindow(
            items = combined,
            offset = offset,
            limit = limit,
        )
    }
}
