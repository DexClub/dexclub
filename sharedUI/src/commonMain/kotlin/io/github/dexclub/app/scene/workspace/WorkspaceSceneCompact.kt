package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.compose.EIconButton
import io.github.dexclub.app.res.IconRes
import io.github.dexclub.app.res.icons.PackageFolder
import io.github.dexclub.node.ClassTreeNode
import io.github.shadcn.ui.compose.Card
import io.github.shadcn.ui.compose.Icon
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.icons.ArrowBack
import io.github.shadcn.ui.compose.icons.Icons
import kotlinx.coroutines.launch

@Composable
internal fun WorkspaceSceneCompact(
    uiState: WorkspaceUiState,
    headerCallbacks: WorkspaceHeaderCallbacks,
    sidePanelCallbacks: WorkspaceSidePanelCallbacks,
    tabBarCallbacks: WorkspaceTabBarCallbacks,
    paneCallbacks: WorkspaceCodePaneCallbacks,
    onBackPressed: () -> Unit,
    drawerGesturesEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val compactSidePanelCallbacks = WorkspaceSidePanelCallbacks(
        onNodeClick = { node ->
            sidePanelCallbacks.onNodeClick(node)
            if (node is ClassTreeNode.ClassNode) {
                scope.launch {
                    drawerState.close()
                }
            }
        },
        onScrollOffsetsChange = sidePanelCallbacks.onScrollOffsetsChange,
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerGesturesEnabled,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight(),
            ) {
                WorkspaceCompactDrawerContent(
                    headerUiState = uiState.headerUiState,
                    sidePanelUiState = uiState.sidePanelUiState,
                    sidePanelCallbacks = compactSidePanelCallbacks,
                )
            }
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ShadcnTheme.colors.background),
        ) {
            WorkspaceCompactTopBar(
                uiState = uiState.headerUiState,
                callbacks = headerCallbacks,
                onBackPressed = onBackPressed,
                onOpenDrawer = {
                    scope.launch {
                        drawerState.open()
                    }
                },
            )
            HorizontalDivider(
                color = ShadcnTheme.colors.border.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = ContentHorizontalPadding),
            )
            Card(
                contentPadding = PaddingValues.Zero,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                WorkspaceCodePanel(
                    uiState = uiState.codePanelUiState,
                    tabBarCallbacks = tabBarCallbacks,
                    paneCallbacks = paneCallbacks,
                    layoutMode = WorkspaceLayoutMode.Compact,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun WorkspaceCompactTopBar(
    uiState: WorkspaceHeaderUiState,
    callbacks: WorkspaceHeaderCallbacks,
    onBackPressed: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(HeaderBarHeight)
            .padding(horizontal = ContentHorizontalPadding),
    ) {
        EIconButton(
            contentPadding = PaddingValues(4.dp),
            onClick = onBackPressed,
        ) {
            Icon(
                imageVector = Icons.Rounded.Filled.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier.size(18.dp),
            )
        }

        EIconButton(
            contentPadding = PaddingValues(4.dp),
            onClick = onOpenDrawer,
            modifier = Modifier.padding(start = 4.dp),
        ) {
            Icon(
                imageVector = IconRes.PackageFolder,
                contentDescription = "类树",
                tint = null,
                modifier = Modifier.size(18.dp),
            )
        }

        WorkspaceHeaderTitleBlock(
            uiState = uiState,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )

        WorkspaceHeaderActionMenu(
            uiState = uiState,
            callbacks = callbacks,
        )
    }
}

@Composable
private fun WorkspaceCompactDrawerContent(
    headerUiState: WorkspaceHeaderUiState,
    sidePanelUiState: WorkspaceSidePanelUiState,
    sidePanelCallbacks: WorkspaceSidePanelCallbacks,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 420.dp),
    ) {
        WorkspaceHeaderTitleBlock(
            uiState = headerUiState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
        )
        HorizontalDivider(
            color = ShadcnTheme.colors.border.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = ContentHorizontalPadding),
        )
        SideLazyColumn(
            uiState = sidePanelUiState,
            callbacks = sidePanelCallbacks,
            showScrollbars = false,
            modifier = Modifier.weight(1f),
        )
    }
}
