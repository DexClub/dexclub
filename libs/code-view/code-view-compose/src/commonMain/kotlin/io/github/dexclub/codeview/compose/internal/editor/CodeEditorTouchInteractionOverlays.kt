package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalDensity
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
    showSelectionToolbar: Boolean,
    showSelectionHandles: Boolean,
    showSelectionToolbarRequestToken: Long,
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
    onHandleInteractionEnd: () -> Unit,
    onHandleAutoScrollStart: (TouchHandleAutoScrollTarget, Offset) -> Unit,
    onHandleAutoScrollMove: (Offset) -> Unit,
    onHandleAutoScrollEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val selection = fieldValue.selection

    if (!selection.collapsed && showSelectionToolbar && selectionToolbarBridge.usePlatformSelectionToolbar) {
        CodeEditorSelectionToolbar(
            bridge = selectionToolbarBridge,
            showRequestToken = showSelectionToolbarRequestToken,
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

    if (!showSelectionHandles) {
        return
    }

    if (selection.collapsed) {
        CodeEditorTouchCursorHandle(
            density = density,
            layoutSnapshot = layoutSnapshot,
            lineLayoutCache = lineLayoutCache,
            canvasMetrics = canvasMetrics,
            viewportSnapshot = viewportSnapshot,
            selection = selection,
            onSelectionChange = onSelectionChange,
            onHandleInteractionStart = onHandleInteractionStart,
            onHandleInteractionEnd = onHandleInteractionEnd,
            onHandleAutoScrollStart = onHandleAutoScrollStart,
            onHandleAutoScrollMove = onHandleAutoScrollMove,
            onHandleAutoScrollEnd = onHandleAutoScrollEnd,
        )
    } else {
        CodeEditorTouchSelectionHandles(
            density = density,
            layoutSnapshot = layoutSnapshot,
            lineLayoutCache = lineLayoutCache,
            canvasMetrics = canvasMetrics,
            viewportSnapshot = viewportSnapshot,
            selection = selection,
            onSelectionChange = onSelectionChange,
            onHandleInteractionStart = onHandleInteractionStart,
            onHandleInteractionEnd = onHandleInteractionEnd,
            onHandleAutoScrollStart = onHandleAutoScrollStart,
            onHandleAutoScrollMove = onHandleAutoScrollMove,
            onHandleAutoScrollEnd = onHandleAutoScrollEnd,
        )
    }
}
