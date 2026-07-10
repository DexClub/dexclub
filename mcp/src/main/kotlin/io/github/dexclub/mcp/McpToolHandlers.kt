package io.github.dexclub.mcp

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.jsonPrimitive

internal fun McpApp.exportMethodTextTool(
    request: CallToolRequest,
    view: String,
    exporter: (WorkspaceContext, String, SourceLocator, String?) -> String,
): CallToolResult {
    val context = when (val resolution = executionContextOrFailureResult(request)) {
        is ExecutionContextResolution.Ready -> resolution.context
        is ExecutionContextResolution.Failed -> return resolution.result
    }
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
        errorResult(cause.message.orEmpty())
    }
}

internal fun McpApp.exportClassTextTool(
    request: CallToolRequest,
    view: String,
    exporter: (WorkspaceContext, String, SourceLocator) -> String,
): CallToolResult {
    val context = when (val resolution = executionContextOrFailureResult(request)) {
        is ExecutionContextResolution.Ready -> resolution.context
        is ExecutionContextResolution.Failed -> return resolution.result
    }
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
        errorResult(cause.message.orEmpty())
    }
}

internal fun McpApp.findClassesUsingStringsTool(request: CallToolRequest): CallToolResult =
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

internal fun McpApp.findMethodsUsingStringsTool(request: CallToolRequest): CallToolResult =
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

internal fun <T, S> McpApp.findStringAnchoredItems(
    request: CallToolRequest,
    finder: (WorkspaceContext, List<String>, List<String>, Int?, Int?) -> T,
    supportedFields: Set<String>,
    renderer: (ExecutionContext, T, Set<String>?, Boolean) -> Pair<KSerializer<S>, S>,
): CallToolResult {
    val context = when (val resolution = executionContextOrFailureResult(request)) {
        is ExecutionContextResolution.Ready -> resolution.context
        is ExecutionContextResolution.Failed -> return resolution.result
    }
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
            sessionRequiredFields = supportedFields.intersect(setOf("methodHandle", "classHandle")),
            hasSession = session != null,
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
