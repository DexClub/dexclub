package io.github.dexclub.app.scene.workspace

import io.github.dexclub.app.model.OpenTabUiModel
import io.github.dexclub.core.editor.EditorInPageSearchState
import io.github.dexclub.core.navigation.NavigateRequestContext
import io.github.dexclub.codeview.core.text.LineSelection

internal data class WorkspaceTabBarCallbacks(
    val onToggleOpenTab: (OpenTabUiModel) -> Unit,
    val onCloseOpenTab: (OpenTabUiModel) -> Unit,
    val onCloseAllOpenTabs: () -> Unit,
    val onCloseOtherOpenTabs: (OpenTabUiModel) -> Unit,
    val onCloseOpenTabsToRight: (OpenTabUiModel) -> Unit,
    val onCloseOpenTabsToLeft: (OpenTabUiModel) -> Unit,
    val onToggleCodeView: (OpenTabUiModel) -> Unit,
    val onPrioritizeKind: (OpenTabUiModel, String) -> Unit,
)

internal data class WorkspaceCodePaneCallbacks(
    val onConsumeNavigationRevealTarget: (NavigationRevealTarget) -> Unit,
    val onCodeViewportChanged: (String, String, Int, Int) -> Unit,
    val onActivatePane: (OpenTabUiModel, Int, String) -> Unit,
    val onUpdateInPageSearchState: (String, String, EditorInPageSearchState) -> Unit,
    val onUpdateCursorSelection: (String, String, Int, Int, LineSelection?) -> Unit,
    val onUpdateScrollOffset: (String, String, Int, Int) -> Unit,
    val onNavigateToDefinition: (NavigateRequestContext) -> Unit,
)
