package io.github.dexclub.core.app

import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.resource.ResourceService
import io.github.dexclub.core.api.shared.Services
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.app.dex.ExportClassArtifactUseCase
import io.github.dexclub.core.app.dex.ExportClassTextUseCase
import io.github.dexclub.core.app.dex.ExportMethodArtifactUseCase
import io.github.dexclub.core.app.dex.ExportMethodTextUseCase
import io.github.dexclub.core.app.dex.FindClassesByQueryUseCase
import io.github.dexclub.core.app.dex.FindClassesUsingStringsByQueryUseCase
import io.github.dexclub.core.app.dex.FindClassesUsingStringsUseCase
import io.github.dexclub.core.app.dex.FindFieldsByQueryUseCase
import io.github.dexclub.core.app.dex.FindMethodsByQueryUseCase
import io.github.dexclub.core.app.dex.FindMethodsUseCase
import io.github.dexclub.core.app.dex.FindMethodsUsingStringsByQueryUseCase
import io.github.dexclub.core.app.dex.FindMethodsUsingStringsUseCase
import io.github.dexclub.core.app.dex.InspectMethodUseCase
import io.github.dexclub.core.app.resource.DecodeXmlUseCase
import io.github.dexclub.core.app.resource.DumpResourceTableUseCase
import io.github.dexclub.core.app.resource.FindResourceValuesUseCase
import io.github.dexclub.core.app.resource.GetResourceValueUseCase
import io.github.dexclub.core.app.resource.InspectManifestUseCase
import io.github.dexclub.core.app.resource.ListResourcesUseCase
import io.github.dexclub.core.app.session.TargetSessionService
import io.github.dexclub.core.app.workspace.GcWorkspaceUseCase
import io.github.dexclub.core.app.workspace.InitializeWorkspaceUseCase
import io.github.dexclub.core.app.workspace.InspectWorkspaceUseCase
import io.github.dexclub.core.app.workspace.ListWorkspaceTargetsUseCase
import io.github.dexclub.core.app.workspace.LoadWorkspaceStatusUseCase
import io.github.dexclub.core.app.workspace.RefreshWorkspaceUseCase
import io.github.dexclub.core.app.workspace.SwitchWorkspaceTargetUseCase

class AppUseCases(
    services: Services,
    val sessionService: TargetSessionService = TargetSessionService(workspaceService = services.workspace),
) {
    val workspace = WorkspaceUseCases(services.workspace)

    val dex = DexUseCases(
        workspaceService = services.workspace,
        dexService = services.dex,
        sessionService = sessionService,
    )

    val resource = ResourceUseCases(
        workspaceService = services.workspace,
        resourceService = services.resource,
        sessionService = sessionService,
    )
}

class WorkspaceUseCases(
    workspaceService: WorkspaceService,
) {
    val inspectWorkspaceUseCase = InspectWorkspaceUseCase(workspaceService)

    val initializeWorkspaceUseCase = InitializeWorkspaceUseCase(workspaceService)

    val switchWorkspaceTargetUseCase = SwitchWorkspaceTargetUseCase(workspaceService)

    val loadWorkspaceStatusUseCase = LoadWorkspaceStatusUseCase(workspaceService)

    val listWorkspaceTargetsUseCase = ListWorkspaceTargetsUseCase(workspaceService)

    val gcWorkspaceUseCase = GcWorkspaceUseCase(workspaceService)

    val refreshWorkspaceUseCase = RefreshWorkspaceUseCase(workspaceService)
}

class DexUseCases(
    workspaceService: WorkspaceService,
    dexService: DexAnalysisService,
    sessionService: TargetSessionService,
) {
    val inspectMethodUseCase = InspectMethodUseCase(
        workspaceService = workspaceService,
        dexService = dexService,
        sessionService = sessionService,
    )

    val findClassesByQueryUseCase = FindClassesByQueryUseCase(
        workspaceService = workspaceService,
        dexService = dexService,
        sessionService = sessionService,
    )

    val findMethodsByQueryUseCase = FindMethodsByQueryUseCase(
        workspaceService = workspaceService,
        dexService = dexService,
        sessionService = sessionService,
    )

    val findFieldsByQueryUseCase = FindFieldsByQueryUseCase(
        workspaceService = workspaceService,
        dexService = dexService,
        sessionService = sessionService,
    )

    val findClassesUsingStringsByQueryUseCase = FindClassesUsingStringsByQueryUseCase(
        workspaceService = workspaceService,
        dexService = dexService,
        sessionService = sessionService,
    )

    val findMethodsUsingStringsByQueryUseCase = FindMethodsUsingStringsByQueryUseCase(
        workspaceService = workspaceService,
        dexService = dexService,
        sessionService = sessionService,
    )

    val findClassesUsingStringsUseCase = FindClassesUsingStringsUseCase(
        workspaceService = workspaceService,
        dexService = dexService,
        sessionService = sessionService,
    )

    val findMethodsUseCase = FindMethodsUseCase(
        workspaceService = workspaceService,
        dexService = dexService,
        sessionService = sessionService,
    )

    val findMethodsUsingStringsUseCase = FindMethodsUsingStringsUseCase(
        workspaceService = workspaceService,
        dexService = dexService,
        sessionService = sessionService,
    )

    val exportClassArtifactUseCase = ExportClassArtifactUseCase(dexService)

    val exportMethodArtifactUseCase = ExportMethodArtifactUseCase(dexService)

    val exportMethodTextUseCase = ExportMethodTextUseCase(
        workspaceService = workspaceService,
        dexService = dexService,
        sessionService = sessionService,
    )

    val exportClassTextUseCase = ExportClassTextUseCase(
        workspaceService = workspaceService,
        dexService = dexService,
        sessionService = sessionService,
    )
}

class ResourceUseCases(
    workspaceService: WorkspaceService,
    resourceService: ResourceService,
    sessionService: TargetSessionService,
) {
    val inspectManifestUseCase = InspectManifestUseCase(
        workspaceService = workspaceService,
        resourceService = resourceService,
        sessionService = sessionService,
    )

    val listResourcesUseCase = ListResourcesUseCase(
        workspaceService = workspaceService,
        resourceService = resourceService,
        sessionService = sessionService,
    )

    val getResourceValueUseCase = GetResourceValueUseCase(
        workspaceService = workspaceService,
        resourceService = resourceService,
        sessionService = sessionService,
    )

    val findResourceValuesUseCase = FindResourceValuesUseCase(
        workspaceService = workspaceService,
        resourceService = resourceService,
        sessionService = sessionService,
    )

    val dumpResourceTableUseCase = DumpResourceTableUseCase(resourceService)

    val decodeXmlUseCase = DecodeXmlUseCase(
        workspaceService = workspaceService,
        resourceService = resourceService,
        sessionService = sessionService,
    )
}
