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
        val componentName = request.optionalStringArgument("component_name")
        val componentType = request.optionalStringArgument("component_type")?.toManifestComponentType()
        val manifest = inspectManifestExecution(
            workspace = context.workspace,
            includes = includes,
            includeText = includeText,
            componentName = componentName,
            componentType = componentType,
        )
        manifestResult(manifest)
    }

internal fun McpApp.listResourcesTool(request: io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest) =
    runToolCatching {
        val context = when (val resolution = executionContextOrFailureResult(request)) {
            is ExecutionContextResolution.Ready -> resolution.context
            is ExecutionContextResolution.Failed -> return@runToolCatching resolution.result
        }
        val resourceId = request.optionalStringArgument("resource_id")
        val packageName = request.optionalStringArgument("package_name")
        val type = request.optionalStringArgument("resource_type")
        val name = request.optionalStringArgument("name")
        val filePath = request.optionalStringArgument("file_path")
        val resolutionFilter = request.optionalStringArgument("resolution")?.toResourceResolution()
        val offset = request.intArgument("offset")
        val limit = request.intArgument("limit")
        val brief = request.briefFlag()
        val fields = request.resourceEntryProjectionFields()
        val entries = listResourcesExecution(
            workspace = context.workspace,
            resourceId = resourceId,
            packageName = packageName,
            type = type,
            name = name,
            filePath = filePath,
            resolution = resolutionFilter,
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
        val type = request.optionalStringArgument("resource_type")
            ?: return@runToolCatching missingRequiredArgumentsResult("resource_type")
        val value = request.optionalStringArgument("value")
            ?: return@runToolCatching missingRequiredArgumentsResult("value")
        val contains = request.booleanArgument("contains") ?: false
        val ignoreCase = request.booleanArgument("ignore_case") ?: false
        val packageName = request.optionalStringArgument("package_name")
        val qualifier = request.optionalStringArgument("qualifier")
        val valueKind = request.optionalStringArgument("value_kind")
        val matchTarget = request.optionalStringArgument("match_target")
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
            packageName = packageName,
            qualifier = qualifier,
            valueKind = valueKind,
            matchTarget = matchTarget,
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
        val packageName = request.optionalStringArgument("package_name")
        val type = request.optionalStringArgument("resource_type")
        val name = request.optionalStringArgument("name")
        val qualifier = request.optionalStringArgument("qualifier")
        val includeAllVariants = request.booleanArgument("include_all_variants") ?: false
        if (resourceId == null && (type == null || name == null)) {
            return@runToolCatching missingAnyOfRequiredArgumentsResult("resource_id", "resource_type+name")
        }
        val resource = getResourceValueExecution(
            workspace = context.workspace,
            resourceId = resourceId,
            packageName = packageName,
            type = type,
            name = name,
            qualifier = qualifier,
            includeAllVariants = includeAllVariants,
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

private fun String.toResourceResolution(): io.github.dexclub.core.app.contract.ResourceResolution =
    when (this) {
        "table-backed" -> io.github.dexclub.core.app.contract.ResourceResolution.TableBacked
        "table-value" -> io.github.dexclub.core.app.contract.ResourceResolution.TableValue
        "unresolved" -> io.github.dexclub.core.app.contract.ResourceResolution.Unresolved
        "table-hole" -> io.github.dexclub.core.app.contract.ResourceResolution.TableHole
        else -> throw IllegalArgumentException("Unsupported resource resolution: $this")
    }

private fun String.toManifestComponentType(): io.github.dexclub.core.app.contract.ManifestComponentType =
    when (this) {
        "activity" -> io.github.dexclub.core.app.contract.ManifestComponentType.Activity
        "activity-alias" -> io.github.dexclub.core.app.contract.ManifestComponentType.ActivityAlias
        "service" -> io.github.dexclub.core.app.contract.ManifestComponentType.Service
        "receiver" -> io.github.dexclub.core.app.contract.ManifestComponentType.Receiver
        "provider" -> io.github.dexclub.core.app.contract.ManifestComponentType.Provider
        else -> throw IllegalArgumentException("Unsupported manifest component type: $this")
    }
