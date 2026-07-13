package io.github.dexclub.mcp

import io.github.dexclub.core.app.dex.ExportClassTextUseCaseRequest
import io.github.dexclub.core.app.dex.ExportClassTextView
import io.github.dexclub.core.app.dex.ExportMethodTextUseCaseRequest
import io.github.dexclub.core.app.dex.ExportMethodTextView
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult

internal fun McpApp.exportMethodTextTool(
    request: CallToolRequest,
    view: String,
): CallToolResult =
    runToolCatching {
        val target = request.dexToolTarget()
        val result = appUseCases.dex.exportMethodTextUseCase.execute(
            ExportMethodTextUseCaseRequest(
                sessionId = target.sessionId,
                workdir = target.workdir,
                methodHandle = request.optionalStringArgument("method_handle"),
                descriptor = request.optionalStringArgument("descriptor"),
                sourcePath = request.optionalStringArgument("source_path"),
                sourceEntry = request.optionalStringArgument("source_entry"),
                view = parseExportMethodView(view),
                mode = request.optionalStringArgument("mode"),
            ),
        )
        exportTextResult(result, view)
    }

internal fun McpApp.exportClassTextTool(
    request: CallToolRequest,
    view: String,
): CallToolResult =
    runToolCatching {
        val target = request.dexToolTarget()
        val result = appUseCases.dex.exportClassTextUseCase.execute(
            ExportClassTextUseCaseRequest(
                sessionId = target.sessionId,
                workdir = target.workdir,
                classHandle = request.optionalStringArgument("class_handle"),
                descriptor = request.optionalStringArgument("descriptor"),
                sourcePath = request.optionalStringArgument("source_path"),
                sourceEntry = request.optionalStringArgument("source_entry"),
                view = parseExportClassView(view),
            ),
        )
        exportTextResult(result, view)
    }

private fun parseExportMethodView(view: String): ExportMethodTextView =
    when (view) {
        "java" -> ExportMethodTextView.Java
        "smali" -> ExportMethodTextView.Smali
        else -> throw IllegalArgumentException("Unsupported export view: $view")
    }

private fun parseExportClassView(view: String): ExportClassTextView =
    when (view) {
        "java" -> ExportClassTextView.Java
        "smali" -> ExportClassTextView.Smali
        else -> throw IllegalArgumentException("Unsupported export view: $view")
    }
