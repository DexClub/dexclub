package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dexclub.codeview.compose.PlatformSelectionToolbarBridge
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerCanvasMetrics
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerViewportSnapshot

@Composable
internal fun CodeEditorTouchInteractionOverlays(
    selectionToolbarBridge: PlatformSelectionToolbarBridge,
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    canvasMetrics: CodeViewerCanvasMetrics,
    viewportSnapshot: CodeViewerViewportSnapshot,
    contentBoundsInWindow: Rect,
    fieldValue: TextFieldValue,
    clipboard: Clipboard,
    onSelectAllRequested: () -> Unit,
    onSelectionChange: (TextRange) -> Unit,
    onHandleInteractionStart: () -> Unit,
) {
    if (selectionToolbarBridge.usePlatformSelectionToolbar) {
        CodeEditorSelectionToolbar(
            bridge = selectionToolbarBridge,
            layoutSnapshot = layoutSnapshot,
            lineLayoutCache = lineLayoutCache,
            canvasMetrics = canvasMetrics,
            viewportSnapshot = viewportSnapshot,
            contentBoundsInWindow = contentBoundsInWindow,
            fieldValue = fieldValue,
            clipboard = clipboard,
            onSelectAllRequested = onSelectAllRequested,
        )
    }

    CodeEditorTouchSelectionHandles(
        density = androidx.compose.ui.platform.LocalDensity.current,
        layoutSnapshot = layoutSnapshot,
        lineLayoutCache = lineLayoutCache,
        canvasMetrics = canvasMetrics,
        viewportSnapshot = viewportSnapshot,
        selection = fieldValue.selection,
        onSelectionChange = onSelectionChange,
        onHandleInteractionStart = onHandleInteractionStart,
    )
}
