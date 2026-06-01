package io.github.dexclub.app.scene.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.github.dexclub.app.res.StringRes
import io.github.dexclub.compat.openDirectoryPickerCompat
import io.github.shadcn.ui.compose.Button
import io.github.shadcn.ui.compose.Icon
import io.github.shadcn.ui.compose.LocalSonner
import io.github.shadcn.ui.compose.OutlineButton
import io.github.shadcn.ui.compose.Scaffold
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.icons.Add
import io.github.shadcn.ui.compose.icons.FolderOpen
import io.github.shadcn.ui.compose.icons.Icons
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
private fun HomeActionButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text)
        }
    }

    if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier,
        ) {
            content()
        }
    } else {
        OutlineButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            content()
        }
    }
}

@Composable
private fun TopActionsPart(
    model: HomeSceneViewModel,
    onOpenWorkspacePicker: () -> Unit,
    compactLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    if (compactLayout) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier,
        ) {
            HomeActionButton(
                text = StringRes.current.createWorkspace,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Filled.Add,
                        contentDescription = StringRes.current.newWorkspace,
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = model::onShowNewWorkspaceDialog,
                primary = true,
                modifier = Modifier.fillMaxWidth(),
            )
            HomeActionButton(
                text = StringRes.current.openWorkspace,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Filled.FolderOpen,
                        contentDescription = StringRes.current.openWorkspace,
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = onOpenWorkspacePicker,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            HomeActionButton(
                text = StringRes.current.createWorkspace,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Filled.Add,
                        contentDescription = StringRes.current.newWorkspace,
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = model::onShowNewWorkspaceDialog,
                primary = true,
                modifier = Modifier.weight(1f),
            )
            HomeActionButton(
                text = StringRes.current.openWorkspace,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Filled.FolderOpen,
                        contentDescription = StringRes.current.openWorkspace,
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = onOpenWorkspacePicker,
                modifier = Modifier.weight(1f),
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            val compactLayout = maxWidth < 640.dp
            val horizontalPadding = if (compactLayout) 16.dp else 24.dp
            val contentWidth = (maxWidth - horizontalPadding * 2).coerceAtMost(640.dp)

            Column(
                verticalArrangement = if (compactLayout) Arrangement.Top else Arrangement.Center,
                horizontalAlignment = if (compactLayout) Alignment.Start else Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(contentWidth)
                    .padding(vertical = if (compactLayout) 24.dp else 32.dp)
                    .verticalScroll(rememberScrollState())
                    .align(if (compactLayout) Alignment.TopCenter else Alignment.Center),
            ) {
                AppTitle()
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
                    compactLayout = compactLayout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                )
                WorkspaceListPart(
                    uiState = uiState,
                    onEnterWorkspace = model::onEnterWorkspace,
                    onDelete = model::onShowDeleteConfirmDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                )
            }
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
