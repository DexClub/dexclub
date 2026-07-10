package io.github.dexclub.mcp

import io.github.dexclub.core.api.resource.DecodeXmlRequest
import io.github.dexclub.core.api.resource.FindResourcesRequest
import io.github.dexclub.core.api.resource.InspectManifestRequest
import io.github.dexclub.core.api.resource.ManifestInspectionSection
import io.github.dexclub.core.api.resource.ResolveResourceRequest
import io.github.dexclub.core.api.workspace.WorkspaceContext

internal fun McpApp.inspectManifest(
    workspace: WorkspaceContext,
    includes: Set<ManifestInspectionSection>,
    includeText: Boolean = false,
) = services.resource.inspectManifest(
    workspace = workspace,
    request = InspectManifestRequest(
        includes = includes,
        includeText = includeText,
    ),
)

internal fun McpApp.getResourceValue(
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

internal fun McpApp.decodeXml(
    workspace: WorkspaceContext,
    path: String,
) = services.resource.decodeXml(
    workspace = workspace,
    request = DecodeXmlRequest(path = path),
)

internal fun McpApp.listResources(
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

internal fun McpApp.findResourceValues(
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
