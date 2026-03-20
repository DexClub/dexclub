package io.github.dexclub.app.scene.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.dexclub.app.di.SharedUiDependencies
import io.github.dexclub.app.navigation.WorkspaceRouteArgs
import io.github.dexclub.app.compose.EIconButton
import io.github.dexclub.app.res.StringRes
import io.github.dexclub.compat.openDirectoryPickerCompat
import io.github.shadcn.ui.compose.Icon
import io.github.shadcn.ui.compose.LocalSonner
import io.github.shadcn.ui.compose.Scaffold
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.icons.Add
import io.github.shadcn.ui.compose.icons.FolderOpen
import io.github.shadcn.ui.compose.icons.Icons
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

@Composable
private fun AppTitle(
    modifier: Modifier = Modifier,
) {
    Text(
        text = StringRes.current.appName,
        style = ShadcnTheme.textStyles.headlineMedium,
        modifier = modifier,
    )
}

@Composable
private fun VersionSummary(
    modifier: Modifier = Modifier,
) {
    Text(
        text = StringRes.current.appName,
        style = ShadcnTheme.textStyles.labelMedium.copy(
            color = ShadcnTheme.textStyles.labelMedium.color.copy(alpha = 0.4f),
        ),
        modifier = modifier,
    )
}

@Composable
private fun TopActionsPart(
    model: HomeSceneViewModel,
    onOpenWorkspacePicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = 12.dp),
    ) {
        EIconButton(
            onClick = {
                model.onShowNewWorkspaceDialog()
            },
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Filled.Add,
                contentDescription = StringRes.current.newWorkspace,
            )
        }

        EIconButton(
            onClick = {
                onOpenWorkspacePicker()
            },
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Filled.FolderOpen,
                contentDescription = StringRes.current.openWorkspace,
            )
        }
    }
}

@Composable
fun HomeScreen(
    model: HomeSceneViewModel = viewModel { SharedUiDependencies.createHomeSceneViewModel() },
    onEnterWorkspace: (WorkspaceRouteArgs) -> Unit,
) {
    val uiState by model.uiState.collectAsState()
    val sonnerState = LocalSonner.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(model, sonnerState) {
        model.effects.collectLatest { effect ->
            when (effect) {
                is HomeUiEffect.EnterWorkspace -> onEnterWorkspace(effect.routeArgs)
                is HomeUiEffect.ShowMessage -> sonnerState.sonner(effect.message)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(paddingValues)
                .padding(vertical = 32.dp)
                .fillMaxSize(),
        ) {
            AppTitle()
            // VersionSummary(modifier = Modifier.padding(top = 4.dp))
            TopActionsPart(
                model = model,
                onOpenWorkspacePicker = {
                    PickerResultLauncher {
                        scope.launch {
                            val file = FileKit.openDirectoryPickerCompat(
                                title = StringRes.current.openWorkspace,
                                dialogSettings = FileKitDialogSettings.createDefault(),
                            )
                            model.onOpen(file)
                        }
                    }.launch()
                },
            )
            WorkspaceListPart(
                uiState = uiState,
                onEnterWorkspace = onEnterWorkspace,
                onDelete = model::onShowDeleteConfirmDialog,
            )
        }
    }

    NewWorkspaceDialog(
        model = model,
        uiState = uiState,
    )

    DeleteConfirmDialog(
        model = model,
        uiState = uiState,
    )
}

