package io.github.dexclub.app.di

import io.github.dexclub.app.navigation.WorkspaceRouteArgs
import io.github.dexclub.app.scene.home.HomeSceneViewModel
import io.github.dexclub.app.scene.workspace.toWorkspaceSceneContext
import io.github.dexclub.app.scene.workspace.WorkspaceSceneViewModel
import io.github.dexclub.core.editor.OpenTabService
import io.github.dexclub.core.workspace.WorkspaceImporter
import io.github.dexclub.core.workspace.WorkspaceInitializer
import io.github.dexclub.core.workspace.WorkspaceIndexService
import io.github.dexclub.data.classindex.RoomWorkspaceClassIndexRepository
import io.github.dexclub.data.editorsession.RoomEditorSessionRepository
import io.github.dexclub.data.settings.DefaultAppSettingsRepository
import io.github.dexclub.data.workspace.DefaultWorkspaceRepository

object SharedUiDependencies {
    fun createHomeSceneViewModel(): HomeSceneViewModel {
        val workspaceRepository = DefaultWorkspaceRepository()
        return HomeSceneViewModel(
            workspaceRepository = workspaceRepository,
            workspaceImporter = WorkspaceImporter(workspaceRepository),
        )
    }

    fun createWorkspaceSceneViewModel(routeArgs: WorkspaceRouteArgs): WorkspaceSceneViewModel {
        val workspaceContext = routeArgs.toWorkspaceSceneContext()
        val databaseDir = "${workspaceContext.absolutePath}/database"
        val classIndexRepository = RoomWorkspaceClassIndexRepository(databaseDir)
        val workspaceIndexService = WorkspaceIndexService(classIndexRepository)
        val editorSessionRepository = RoomEditorSessionRepository(databaseDir)
        return WorkspaceSceneViewModel(
            workspaceContext = workspaceContext,
            appSettingsRepository = DefaultAppSettingsRepository(),
            openTabService = OpenTabService(
                editorSessionRepository = editorSessionRepository,
                workspaceIndexService = workspaceIndexService,
            ),
            workspaceIndexService = workspaceIndexService,
            workspaceInitializer = WorkspaceInitializer(
                workspaceIndexService = workspaceIndexService,
                editorSessionRepository = editorSessionRepository,
            ),
            editorSessionRepository = editorSessionRepository,
        )
    }
}
