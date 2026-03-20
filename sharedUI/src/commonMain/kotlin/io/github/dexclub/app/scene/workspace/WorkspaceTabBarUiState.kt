package io.github.dexclub.app.scene.workspace

import io.github.dexclub.app.model.OPEN_TAB_KIND_JAVA
import io.github.dexclub.app.model.OPEN_TAB_KIND_SMALI
import io.github.dexclub.app.model.OpenTabMode
import io.github.dexclub.app.model.OpenTabUiModel

data class WorkspaceTabBarItemUiState(
    val tab: OpenTabUiModel,
    val isSelected: Boolean = false,
    val hasOtherTabs: Boolean = false,
    val canCloseLeftTabs: Boolean = false,
    val canCloseRightTabs: Boolean = false,
)

data class WorkspaceTabBarUiState(
    val items: List<WorkspaceTabBarItemUiState> = emptyList(),
    val selectedTab: OpenTabUiModel? = null,
    val selectedTabIndex: Int = -1,
    val canShowMixedViewActions: Boolean = false,
    val nextPreferredKind: String? = null,
)

internal fun buildWorkspaceTabBarUiState(
    openTabs: List<OpenTabUiModel>,
    selectedOpenTab: OpenTabUiModel?,
): WorkspaceTabBarUiState {
    val selectedTabId = selectedOpenTab?.tabId
    val selectedTabIndex = openTabs.indexOfFirst { it.tabId == selectedTabId }
        .takeIf { it >= 0 }
        ?: openTabs.lastIndex
    val selectedTab = selectedOpenTab ?: openTabs.getOrNull(selectedTabIndex)

    return WorkspaceTabBarUiState(
        items = openTabs.mapIndexed { index, tab ->
            WorkspaceTabBarItemUiState(
                tab = tab,
                isSelected = tab.tabId == selectedTab?.tabId,
                hasOtherTabs = openTabs.size > 1,
                canCloseLeftTabs = index > 0,
                canCloseRightTabs = index < openTabs.lastIndex,
            )
        },
        selectedTab = selectedTab,
        selectedTabIndex = selectedTabIndex,
        canShowMixedViewActions = selectedTab?.mode == OpenTabMode.MIXED,
        nextPreferredKind = selectedTab
            ?.takeIf { it.mode == OpenTabMode.MIXED }
            ?.let(::resolveNextPreferredKind),
    )
}

internal fun resolveNextPreferredKind(tab: OpenTabUiModel): String {
    val paneKinds = tab.panes
        .sortedBy { it.paneIndex }
        .map { it.kind }
        .distinct()
    val currentPreferredKind = paneKinds.firstOrNull() ?: tab.primaryKind
    return paneKinds.getOrNull(1) ?: oppositeKind(currentPreferredKind)
}

internal fun oppositeKind(kind: String?): String {
    return if (kind == OPEN_TAB_KIND_SMALI) {
        OPEN_TAB_KIND_JAVA
    } else {
        OPEN_TAB_KIND_SMALI
    }
}
