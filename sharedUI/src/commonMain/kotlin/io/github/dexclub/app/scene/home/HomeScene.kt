package io.github.dexclub.app.scene.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dexclub.sharedui.generated.resources.Res
import dexclub.sharedui.generated.resources.dexclub_icon
import io.github.dexclub.BuildConfig
import io.github.dexclub.app.di.SharedUiDependencies
import io.github.dexclub.app.model.WorkspaceSummary
import io.github.dexclub.app.navigation.WorkspaceRouteArgs
import io.github.dexclub.app.res.StringRes
import io.github.dexclub.compat.openDirectoryPickerCompat
import io.github.shadcn.ui.compose.Button
import io.github.shadcn.ui.compose.Icon
import io.github.shadcn.ui.compose.LocalSonner
import io.github.shadcn.ui.compose.OutlineButton
import io.github.shadcn.ui.compose.Scaffold
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.foundation.rememberShadcnIndication
import io.github.shadcn.ui.compose.icons.Add
import io.github.shadcn.ui.compose.icons.FolderOpen
import io.github.shadcn.ui.compose.icons.Icons
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
private fun HomeBrandTitle(
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ShadcnTheme.colors.primary.copy(alpha = 0.08f)),
        ) {
            Image(
                painter = painterResource(Res.drawable.dexclub_icon),
                contentDescription = StringRes.current.appName,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = StringRes.current.appName,
                style = ShadcnTheme.textStyles.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${BuildConfig.GIT_COMMIT_COUNT} commits / ${BuildConfig.GIT_COMMIT_HASH}",
                style = ShadcnTheme.textStyles.labelMedium.copy(
                    color = ShadcnTheme.colors.primary.copy(alpha = 0.58f),
                ),
            )
        }
    }
}

@Composable
private fun HomePrimaryActionButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text)
        }
    }
}

@Composable
private fun HomeNavItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val backgroundColor = if (selected) {
        ShadcnTheme.colors.primary.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }
    val borderColor = if (selected) {
        ShadcnTheme.colors.primary.copy(alpha = 0.22f)
    } else {
        Color.Transparent
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(backgroundColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberShadcnIndication(),
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (selected) {
                        ShadcnTheme.colors.primary
                    } else {
                        ShadcnTheme.colors.primary.copy(alpha = 0.14f)
                    },
                ),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = ShadcnTheme.textStyles.bodyMedium.copy(
                color = if (selected) {
                    ShadcnTheme.colors.primary
                } else {
                    ShadcnTheme.colors.primary.copy(alpha = 0.68f)
                },
            ),
        )
    }
}

@Composable
private fun HomeSidebar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(ShadcnTheme.colors.muted.copy(alpha = 0.34f))
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        HomeBrandTitle()
        Spacer(modifier = Modifier.height(28.dp))
        HomeNavItem(
            text = StringRes.current.projectsTab,
            selected = selectedTab == HomeTab.Projects,
            onClick = { onTabSelected(HomeTab.Projects) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        HomeNavItem(
            text = StringRes.current.customizeTab,
            selected = selectedTab == HomeTab.Customize,
            onClick = { onTabSelected(HomeTab.Customize) },
        )
    }
}

@Composable
private fun ProjectsPane(
    uiState: HomeUiState,
    onCreateWorkspace: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onEnterWorkspace: (WorkspaceSummary) -> Unit,
    onDeleteWorkspace: (WorkspaceSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            HomePrimaryActionButton(
                text = StringRes.current.createWorkspace,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Filled.Add,
                        contentDescription = StringRes.current.createWorkspace,
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = onCreateWorkspace,
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlineButton(
                onClick = onOpenWorkspace,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Filled.FolderOpen,
                        contentDescription = StringRes.current.openWorkspace,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = StringRes.current.openWorkspace)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.workspaceItems.isEmpty()) {
            Text(
                text = StringRes.current.noProjects,
                style = ShadcnTheme.textStyles.bodyMedium.copy(
                    color = ShadcnTheme.colors.primary.copy(alpha = 0.56f),
                ),
            )
        } else {
            WorkspaceListPart(
                uiState = uiState,
                onEnterWorkspace = onEnterWorkspace,
                onDelete = onDeleteWorkspace,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CustomizePane(
    uiState: HomeUiState,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier.padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    color = ShadcnTheme.colors.muted.copy(alpha = 0.4f),
                    shape = shape,
                )
                .border(
                    width = 1.dp,
                    color = ShadcnTheme.colors.border.copy(alpha = 0.7f),
                    shape = shape,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberShadcnIndication(),
                    onClick = onChooseFolder,
                )
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Text(
                text = "项目路径",
                style = ShadcnTheme.textStyles.labelLarge.copy(
                    color = ShadcnTheme.colors.primary.copy(alpha = 0.72f),
                ),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = uiState.projectCacheDir.ifBlank { uiState.defaultProjectCacheDir },
                style = ShadcnTheme.textStyles.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Rounded.Filled.FolderOpen,
                contentDescription = StringRes.current.chooseFolder,
                tint = ShadcnTheme.colors.primary.copy(alpha = 0.62f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun HomeContentPane(
    uiState: HomeUiState,
    onCreateWorkspace: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onChooseProjectCacheDir: () -> Unit,
    onEnterWorkspace: (WorkspaceSummary) -> Unit,
    onDeleteWorkspace: (WorkspaceSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState.selectedTab) {
        HomeTab.Projects -> ProjectsPane(
            uiState = uiState,
            onCreateWorkspace = onCreateWorkspace,
            onOpenWorkspace = onOpenWorkspace,
            onEnterWorkspace = onEnterWorkspace,
            onDeleteWorkspace = onDeleteWorkspace,
            modifier = modifier,
        )

        HomeTab.Customize -> CustomizePane(
            uiState = uiState,
            onChooseFolder = onChooseProjectCacheDir,
            modifier = modifier,
        )
    }
}

@Composable
private fun rememberOpenWorkspaceLauncher(
    onDirectoryPicked: (PlatformFile?) -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    return remember(scope, onDirectoryPicked) {
        {
            PickerResultLauncher {
                scope.launch {
                    val file = FileKit.openDirectoryPickerCompat(
                        title = StringRes.current.openWorkspace,
                        dialogSettings = FileKitDialogSettings.createDefault(),
                    )
                    onDirectoryPicked(file)
                }
            }.launch()
        }
    }
}

@Composable
private fun rememberProjectCacheDirLauncher(
    onDirectoryPicked: (PlatformFile?) -> Unit,
): (String) -> Unit {
    val scope = rememberCoroutineScope()
    return remember(scope, onDirectoryPicked) {
        { path ->
            PickerResultLauncher {
                scope.launch {
                    val file = FileKit.openDirectoryPickerCompat(
                        title = StringRes.current.projectCachePath,
                        directory = PlatformFile(path),
                        dialogSettings = FileKitDialogSettings.createDefault(),
                    )
                    onDirectoryPicked(file)
                }
            }.launch()
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
    val openWorkspaceLauncher = rememberOpenWorkspaceLauncher(model::onOpen)
    val chooseProjectCacheDirLauncher = rememberProjectCacheDirLauncher(model::applyProjectCacheDir)

    LaunchedEffect(model, sonnerState, chooseProjectCacheDirLauncher) {
        model.effects.collectLatest { effect ->
            when (effect) {
                is HomeUiEffect.EnterWorkspace -> onEnterWorkspace(effect.routeArgs)
                is HomeUiEffect.ShowMessage -> sonnerState.sonner(effect.message)
                is HomeUiEffect.ChooseProjectCacheDir -> chooseProjectCacheDirLauncher(effect.initialPath)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ShadcnTheme.colors.background),
        ) {
            HomeSidebar(
                selectedTab = uiState.selectedTab,
                onTabSelected = model::selectTab,
                modifier = Modifier.width(HomeSidebarWidth),
            )
            HomeContentPane(
                uiState = uiState,
                onCreateWorkspace = model::onShowNewWorkspaceDialog,
                onOpenWorkspace = openWorkspaceLauncher,
                onChooseProjectCacheDir = model::chooseProjectCacheDir,
                onEnterWorkspace = model::onEnterWorkspace,
                onDeleteWorkspace = model::onShowDeleteConfirmDialog,
                modifier = Modifier.weight(1f),
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

private val HomeSidebarWidth: Dp = 220.dp
