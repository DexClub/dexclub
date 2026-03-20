package io.github.dexclub.app.scene.workspace

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.dexclub.app.di.SharedUiDependencies
import io.github.dexclub.app.navigation.WorkspaceRouteArgs

val HeaderBarHeight = 48.dp
val ContentHorizontalPadding = 12.dp

@Composable
expect fun WorkspaceScene(
    onBackPressed: () -> Unit,
    routeArgs: WorkspaceRouteArgs,
    model: WorkspaceSceneViewModel = viewModel { SharedUiDependencies.createWorkspaceSceneViewModel(routeArgs) },
)

