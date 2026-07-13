package io.github.dexclub.cli

import io.github.dexclub.core.app.contract.ManifestInspectionSection
import io.github.dexclub.core.app.contract.ManifestResult
import io.github.dexclub.core.app.AppUseCases
import io.github.dexclub.core.app.projection.toProjection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class ResourceCommandAdapter(
    private val queryTextLoader: QueryTextLoader,
    private val targetWorkspaceRuntime: CliTargetWorkspaceRuntime,
    private val appUseCases: AppUseCases,
) {
    private val json = Json

    fun manifest(request: CliRequest.Manifest): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.resource.inspectManifestUseCase.execute(
            io.github.dexclub.core.app.resource.InspectManifestUseCaseRequest(
                workspace = workspace,
                includes = ManifestInspectionSection.entries.toSet(),
                includeText = true,
            ),
        )
        return CommandResult(
            payload = RenderPayload.Manifest(
                TextArtifactView.from(
                    ManifestResult(
                        sourcePath = result.manifest.sourcePath,
                        sourceEntry = result.manifest.sourceEntry,
                        text = result.manifest.text.orEmpty(),
                    ),
                ),
            ),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun resTable(request: CliRequest.ResTable): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.resource.dumpResourceTableUseCase.execute(
            io.github.dexclub.core.app.resource.DumpResourceTableUseCaseRequest(workspace),
        )
        return CommandResult(
            payload = RenderPayload.ResourceTable(ResourceTableView.from(result.result)),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun decodeXml(request: CliRequest.DecodeXml): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.resource.decodeXmlUseCase.execute(
            io.github.dexclub.core.app.resource.DecodeXmlUseCaseRequest(
                workspace = workspace,
                path = request.path,
            ),
        )
        return CommandResult(
            payload = RenderPayload.DecodedXml(TextArtifactView.from(result.xml.toProjection())),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun listRes(request: CliRequest.ListRes): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.resource.listResourcesUseCase.execute(
            io.github.dexclub.core.app.resource.ListResourcesUseCaseRequest(
                workspace = workspace,
            ),
        )
        return CommandResult(
            payload = RenderPayload.ResourceEntries(result.items.map(ResourceEntryView::from)),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun getResValue(request: CliRequest.GetResValue): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val result = appUseCases.resource.getResourceValueUseCase.execute(
            io.github.dexclub.core.app.resource.GetResourceValueUseCaseRequest(
                workspace = workspace,
                resourceId = request.resourceId,
                type = request.type,
                name = request.name,
            ),
        )
        return CommandResult(
            payload = RenderPayload.ResourceValue(ResourceValueView.from(result.resource.toProjection())),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    fun findResValues(request: CliRequest.FindResValues): CommandResult {
        val workspace = targetWorkspaceRuntime.openWorkspace(request.workdir)
        val queryText = queryTextLoader.load(request.query, CliUsages.findResValues)
        val parsedQuery = parseResourceSearchQuery(queryText)
        val result = appUseCases.resource.findResourceValuesUseCase.execute(
            io.github.dexclub.core.app.resource.FindResourceValuesUseCaseRequest(
                workspace = workspace,
                type = parsedQuery.type,
                value = parsedQuery.value,
                contains = parsedQuery.contains,
                ignoreCase = parsedQuery.ignoreCase,
                offset = request.window.offset,
                limit = request.window.limit,
            ),
        )
        return CommandResult(
            payload = RenderPayload.ResourceValueHits(result.items.map { ResourceEntryValueHitView.from(it.toProjection()) }),
            outputFormat = request.outputFormat,
            exitCode = 0,
        )
    }

    private fun parseResourceSearchQuery(queryText: String): ResourceSearchQuery {
        val root = json.parseToJsonElement(queryText).jsonObject
        val type = root["type"]?.jsonPrimitive?.content?.trim().orEmpty()
        val value = root["value"]?.jsonPrimitive?.content?.trim().orEmpty()
        if (type.isEmpty() || value.isEmpty()) {
            throw CliUsageError(
                message = "query JSON must include non-empty type and value",
                usage = CliUsages.findResValues,
            )
        }
        return ResourceSearchQuery(
            type = type,
            value = value,
            contains = root["contains"]?.jsonPrimitive?.booleanOrNull ?: false,
            ignoreCase = root["ignoreCase"]?.jsonPrimitive?.booleanOrNull ?: false,
        )
    }
}

private data class ResourceSearchQuery(
    val type: String,
    val value: String,
    val contains: Boolean,
    val ignoreCase: Boolean,
)

