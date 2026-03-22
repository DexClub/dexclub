package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.dexclub.app.compose.Loading
import io.github.shadcn.ui.compose.LocalSonner
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun WorkspaceSceneContent(
    model: WorkspaceSceneViewModel,
    onRequestExportWorkspaceLogs: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    drawerGesturesEnabled: Boolean = true,
) {
    val uiState by model.uiState.collectAsState()
    val sonnerState = LocalSonner.current

    LaunchedEffect(Unit) {
        model.initialize()
    }

    LaunchedEffect(model, sonnerState) {
        model.effects.collectLatest { effect ->
            when (effect) {
                is WorkspaceUiEffect.ShowMessage -> sonnerState.sonner(effect.message)
            }
        }
    }

    val headerCallbacks = remember(model, onRequestExportWorkspaceLogs) {
        WorkspaceHeaderCallbacks(
            onRequestExportWorkspaceLogs = onRequestExportWorkspaceLogs,
            onResetSearchDialogState = model::resetSearchDialogState,
            onSearchDialogTabSelected = model::selectSearchDialogTab,
            onSearchDialogQueryChange = model::updateSearchDialogQuery,
            onSearchDialogRequest = model::submitSearchDialog,
            onOpenClassResult = { result -> model.onOpenClassByName(result.className) },
            onOpenStringResult = model::onOpenStringSearchResult,
            onAutoUnicodeDecodeChange = model::updateAutoUnicodeDecode,
            onCodeScrollPastEndChange = model::updateCodeScrollPastEnd,
        )
    }
    val sidePanelCallbacks = remember(model) {
        WorkspaceSidePanelCallbacks(
            onNodeClick = model::onSideNodeClick,
            onScrollOffsetsChange = model::updateSidePanelScrollOffsets,
        )
    }
    val tabBarCallbacks = remember(model) {
        WorkspaceTabBarCallbacks(
            onToggleOpenTab = model::onToggleOpenTab,
            onCloseOpenTab = model::onCloseOpenTab,
            onCloseAllOpenTabs = model::closeAllOpenTabs,
            onCloseOtherOpenTabs = model::closeOtherOpenTabs,
            onCloseOpenTabsToRight = model::closeOpenTabsToRight,
            onCloseOpenTabsToLeft = model::closeOpenTabsToLeft,
            onToggleCodeView = model::toggleCodeView,
            onPrioritizeKind = model::prioritizeKind,
        )
    }
    val paneCallbacks = remember(model) {
        WorkspaceCodePaneCallbacks(
            onConsumeNavigationRevealTarget = model::consumeNavigationRevealTarget,
            onCodeViewportChanged = model::onCodeViewportChanged,
            onActivatePane = model::activatePane,
            onUpdateCursorSelection = model::updateCursorSelection,
            onUpdateScrollOffset = model::updateScrollOffset,
            onNavigateToDefinition = model::navigateToDefinition,
        )
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        when (resolveWorkspaceLayoutMode(maxWidth)) {
            WorkspaceLayoutMode.Compact -> {
                WorkspaceSceneCompact(
                    uiState = uiState,
                    headerCallbacks = headerCallbacks,
                    sidePanelCallbacks = sidePanelCallbacks,
                    tabBarCallbacks = tabBarCallbacks,
                    paneCallbacks = paneCallbacks,
                    onBackPressed = onBackPressed,
                    drawerGesturesEnabled = drawerGesturesEnabled,
                )
            }

            WorkspaceLayoutMode.Medium,
            WorkspaceLayoutMode.Expanded,
            -> {
                WorkspaceSceneExpanded(
                    uiState = uiState,
                    headerCallbacks = headerCallbacks,
                    sidePanelCallbacks = sidePanelCallbacks,
                    tabBarCallbacks = tabBarCallbacks,
                    paneCallbacks = paneCallbacks,
                    dragHandleModifier = dragHandleModifier,
                )
            }
        }
    }

    if (uiState.loading) {
        Loading(uiState.loadingMessage)
    }
}
