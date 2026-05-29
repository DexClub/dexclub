package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import io.github.dexclub.app.model.OpenTabUiModel
import io.github.shadcn.ui.compose.DropdownMenu
import io.github.shadcn.ui.compose.DropdownMenuItem
import io.github.shadcn.ui.compose.Icon
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.icons.Icons
import io.github.shadcn.ui.compose.icons.KeyboardArrowDown
import io.github.shadcn.ui.compose.icons.MoreVert

@Composable
internal fun HeaderTabBar(
    tabBarUiState: WorkspaceTabBarUiState,
    callbacks: WorkspaceTabBarCallbacks,
) {
    var actionMenuExpanded by remember { mutableStateOf(false) }
    val tabListState = rememberLazyListState()
    val tabItems = tabBarUiState.items
    val selectedDisplayIndex = tabItems.indexOfFirst { item -> item.isSelected }

    LaunchedEffect(tabBarUiState.canShowMixedViewActions) {
        if (!tabBarUiState.canShowMixedViewActions) {
            actionMenuExpanded = false
        }
    }

    LaunchedEffect(tabBarUiState.selectedTab?.tabId, selectedDisplayIndex) {
        if (selectedDisplayIndex >= 0) {
            if (!tabListState.animateSelectedItemToTrailingEdge(selectedDisplayIndex)) {
                val layoutInfo = tabListState.layoutInfo
                val visibleItemCount = layoutInfo.visibleItemsInfo.count { itemInfo ->
                    itemInfo.offset >= layoutInfo.viewportStartOffset &&
                            itemInfo.offset + itemInfo.size <= layoutInfo.viewportEndOffset
                }.coerceAtLeast(1)
                val targetIndex = (selectedDisplayIndex - visibleItemCount + 1).coerceAtLeast(0)
                tabListState.animateScrollToItem(targetIndex)
                tabListState.scrollSelectedItemToTrailingEdge(selectedDisplayIndex)
            }
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
            state = tabListState,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            items(tabItems, key = { item -> item.tab.tabId }) { item ->
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
        if (tabBarUiState.items.any { item -> !item.isSelected }) {
            OpenTabJumpMenu(
                tabBarUiState = tabBarUiState,
                onToggleOpenTab = callbacks.onToggleOpenTab,
                modifier = Modifier.padding(start = 8.dp),
            )
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

@Composable
private fun OpenTabJumpMenu(
    tabBarUiState: WorkspaceTabBarUiState,
    onToggleOpenTab: (OpenTabUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        trigger = {
            EIconButton(
                shape = CircleShape,
                contentPadding = PaddingValues(4.dp),
                indicationColor = ShadcnTheme.colors.accent,
                onClick = { expanded = true },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = ShadcnTheme.colors.primary.copy(alpha = 0.62f),
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        modifier = modifier,
    ) {
        tabBarUiState.items.filterNot { item -> item.isSelected }.forEach { item ->
            val tab = item.tab
            DropdownMenuItem(
                text = tab.title,
                textStyle = ShadcnTheme.textStyles.bodySmall,
                onClick = {
                    onToggleOpenTab(tab)
                    expanded = false
                },
            )
        }
    }
}

private suspend fun LazyListState.animateSelectedItemToTrailingEdge(selectedIndex: Int): Boolean {
    val scrollDelta = selectedItemTrailingEdgeScrollDelta(selectedIndex) ?: return false
    if (scrollDelta != 0) {
        animateScrollBy(scrollDelta.toFloat())
    }
    return true
}

private suspend fun LazyListState.scrollSelectedItemToTrailingEdge(selectedIndex: Int) {
    val scrollDelta = selectedItemTrailingEdgeScrollDelta(selectedIndex) ?: return
    if (scrollDelta != 0) {
        scrollBy(scrollDelta.toFloat())
    }
}

private fun LazyListState.selectedItemTrailingEdgeScrollDelta(selectedIndex: Int): Int? {
    val layoutInfo = layoutInfo
    val selectedItem = layoutInfo.visibleItemsInfo.firstOrNull { itemInfo -> itemInfo.index == selectedIndex }
        ?: return null
    val trailingOverflow = selectedItem.offset + selectedItem.size - layoutInfo.viewportEndOffset
    if (trailingOverflow > 0) {
        return trailingOverflow
    }

    val trailingGap = layoutInfo.viewportEndOffset - selectedItem.offset - selectedItem.size
    if (trailingGap > 0 && selectedIndex < layoutInfo.totalItemsCount - 1) {
        return -trailingGap
    }
    return 0
}
