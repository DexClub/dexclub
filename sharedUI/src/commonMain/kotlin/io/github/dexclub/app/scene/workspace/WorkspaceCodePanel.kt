package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.ShadcnTheme

@Composable
internal fun WorkspaceCodePanel(
    uiState: WorkspaceCodePanelUiState,
    tabBarCallbacks: WorkspaceTabBarCallbacks,
    paneCallbacks: WorkspaceCodePaneCallbacks,
    layoutMode: WorkspaceLayoutMode,
    modifier: Modifier = Modifier,
) {
    if (uiState.openTabs.isEmpty()) return

    val openTabList = remember(uiState.openTabs) { uiState.openTabs.toList() }
    val pagerState = rememberPagerState(
        initialPage = maxOf(0, uiState.tabBarUiState.selectedTabIndex),
        pageCount = { openTabList.size },
    )

    LaunchedEffect(uiState.tabBarUiState.selectedTabIndex, openTabList.size) {
        val selectedIndex = uiState.tabBarUiState.selectedTabIndex
        if (selectedIndex >= 0 && pagerState.targetPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        HeaderTabBar(
            tabBarUiState = uiState.tabBarUiState,
            callbacks = tabBarCallbacks,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            color = ShadcnTheme.colors.border.copy(alpha = 0.6f),
        )
        CodeViewPager(
            codePanelUiState = uiState,
            callbacks = paneCallbacks,
            state = pagerState,
            layoutMode = layoutMode,
            modifier = Modifier.weight(1f),
        )
    }
}
