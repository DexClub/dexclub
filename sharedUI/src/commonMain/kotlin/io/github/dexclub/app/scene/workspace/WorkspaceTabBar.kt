package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.compose.EIconButton
import io.github.shadcn.ui.compose.DropdownMenu
import io.github.shadcn.ui.compose.DropdownMenuItem
import io.github.shadcn.ui.compose.Icon
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.icons.Icons
import io.github.shadcn.ui.compose.icons.MoreVert

@Composable
internal fun HeaderTabBar(
    tabBarUiState: WorkspaceTabBarUiState,
    callbacks: WorkspaceTabBarCallbacks,
) {
    var actionMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(tabBarUiState.canShowMixedViewActions) {
        if (!tabBarUiState.canShowMixedViewActions) {
            actionMenuExpanded = false
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderBarHeight)
            .padding(horizontal = ContentHorizontalPadding),
    ) {
        LazyRow(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            items(tabBarUiState.items, key = { item -> item.tab.tabId }) { item ->
                TabBarItem(
                    itemState = item,
                    onToggle = callbacks.onToggleOpenTab,
                    onClose = callbacks.onCloseOpenTab,
                    onCloseAll = callbacks.onCloseAllOpenTabs,
                    onCloseOthers = callbacks.onCloseOtherOpenTabs,
                    onCloseTabsToRight = callbacks.onCloseOpenTabsToRight,
                    onCloseTabsToLeft = callbacks.onCloseOpenTabsToLeft,
                    onToggleViewType = callbacks.onToggleCodeView,
                )
            }
        }
        if (tabBarUiState.canShowMixedViewActions) {
            DropdownMenu(
                expanded = actionMenuExpanded,
                onDismissRequest = { actionMenuExpanded = false },
                trigger = {
                    EIconButton(
                        shape = CircleShape,
                        contentPadding = PaddingValues(4.dp),
                        indicationColor = ShadcnTheme.colors.accent,
                        onClick = { actionMenuExpanded = true },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Filled.MoreVert,
                            contentDescription = null,
                            tint = ShadcnTheme.colors.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                DropdownMenuItem(
                    text = "切换视图",
                    textStyle = ShadcnTheme.textStyles.bodySmall,
                    onClick = {
                        val selectedTab = tabBarUiState.selectedTab ?: return@DropdownMenuItem
                        val nextPreferredKind = tabBarUiState.nextPreferredKind ?: return@DropdownMenuItem
                        callbacks.onPrioritizeKind(selectedTab, nextPreferredKind)
                        actionMenuExpanded = false
                    },
                )
            }
        }
    }
}
