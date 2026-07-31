package io.github.dexclub.mcp

import io.github.dexclub.core.app.contract.ManifestComponentType
import io.github.dexclub.core.app.contract.ManifestInspectionSection
import io.github.dexclub.core.app.contract.ResourceResolution
import io.github.dexclub.core.app.contract.WorkspaceContext
import io.github.dexclub.core.app.resource.DecodeXmlUseCaseResult
import io.github.dexclub.core.app.resource.FindResourceValuesUseCaseResult
import io.github.dexclub.core.app.resource.GetResourceValueUseCaseResult
import io.github.dexclub.core.app.resource.InspectManifestUseCaseResult
import io.github.dexclub.core.app.resource.ListResourcesUseCaseResult

internal fun McpApp.inspectManifest(
    workspace: WorkspaceContext,
    includes: Set<ManifestInspectionSection>,
    includeText: Boolean = false,
    componentName: String? = null,
    componentType: ManifestComponentType? = null,
) = appUseCases.resource.inspectManifestUseCase.execute(
    io.github.dexclub.core.app.resource.InspectManifestUseCaseRequest(
        workspace = workspace,
        includes = includes,
        includeText = includeText,
        componentName = componentName,
        componentType = componentType,
    ),
).manifest

internal fun McpApp.inspectManifestExecution(
    workspace: WorkspaceContext,
    includes: Set<ManifestInspectionSection>,
    includeText: Boolean = false,
    componentName: String? = null,
    componentType: ManifestComponentType? = null,
): InspectManifestUseCaseResult =
    appUseCases.resource.inspectManifestUseCase.execute(
        io.github.dexclub.core.app.resource.InspectManifestUseCaseRequest(
            workspace = workspace,
            includes = includes,
            includeText = includeText,
            componentName = componentName,
            componentType = componentType,
        ),
    )

internal fun McpApp.getResourceValue(
    workspace: WorkspaceContext,
    resourceId: String? = null,
    packageName: String? = null,
    type: String? = null,
    name: String? = null,
    qualifier: String? = null,
    includeAllVariants: Boolean = false,
) = appUseCases.resource.getResourceValueUseCase.execute(
    io.github.dexclub.core.app.resource.GetResourceValueUseCaseRequest(
        workspace = workspace,
        resourceId = resourceId,
        packageName = packageName,
        type = type,
        name = name,
        qualifier = qualifier,
        includeAllVariants = includeAllVariants,
    ),
).resource

internal fun McpApp.getResourceValueExecution(
    workspace: WorkspaceContext,
    resourceId: String? = null,
    packageName: String? = null,
    type: String? = null,
    name: String? = null,
    qualifier: String? = null,
    includeAllVariants: Boolean = false,
): GetResourceValueUseCaseResult =
    appUseCases.resource.getResourceValueUseCase.execute(
        io.github.dexclub.core.app.resource.GetResourceValueUseCaseRequest(
            workspace = workspace,
            resourceId = resourceId,
            packageName = packageName,
            type = type,
            name = name,
            qualifier = qualifier,
            includeAllVariants = includeAllVariants,
        ),
    )

internal fun McpApp.decodeXml(
    workspace: WorkspaceContext,
    path: String,
) = appUseCases.resource.decodeXmlUseCase.execute(
    io.github.dexclub.core.app.resource.DecodeXmlUseCaseRequest(
        workspace = workspace,
        path = path,
    ),
).xml

internal fun McpApp.decodeXmlExecution(
    workspace: WorkspaceContext,
    path: String,
): DecodeXmlUseCaseResult =
    appUseCases.resource.decodeXmlUseCase.execute(
        io.github.dexclub.core.app.resource.DecodeXmlUseCaseRequest(
            workspace = workspace,
            path = path,
        ),
    )

internal fun McpApp.listResources(
    workspace: WorkspaceContext,
    resourceId: String? = null,
    packageName: String? = null,
    type: String? = null,
    name: String? = null,
    filePath: String? = null,
    resolution: ResourceResolution? = null,
    offset: Int? = null,
    limit: Int? = null,
) =
    appUseCases.resource.listResourcesUseCase.execute(
        io.github.dexclub.core.app.resource.ListResourcesUseCaseRequest(
            workspace = workspace,
            resourceId = resourceId,
            packageName = packageName,
            type = type,
            name = name,
            filePath = filePath,
            resolution = resolution,
            offset = offset,
            limit = limit,
        ),
    )

internal fun McpApp.listResourcesExecution(
    workspace: WorkspaceContext,
    resourceId: String? = null,
    packageName: String? = null,
    type: String? = null,
    name: String? = null,
    filePath: String? = null,
    resolution: ResourceResolution? = null,
    offset: Int? = null,
    limit: Int? = null,
): ListResourcesUseCaseResult =
    appUseCases.resource.listResourcesUseCase.execute(
        io.github.dexclub.core.app.resource.ListResourcesUseCaseRequest(
            workspace = workspace,
            resourceId = resourceId,
            packageName = packageName,
            type = type,
            name = name,
            filePath = filePath,
            resolution = resolution,
            offset = offset,
            limit = limit,
        ),
    )

internal fun McpApp.findResourceValues(
    workspace: WorkspaceContext,
    type: String,
    value: String,
    contains: Boolean = false,
    ignoreCase: Boolean = false,
    packageName: String? = null,
    qualifier: String? = null,
    valueKind: String? = null,
    matchTarget: String? = null,
    offset: Int? = null,
    limit: Int? = null,
) =
    appUseCases.resource.findResourceValuesUseCase.execute(
        io.github.dexclub.core.app.resource.FindResourceValuesUseCaseRequest(
            workspace = workspace,
            type = type.trim(),
            value = value,
            contains = contains,
            ignoreCase = ignoreCase,
            packageName = packageName,
            qualifier = qualifier,
            valueKind = valueKind,
            matchTarget = matchTarget,
            offset = offset,
            limit = limit,
        ),
    )

internal fun McpApp.findResourceValuesExecution(
    workspace: WorkspaceContext,
    type: String,
    value: String,
    contains: Boolean = false,
    ignoreCase: Boolean = false,
    packageName: String? = null,
    qualifier: String? = null,
    valueKind: String? = null,
    matchTarget: String? = null,
    offset: Int? = null,
    limit: Int? = null,
): FindResourceValuesUseCaseResult =
    appUseCases.resource.findResourceValuesUseCase.execute(
        io.github.dexclub.core.app.resource.FindResourceValuesUseCaseRequest(
            workspace = workspace,
            type = type.trim(),
            value = value,
            contains = contains,
            ignoreCase = ignoreCase,
            packageName = packageName,
            qualifier = qualifier,
            valueKind = valueKind,
            matchTarget = matchTarget,
            offset = offset,
            limit = limit,
        ),
    )

