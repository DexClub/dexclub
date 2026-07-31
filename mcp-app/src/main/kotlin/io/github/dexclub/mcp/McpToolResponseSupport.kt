package io.github.dexclub.mcp

import io.github.dexclub.core.app.contract.ClassHit
import io.github.dexclub.core.app.contract.DexQueryError
import io.github.dexclub.core.app.contract.MethodHit
import io.github.dexclub.core.app.dex.ExportClassTextUseCaseResult
import io.github.dexclub.core.app.dex.ExportMethodTextUseCaseResult
import io.github.dexclub.core.app.dex.FindClassesUseCaseResult
import io.github.dexclub.core.app.dex.FindFieldsUseCaseResult
import io.github.dexclub.core.app.dex.FindMethodsUseCaseResult
import io.github.dexclub.core.app.dex.InspectMethodUseCaseResult
import io.github.dexclub.core.app.resource.DecodeXmlUseCaseResult
import io.github.dexclub.core.app.resource.FindResourceValuesUseCaseResult
import io.github.dexclub.core.app.resource.GetResourceValueUseCaseResult
import io.github.dexclub.core.app.resource.InspectManifestUseCaseResult
import io.github.dexclub.core.app.resource.ListResourcesUseCaseResult
import io.github.dexclub.core.app.contract.ResourceDecodeError
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.KSerializer

internal fun <T> McpApp.successResult(serializer: KSerializer<T>, payload: T): CallToolResult =
    CallToolResult(
        content = listOf(TextContent(json.encodeToString(serializer, payload))),
    )

internal inline fun McpApp.runToolCatching(block: () -> CallToolResult): CallToolResult =
    try {
        block()
    } catch (cause: NoSuchElementException) {
        val session = cause.message?.removePrefix("session_id not found: ").orEmpty()
        missingSessionResult(session)
    } catch (cause: ResourceDecodeError) {
        resourceErrorResult(cause)
    } catch (cause: DexQueryError) {
        errorResult(cause.message.orEmpty(), code = "invalid_query")
    } catch (cause: IllegalArgumentException) {
        errorResult(cause.message.orEmpty(), code = "invalid_argument")
    } catch (cause: Exception) {
        internalErrorResult(cause)
    }

internal fun McpApp.inspectMethodResult(
    execution: InspectMethodUseCaseResult,
    brief: Boolean,
): CallToolResult =
    successResult(InspectMethodResult.serializer(), execution.toInspectMethodResult(brief = brief))

internal fun McpApp.findMethodsResult(
    execution: FindMethodsUseCaseResult,
    handleProvider: ((MethodHit) -> String)?,
    fields: Set<String>?,
    brief: Boolean,
): CallToolResult =
    successResult(
        FindMethodsResult.serializer(),
        execution.toFindMethodsResult(
            handleProvider = handleProvider,
            fields = fields,
            brief = brief,
        ),
    )

internal fun McpApp.findClassesResult(
    execution: FindClassesUseCaseResult,
    handleProvider: ((ClassHit) -> String)?,
    fields: Set<String>?,
    brief: Boolean,
): CallToolResult =
    successResult(
        FindClassesResult.serializer(),
        execution.toFindClassesResult(
            handleProvider = handleProvider,
            fields = fields,
            brief = brief,
        ),
    )

internal fun McpApp.findFieldsResult(
    execution: FindFieldsUseCaseResult,
    fields: Set<String>?,
    brief: Boolean,
): CallToolResult =
    successResult(
        FindFieldsResult.serializer(),
        execution.toFindFieldsResult(
            fields = fields,
            brief = brief,
        ),
    )

internal fun McpApp.exportTextResult(execution: ExportMethodTextUseCaseResult, view: String): CallToolResult =
    successResult(ExportTextResult.serializer(), execution.toExportTextResult(view = view))

internal fun McpApp.exportTextResult(execution: ExportClassTextUseCaseResult, view: String): CallToolResult =
    successResult(ExportTextResult.serializer(), execution.toExportTextResult(view = view))

internal fun McpApp.manifestResult(execution: InspectManifestUseCaseResult): CallToolResult =
    successResult(ManifestDecodeResult.serializer(), execution.toManifestDecodeResult())

internal fun McpApp.listResourcesResult(
    execution: ListResourcesUseCaseResult,
    fields: Set<String>?,
    brief: Boolean,
): CallToolResult =
    successResult(
        ListResourcesResult.serializer(),
        execution.toListResourcesResult(fields = fields, brief = brief),
    )

internal fun McpApp.findResourcesResult(
    execution: FindResourceValuesUseCaseResult,
    fields: Set<String>?,
    brief: Boolean,
): CallToolResult =
    successResult(
        FindResourcesResult.serializer(),
        execution.toFindResourcesResult(fields = fields, brief = brief),
    )

internal fun McpApp.resolveResourceResult(execution: GetResourceValueUseCaseResult): CallToolResult =
    successResult(ResolveResourceResult.serializer(), execution.toResolveResourceResult())

internal fun McpApp.decodeXmlResult(execution: DecodeXmlUseCaseResult): CallToolResult =
    successResult(DecodeXmlResult.serializer(), execution.toDecodeXmlResult())

