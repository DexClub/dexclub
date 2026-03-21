package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.Card
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun WorkspaceSceneExpanded(
    uiState: WorkspaceUiState,
    headerCallbacks: WorkspaceHeaderCallbacks,
    sidePanelCallbacks: WorkspaceSidePanelCallbacks,
    tabBarCallbacks: WorkspaceTabBarCallbacks,
    paneCallbacks: WorkspaceCodePaneCallbacks,
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var sidePanelWidthPx by remember { mutableFloatStateOf(with(density) { 300.dp.toPx() }) }
    val minSideWidthPx = remember(density) { with(density) { 20.dp.toPx() } }
    val minCodeWidthPx = remember(density) { with(density) { 20.dp.toPx() } }
    val dragHandleWidth = 5.dp
    val dragHandleWidthPx = remember(density) { with(density) { dragHandleWidth.toPx() } }
    var containerWidthPx by remember { mutableIntStateOf(0) }

    val sidePanel = remember {
        movableContentOf<
            WorkspaceHeaderUiState,
            WorkspaceSidePanelUiState,
            WorkspaceHeaderCallbacks,
            WorkspaceSidePanelCallbacks,
        > { headerUiState, sidePanelUiState, currentHeaderCallbacks, currentSidePanelCallbacks ->
            WorkspaceSidePanel(
                headerUiState = headerUiState,
                sidePanelUiState = sidePanelUiState,
                headerCallbacks = currentHeaderCallbacks,
                sidePanelCallbacks = currentSidePanelCallbacks,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    val codePanel = remember {
        movableContentOf<
            WorkspaceCodePanelUiState,
            WorkspaceTabBarCallbacks,
            WorkspaceCodePaneCallbacks,
        > { codePanelUiState, currentTabBarCallbacks, currentPaneCallbacks ->
            WorkspaceCodePanel(
                uiState = codePanelUiState,
                tabBarCallbacks = currentTabBarCallbacks,
                paneCallbacks = currentPaneCallbacks,
                layoutMode = WorkspaceLayoutMode.Expanded,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerWidthPx = it.width },
    ) {
        Card(
            contentPadding = PaddingValues.Zero,
            modifier = Modifier
                .fillMaxHeight()
                .layout { measurable, constraints ->
                    val widthPx = sidePanelWidthPx.roundToInt()
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = widthPx, maxWidth = widthPx),
                    )
                    layout(widthPx, placeable.height) {
                        placeable.place(0, 0)
                    }
                },
        ) {
            sidePanel(uiState.headerUiState, uiState.sidePanelUiState, headerCallbacks, sidePanelCallbacks)
        }
        Box(
            modifier = Modifier
                .width(dragHandleWidth)
                .fillMaxHeight()
                .then(dragHandleModifier)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val maxWidthPx = (containerWidthPx - dragHandleWidthPx - minCodeWidthPx)
                            .coerceAtLeast(minSideWidthPx)
                        val newWidth = (sidePanelWidthPx + dragAmount.x)
                            .coerceIn(minSideWidthPx, maxWidthPx)
                        if (abs(newWidth - sidePanelWidthPx) >= 1f) {
                            sidePanelWidthPx = newWidth
                        }
                    }
                },
        )
        Card(
            contentPadding = PaddingValues.Zero,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            codePanel(uiState.codePanelUiState, tabBarCallbacks, paneCallbacks)
        }
    }
}
