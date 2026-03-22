package io.github.dexclub.app.scene.workspace

import io.github.dexclub.app.model.OpenTabUiModel
import io.github.dexclub.core.editor.EditorContentStateSnapshot
import io.github.dexclub.core.editor.buildEditorContentKey
import io.github.dexclub.settings.AppSettings

data class WorkspaceCodePaneUiState(
    val contentKey: String,
    val text: String = "",
    val editorState: EditorContentStateSnapshot = EditorContentStateSnapshot(),
    val scrollPastEnd: Int = AppSettings().codeScrollPastEnd,
)

data class WorkspaceCodePanelUiState(
    val openTabs: List<OpenTabUiModel> = emptyList(),
    val selectedOpenTab: OpenTabUiModel? = null,
    val navigationRevealTarget: NavigationRevealTarget? = null,
    val tabBarUiState: WorkspaceTabBarUiState = WorkspaceTabBarUiState(),
    val paneStates: Map<String, WorkspaceCodePaneUiState> = emptyMap(),
) {
    fun paneState(tabId: String, kind: String): WorkspaceCodePaneUiState {
        val contentKey = buildEditorContentKey(tabId, kind)
        return paneStates[contentKey] ?: WorkspaceCodePaneUiState(contentKey = contentKey)
    }
}

internal fun buildWorkspaceCodePanelUiState(
    openTabs: List<OpenTabUiModel>,
    selectedOpenTab: OpenTabUiModel?,
    navigationRevealTarget: NavigationRevealTarget?,
    codeContents: Map<String, String>,
    appSettings: AppSettings,
    resolveEditorState: (tab: OpenTabUiModel, kind: String) -> EditorContentStateSnapshot,
): WorkspaceCodePanelUiState {
    return WorkspaceCodePanelUiState(
        openTabs = openTabs,
        selectedOpenTab = selectedOpenTab,
        navigationRevealTarget = navigationRevealTarget,
        tabBarUiState = buildWorkspaceTabBarUiState(
            openTabs = openTabs,
            selectedOpenTab = selectedOpenTab,
        ),
        paneStates = buildWorkspaceCodePaneUiStates(
            openTabs = openTabs,
            codeContents = codeContents,
            appSettings = appSettings,
            resolveEditorState = resolveEditorState,
        ),
    )
}

internal fun buildWorkspaceCodePaneUiStates(
    openTabs: List<OpenTabUiModel>,
    codeContents: Map<String, String>,
    appSettings: AppSettings,
    resolveEditorState: (tab: OpenTabUiModel, kind: String) -> EditorContentStateSnapshot,
): Map<String, WorkspaceCodePaneUiState> {
    return buildMap {
        openTabs.forEach { tab ->
            (tab.contents.keys + tab.requiredKinds)
                .distinct()
                .forEach { kind ->
                    val contentKey = buildEditorContentKey(tab.tabId, kind)
                    put(
                        contentKey,
                        WorkspaceCodePaneUiState(
                            contentKey = contentKey,
                            text = codeContents[contentKey].orEmpty(),
                            editorState = resolveEditorState(tab, kind),
                            scrollPastEnd = appSettings.codeScrollPastEnd,
                        ),
                    )
                }
        }
    }
}
