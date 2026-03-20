package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.shadcn.ui.compose.ShadcnTheme

@Composable
internal fun WorkspaceSidePanel(
    headerUiState: WorkspaceHeaderUiState,
    sidePanelUiState: WorkspaceSidePanelUiState,
    headerCallbacks: WorkspaceHeaderCallbacks,
    sidePanelCallbacks: WorkspaceSidePanelCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        SideHeaderBar(
            uiState = headerUiState,
            callbacks = headerCallbacks,
            modifier = Modifier,
        )
        HorizontalDivider(
            color = ShadcnTheme.colors.border.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = ContentHorizontalPadding),
        )
        SideLazyColumn(
            uiState = sidePanelUiState,
            callbacks = sidePanelCallbacks,
            modifier = Modifier,
        )
    }
}
