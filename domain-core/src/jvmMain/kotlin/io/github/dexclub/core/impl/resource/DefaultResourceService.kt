package io.github.dexclub.core.impl.resource

import io.github.dexclub.core.api.resource.DecodeXmlRequest
import io.github.dexclub.core.api.resource.DecodedXmlResult
import io.github.dexclub.core.api.resource.FindResourcesRequest
import io.github.dexclub.core.api.resource.InspectManifestRequest
import io.github.dexclub.core.api.resource.ManifestResult
import io.github.dexclub.core.api.resource.ManifestInspectionResult
import io.github.dexclub.core.api.resource.ResourceEntry
import io.github.dexclub.core.api.resource.ResourceEntryValueHit
import io.github.dexclub.core.api.resource.ResourceService
import io.github.dexclub.core.api.resource.ResourceTableResult
import io.github.dexclub.core.api.resource.ResourceValue
import io.github.dexclub.core.api.resource.ResolveResourceRequest
import io.github.dexclub.core.api.shared.Operation
import io.github.dexclub.core.api.shared.applyPageWindow
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceResolveError
import io.github.dexclub.core.api.workspace.WorkspaceResolveErrorReason
import io.github.dexclub.core.impl.dex.CapabilityChecker
import io.github.dexclub.core.impl.workspace.store.WorkspaceStore

internal class DefaultResourceService(
    private val store: WorkspaceStore,
    private val capabilityChecker: CapabilityChecker,
    private val manifestExecutor: ManifestExecutor,
    private val resourceTableExecutor: ResourceTableExecutor,
    private val xmlExecutor: XmlExecutor,
    private val resourceEntryListExecutor: ResourceEntryListExecutor,
    private val resourceValueExecutor: ResourceValueExecutor,
    private val manifestInspectionParser: ManifestInspectionParser = ManifestInspectionParser(),
) : ResourceService {
    override fun decodeManifest(workspace: WorkspaceContext): ManifestResult {
        capabilityChecker.require(workspace, Operation.ManifestDecode)
        val snapshot = requireActiveSnapshot(workspace)
        return manifestExecutor.decodeManifest(
            workspace = workspace,
            inventory = snapshot.inventory,
        )
    }

    override fun inspectManifest(
        workspace: WorkspaceContext,
        request: InspectManifestRequest,
    ): ManifestInspectionResult {
        val manifest = decodeManifest(workspace)
        return manifestInspectionParser.parse(manifest, request)
    }

    override fun dumpResourceTable(workspace: WorkspaceContext): ResourceTableResult {
        capabilityChecker.require(workspace, Operation.ResourceTableDecode)
        val snapshot = requireActiveSnapshot(workspace)
        return resourceTableExecutor.dumpResourceTable(
            workspace = workspace,
            inventory = snapshot.inventory,
        )
    }

    override fun decodeXml(workspace: WorkspaceContext, request: DecodeXmlRequest): DecodedXmlResult {
        capabilityChecker.require(workspace, Operation.XmlDecode)
        val snapshot = requireActiveSnapshot(workspace)
        return xmlExecutor.decodeXml(
            workspace = workspace,
            inventory = snapshot.inventory,
            request = request,
        )
    }

    override fun listResourceEntries(workspace: WorkspaceContext): List<ResourceEntry> {
        capabilityChecker.require(workspace, Operation.ResourceEntryList)
        val snapshot = requireActiveSnapshot(workspace)
        return resourceEntryListExecutor.listResourceEntries(
            workspace = workspace,
            inventory = snapshot.inventory,
        )
    }

    override fun getResourceValue(
        workspace: WorkspaceContext,
        request: ResolveResourceRequest,
    ): ResourceValue {
        capabilityChecker.require(workspace, Operation.ResourceTableDecode)
        val snapshot = requireActiveSnapshot(workspace)
        return resourceValueExecutor.getResourceValue(
            workspace = workspace,
            inventory = snapshot.inventory,
            request = request,
        )
    }

    override fun findResourceValues(
        workspace: WorkspaceContext,
        request: FindResourcesRequest,
    ): List<ResourceEntryValueHit> {
        capabilityChecker.require(workspace, Operation.ResourceTableDecode)
        val snapshot = requireActiveSnapshot(workspace)
        val hits = resourceValueExecutor.findResourceValues(
            workspace = workspace,
            inventory = snapshot.inventory,
            request = request,
        )
        return hits
            .sortedWith(
                compareBy<ResourceEntryValueHit>(
                    { it.type.orEmpty() },
                    { it.name.orEmpty() },
                    { it.value.orEmpty() },
                    { it.resourceId.orEmpty() },
                    { it.sourcePath.orEmpty() },
                    { it.sourceEntry.orEmpty() },
                ),
            )
            .applyPageWindow(request.window)
    }

    private fun requireActiveSnapshot(workspace: WorkspaceContext) =
        store.loadSnapshot(workspace.workdir, workspace.activeTargetId)
            ?: throw WorkspaceResolveError(
                reason = WorkspaceResolveErrorReason.InvalidSnapshot,
                workdir = workspace.workdir,
                message = "Active target snapshot is missing: ${workspace.activeTargetId}",
            )
}
