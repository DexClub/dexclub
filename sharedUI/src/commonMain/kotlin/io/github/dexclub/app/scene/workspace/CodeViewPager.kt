package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
) {
    if (!isContentVisible && !isSelected) {
        Box(Modifier.fillMaxSize())
        return
    }

    fun paneStateOf(kind: String): WorkspaceCodePaneUiState {
        return codePanelUiState.paneState(tab.tabId, kind)
    }

    when (tab.mode) {
        OpenTabMode.MIXED -> {
            val paneKinds = tab.panes
                .sortedBy { it.paneIndex }
                .map { it.kind }
                .ifEmpty { tab.requiredKinds }
                .distinct()

            val leftKind = paneKinds.firstOrNull() ?: OPEN_TAB_KIND_SMALI
            val rightKind = paneKinds.getOrNull(1)
                ?: tab.requiredKinds.firstOrNull { it != leftKind }
                ?: oppositeKind(leftKind)

            Row(modifier = Modifier.fillMaxSize()) {
                key("${tab.tabId}#$leftKind") {
                    CodeViewPane(
                        tab = tab,
                        paneState = paneStateOf(leftKind),
                        callbacks = callbacks,
                        paneIndex = 0,
                        kind = leftKind,
                        isSelectedTab = isSelected,
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
                        paneIndex = 1,
                        kind = rightKind,
                        isSelectedTab = isSelected,
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
            )
        }
    }
}
