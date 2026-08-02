package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.compose.EIconButton
import io.github.shadcn.ui.compose.DropdownMenu
import io.github.shadcn.ui.compose.DropdownMenuItem
import io.github.shadcn.ui.compose.Icon
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.icons.Icons
import io.github.shadcn.ui.compose.icons.Menu
import io.github.shadcn.ui.compose.icons.MoreVert

@Composable
internal fun SideHeaderBar(
    uiState: WorkspaceHeaderUiState,
    callbacks: WorkspaceHeaderCallbacks,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(HeaderBarHeight)
            .padding(horizontal = ContentHorizontalPadding),
    ) {
        WorkspaceProjectMenu(
            callbacks = callbacks,
        )

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
internal fun WorkspaceHeaderTitleBlock(
    uiState: WorkspaceHeaderUiState,
    modifier: Modifier = Modifier,
    showDisplayPath: Boolean = true,
) {
    Column(modifier = modifier) {
        Text(
            text = uiState.workspaceName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = ShadcnTheme.textStyles.labelMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
        if (showDisplayPath) {
            Text(
                text = uiState.displayPath,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = ShadcnTheme.textStyles.labelSmall.copy(
                    color = ShadcnTheme.colors.primary.copy(alpha = 0.6f),
                ),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
internal fun WorkspaceProjectMenu(
    callbacks: WorkspaceHeaderCallbacks,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
        trigger = {
            EIconButton(
                shape = CircleShape,
                contentPadding = PaddingValues(4.dp),
                indicationColor = ShadcnTheme.colors.accent,
                onClick = {
                    menuExpanded = true
                },
                modifier = modifier,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Filled.Menu,
                    contentDescription = "项目菜单",
                    tint = ShadcnTheme.colors.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    ) {
        DropdownMenuItem(
            text = "新建项目",
            textStyle = ShadcnTheme.textStyles.bodySmall,
            onClick = {
                callbacks.onCreateWorkspace()
                menuExpanded = false
            },
        )
        DropdownMenuItem(
            text = "打开项目",
            textStyle = ShadcnTheme.textStyles.bodySmall,
            onClick = {
                callbacks.onOpenWorkspace()
                menuExpanded = false
            },
        )
        DropdownMenuItem(
            text = "关闭当前项目",
            textStyle = ShadcnTheme.textStyles.bodySmall,
            onClick = {
                callbacks.onNavigateHome()
                menuExpanded = false
            },
        )
    }
}

@Composable
internal fun WorkspaceHeaderActionMenu(
    uiState: WorkspaceHeaderUiState,
    callbacks: WorkspaceHeaderCallbacks,
    modifier: Modifier = Modifier,
) {
    var actionMenuExpanded by remember { mutableStateOf(false) }
    var searchDialogVisible by remember { mutableStateOf(false) }
    var settingsDialogVisible by remember { mutableStateOf(false) }

    fun dismissSearchDialog() {
        callbacks.onResetSearchDialogState()
        searchDialogVisible = false
    }

    DropdownMenu(
        expanded = actionMenuExpanded,
        onDismissRequest = { actionMenuExpanded = false },
        trigger = {
            EIconButton(
                shape = CircleShape,
                contentPadding = PaddingValues(4.dp),
                indicationColor = ShadcnTheme.colors.accent,
                onClick = {
                    actionMenuExpanded = true
                },
                modifier = modifier,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Filled.MoreVert,
                    contentDescription = null,
                    tint = ShadcnTheme.colors.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    ) {
        DropdownMenuItem(
            text = "搜索",
            textStyle = ShadcnTheme.textStyles.bodySmall,
            onClick = {
                callbacks.onResetSearchDialogState()
                searchDialogVisible = true
                actionMenuExpanded = false
            },
        )
        DropdownMenuItem(
            text = "导出日志（最近7天）",
            textStyle = ShadcnTheme.textStyles.bodySmall,
            onClick = {
                callbacks.onRequestExportWorkspaceLogs()
                actionMenuExpanded = false
            },
        )
        DropdownMenuItem(
            text = "设置",
            textStyle = ShadcnTheme.textStyles.bodySmall,
            onClick = {
                settingsDialogVisible = true
                actionMenuExpanded = false
            },
        )
    }

    WorkspaceSearchDialog(
        uiState = uiState.searchDialogUiState,
        visible = searchDialogVisible,
        onDismissRequest = ::dismissSearchDialog,
        onTabSelected = callbacks.onSearchDialogTabSelected,
        onQueryChange = callbacks.onSearchDialogQueryChange,
        onSearchRequest = callbacks.onSearchDialogRequest,
        onOpenClassResult = callbacks.onOpenClassResult,
        onOpenStringResult = callbacks.onOpenStringResult,
    )

    WorkspaceSettingsDialog(
        uiState = uiState.settingsUiState,
        visible = settingsDialogVisible,
        onDismissRequest = { settingsDialogVisible = false },
        onSmaliUnicodeDecodeChange = callbacks.onSmaliUnicodeDecodeChange,
        onJavaUnicodeDecodeChange = callbacks.onJavaUnicodeDecodeChange,
        onCodeScrollPastEndChange = callbacks.onCodeScrollPastEndChange,
    )
}
