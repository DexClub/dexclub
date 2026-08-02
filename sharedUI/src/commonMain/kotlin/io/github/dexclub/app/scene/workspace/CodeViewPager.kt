package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.model.OPEN_TAB_KIND_SMALI
import io.github.dexclub.app.model.OpenTabMode
import io.github.dexclub.app.model.OpenTabUiModel

@Composable
private fun CodeViewPage(
    tab: OpenTabUiModel,
    codePanelUiState: WorkspaceCodePanelUiState,
    callbacks: WorkspaceCodePaneCallbacks,
    isSelected: Boolean,
    isContentVisible: Boolean,
    layoutMode: WorkspaceLayoutMode,
) {
    if (!isContentVisible && !isSelected) {
        Box(Modifier.fillMaxSize())
        return
    }

    fun paneKey(
        paneIndex: Int,
        kind: String,
    ): String {
        return "$paneIndex#$kind"
    }

    fun paneStateOf(kind: String): WorkspaceCodePaneUiState {
        return codePanelUiState.paneState(tab.tabId, kind)
    }

    var activePaneKey by remember(tab.tabId) {
        mutableStateOf(paneKey(tab.activePaneIndex, tab.activeKind))
    }

    LaunchedEffect(tab.activePaneIndex, tab.activeKind) {
        activePaneKey = paneKey(tab.activePaneIndex, tab.activeKind)
    }

    fun requestActivatePane(
        paneIndex: Int,
        kind: String,
    ) {
        activePaneKey = paneKey(paneIndex, kind)
        callbacks.onActivatePane(tab, paneIndex, kind)
    }

    when (tab.mode) {
        OpenTabMode.MIXED -> {
            val paneKinds = tab.panes
                .sortedBy { it.paneIndex }
                .map { it.kind }
                .ifEmpty { tab.requiredKinds }
                .distinct()

            if (layoutMode.isCompact) {
                val activePane = tab.panes.firstOrNull { pane -> pane.paneIndex == tab.activePaneIndex }
                val compactKind = activePane?.kind
                    ?: paneKinds.firstOrNull { kind -> kind == tab.activeKind }
                    ?: paneKinds.firstOrNull()
                    ?: tab.activeKind
                val compactPaneIndex = activePane?.paneIndex
                    ?: tab.panes.firstOrNull { pane -> pane.kind == compactKind }?.paneIndex
                    ?: 0

                key("${tab.tabId}#$compactKind") {
                    CodeViewPane(
                        tab = tab,
                        paneState = paneStateOf(compactKind),
                        callbacks = callbacks,
                        paneIndex = compactPaneIndex,
                        kind = compactKind,
                        isSelectedTab = isSelected,
                        isActivePane = isSelected && activePaneKey == paneKey(compactPaneIndex, compactKind),
                        onRequestActivatePane = ::requestActivatePane,
                        navigationRevealTarget = codePanelUiState.navigationRevealTarget,
                        modifier = Modifier.fillMaxSize(),
                        paddingValues = PaddingValues(end = 4.dp),
                    )
                }
                return
            }

            val leftKind = paneKinds.firstOrNull() ?: OPEN_TAB_KIND_SMALI
            val rightKind = paneKinds.getOrNull(1)
                ?: tab.requiredKinds.firstOrNull { it != leftKind }
                ?: oppositeKind(leftKind)
            val leftPaneIndex = tab.panes.firstOrNull { pane -> pane.kind == leftKind }?.paneIndex ?: 0
            val rightPaneIndex = tab.panes.firstOrNull { pane -> pane.kind == rightKind }?.paneIndex ?: 1

            Row(modifier = Modifier.fillMaxSize()) {
                key("${tab.tabId}#$leftKind") {
                    CodeViewPane(
                        tab = tab,
                        paneState = paneStateOf(leftKind),
                        callbacks = callbacks,
                        paneIndex = leftPaneIndex,
                        kind = leftKind,
                        isSelectedTab = isSelected,
                        isActivePane = isSelected && activePaneKey == paneKey(leftPaneIndex, leftKind),
                        onRequestActivatePane = ::requestActivatePane,
                        navigationRevealTarget = codePanelUiState.navigationRevealTarget,
                        modifier = Modifier.weight(1f),
                        paddingValues = PaddingValues(end = 4.dp),
                    )
                }

                key("${tab.tabId}#$rightKind") {
                    CodeViewPane(
                        tab = tab,
                        paneState = paneStateOf(rightKind),
                        callbacks = callbacks,
                        paneIndex = rightPaneIndex,
                        kind = rightKind,
                        isSelectedTab = isSelected,
                        isActivePane = isSelected && activePaneKey == paneKey(rightPaneIndex, rightKind),
                        onRequestActivatePane = ::requestActivatePane,
                        navigationRevealTarget = codePanelUiState.navigationRevealTarget,
                        modifier = Modifier.weight(1f),
                        paddingValues = PaddingValues(end = 4.dp),
                    )
                }
            }
        }

        else -> {
            val kind = tab.requiredKinds.firstOrNull() ?: tab.primaryKind
            key("${tab.tabId}#$kind") {
                CodeViewPane(
                    tab = tab,
                    paneState = paneStateOf(kind),
                    callbacks = callbacks,
                    paneIndex = 0,
                    kind = kind,
                    isSelectedTab = isSelected,
                    isActivePane = isSelected,
                    onRequestActivatePane = ::requestActivatePane,
                    navigationRevealTarget = codePanelUiState.navigationRevealTarget,
                    modifier = Modifier.fillMaxSize(),
                    paddingValues = PaddingValues(end = 4.dp),
                )
            }
        }
    }
}

@Composable
internal fun CodeViewPager(
    codePanelUiState: WorkspaceCodePanelUiState,
    callbacks: WorkspaceCodePaneCallbacks,
    state: PagerState,
    layoutMode: WorkspaceLayoutMode,
    modifier: Modifier = Modifier,
) {
    val openTabs = codePanelUiState.openTabs

    HorizontalPager(
        state = state,
        userScrollEnabled = false,
        beyondViewportPageCount = openTabs.size,
        modifier = modifier,
    ) { page ->
        val pageTab = openTabs.getOrNull(page) ?: return@HorizontalPager
        val isSelected = pageTab.tabId == codePanelUiState.selectedOpenTab?.tabId
        val isContentVisible by remember(page) {
            derivedStateOf { page == state.currentPage || page == state.targetPage }
        }
        key(pageTab.tabId) {
            CodeViewPage(
                tab = pageTab,
                codePanelUiState = codePanelUiState,
                callbacks = callbacks,
                isSelected = isSelected,
                isContentVisible = isContentVisible,
                layoutMode = layoutMode,
            )
        }
    }
}
