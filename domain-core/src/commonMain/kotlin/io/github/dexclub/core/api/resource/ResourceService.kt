package io.github.dexclub.core.api.resource

import io.github.dexclub.core.api.workspace.WorkspaceContext

interface ResourceService {
    fun decodeManifest(workspace: WorkspaceContext): ManifestResult

    fun inspectManifest(
        workspace: WorkspaceContext,
        request: InspectManifestRequest = InspectManifestRequest(),
    ): ManifestInspectionResult

    fun dumpResourceTable(workspace: WorkspaceContext): ResourceTableResult

    fun decodeXml(workspace: WorkspaceContext, request: DecodeXmlRequest): DecodedXmlResult

    fun listResourceEntries(workspace: WorkspaceContext): List<ResourceEntry>

    fun getResourceValue(workspace: WorkspaceContext, request: ResolveResourceRequest): ResourceValue

    fun findResourceValues(
        workspace: WorkspaceContext,
        request: FindResourcesRequest,
    ): List<ResourceEntryValueHit>
}
