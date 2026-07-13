package io.github.dexclub.mcp

import io.modelcontextprotocol.kotlin.sdk.server.Server

internal fun McpApp.registerResourceTools(server: Server) {
    registerCatalogTool(server, McpResourceToolCatalog.require("manifest")) { request -> manifestTool(request) }

    registerCatalogTool(server, McpResourceToolCatalog.require("list_res")) { request -> listResourcesTool(request) }

    registerCatalogTool(server, McpResourceToolCatalog.require("find_resource_values")) { request ->
        findResourceValuesTool(request)
    }

    registerCatalogTool(server, McpResourceToolCatalog.require("get_resource_value")) { request ->
        getResourceValueTool(request)
    }

    registerCatalogTool(server, McpResourceToolCatalog.require("decode_xml")) { request -> decodeXmlTool(request) }
}

internal fun McpApp.manifestTool(request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest) =
    runToolCatching {
        val context = when (val resolution = executionContextOrFailureResult(request)) {
            is ExecutionContextResolution.Ready -> resolution.context
            is ExecutionContextResolution.Failed -> return@runToolCatching resolution.result
        }
        val includes = request.manifestIncludeSections()
        val includeText = request.includeTextFlag()
        val manifest = inspectManifestExecution(
            workspace = context.workspace,
            includes = includes,
            includeText = includeText,
        )
        manifestResult(manifest)
    }

internal fun McpApp.listResourcesTool(request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest) =
    runToolCatching {
        val context = when (val resolution = executionContextOrFailureResult(request)) {
            is ExecutionContextResolution.Ready -> resolution.context
            is ExecutionContextResolution.Failed -> return@runToolCatching resolution.result
        }
        val type = request.optionalStringArgument("type")
        val offset = request.intArgument("offset")
        val limit = request.intArgument("limit")
        val brief = request.briefFlag()
        val fields = request.resourceEntryProjectionFields()
        val entries = listResourcesExecution(
            workspace = context.workspace,
            type = type,
            offset = offset,
            limit = limit,
        )
        listResourcesResult(entries, fields = fields, brief = brief)
    }

internal fun McpApp.findResourceValuesTool(request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest) =
    runToolCatching {
        val context = when (val resolution = executionContextOrFailureResult(request)) {
            is ExecutionContextResolution.Ready -> resolution.context
            is ExecutionContextResolution.Failed -> return@runToolCatching resolution.result
        }
        val type = request.optionalStringArgument("type")
            ?: return@runToolCatching missingRequiredArgumentsResult("type", "value")
        val value = request.optionalStringArgument("value")
            ?: return@runToolCatching missingRequiredArgumentsResult("type", "value")
        val contains = request.booleanArgument("contains") ?: false
        val ignoreCase = request.booleanArgument("ignore_case") ?: false
        val offset = request.intArgument("offset")
        val limit = request.intArgument("limit")
        val brief = request.briefFlag()
        val fields = request.resourceValueProjectionFields()
        val hits = findResourceValuesExecution(
            workspace = context.workspace,
            type = type,
            value = value,
            contains = contains,
            ignoreCase = ignoreCase,
            offset = offset,
            limit = limit,
        )
        findResourcesResult(hits, fields = fields, brief = brief)
    }

internal fun McpApp.getResourceValueTool(request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest) =
    runToolCatching {
        val context = when (val resolution = executionContextOrFailureResult(request)) {
            is ExecutionContextResolution.Ready -> resolution.context
            is ExecutionContextResolution.Failed -> return@runToolCatching resolution.result
        }
        val resourceId = request.optionalStringArgument("resource_id")
        val type = request.optionalStringArgument("type")
        val name = request.optionalStringArgument("name")
        if (resourceId == null && (type == null || name == null)) {
            return@runToolCatching missingAnyOfRequiredArgumentsResult("resource_id", "type+name")
        }
        val resource = getResourceValueExecution(
            workspace = context.workspace,
            resourceId = resourceId,
            type = type,
            name = name,
        )
        resolveResourceResult(resource)
    }

internal fun McpApp.decodeXmlTool(request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest) =
    runToolCatching {
        val context = when (val resolution = executionContextOrFailureResult(request)) {
            is ExecutionContextResolution.Ready -> resolution.context
            is ExecutionContextResolution.Failed -> return@runToolCatching resolution.result
        }
        val path = request.optionalStringArgument("path")
            ?: return@runToolCatching missingRequiredArgumentsResult("path")
        val xml = decodeXmlExecution(
            workspace = context.workspace,
            path = path,
        )
        decodeXmlResult(xml)
    }
