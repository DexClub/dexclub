package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.TextRange
import io.github.dexclub.codeview.compose.PlatformSelectionToolbarBridge
import io.github.dexclub.codeview.compose.copyText
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerCanvasMetrics
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerViewportSnapshot
import io.github.dexclub.codeview.compose.internal.viewer.contentViewportLeftPx
import io.github.dexclub.codeview.compose.internal.viewer.contentViewportRightPx
import io.github.dexclub.codeview.compose.internal.viewer.contentXToViewportX
import io.github.dexclub.codeview.compose.internal.viewer.contentYToViewportY
import kotlinx.coroutines.launch

@Composable
internal fun CodeEditorSelectionToolbar(
    bridge: PlatformSelectionToolbarBridge,
    showRequestToken: Long,
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    canvasMetrics: CodeViewerCanvasMetrics,
    viewportSnapshot: CodeViewerViewportSnapshot,
    contentBoundsInWindow: Rect,
    fieldValue: androidx.compose.ui.text.input.TextFieldValue,
    clipboard: Clipboard,
    onSelectAllRequested: () -> Unit,
) {
    val selection = fieldValue.selection
    val scope = rememberCoroutineScope()
    val selectedText = remember(fieldValue.text, selection) {
        fieldValue.selectedText()
    }
    val selectionRect = remember(
        layoutSnapshot,
        lineLayoutCache,
        canvasMetrics,
        viewportSnapshot,
        contentBoundsInWindow,
        selection.start,
        selection.end,
    ) {
        resolveSelectionToolbarRect(
            layoutSnapshot = layoutSnapshot,
            lineLayoutCache = lineLayoutCache,
            canvasMetrics = canvasMetrics,
            viewportSnapshot = viewportSnapshot,
            contentBoundsInWindow = contentBoundsInWindow,
            selection = selection,
        )
    }
    val visible = !selection.collapsed && selectedText.isNotEmpty() && !contentBoundsInWindow.isEmpty

    LaunchedEffect(bridge, visible, selectionRect, selectedText, showRequestToken) {
        if (!visible) {
            bridge.hideSelectionToolbar()
            return@LaunchedEffect
        }
        bridge.showSelectionToolbar(
            rect = selectionRect,
            onCopyRequested = {
                scope.launch {
                    clipboard.copyText(selectedText, label = "code_selection")
                }
            },
            onSelectAllRequested = onSelectAllRequested,
        )
    }

    DisposableEffect(bridge) {
        onDispose {
            bridge.hideSelectionToolbar()
        }
    }
}

private fun resolveSelectionToolbarRect(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    canvasMetrics: CodeViewerCanvasMetrics,
    viewportSnapshot: CodeViewerViewportSnapshot,
    contentBoundsInWindow: Rect,
    selection: TextRange,
): Rect {
    if (selection.collapsed || contentBoundsInWindow.isEmpty) {
        return contentBoundsInWindow
    }

    val normalizedStart = selection.normalizedStart
    val normalizedEnd = selection.normalizedEnd
    val startCursor = layoutSnapshot.offsetToCursor(normalizedStart)
    val endCursor = layoutSnapshot.offsetToCursor(normalizedEnd)
    val sameLine = startCursor.line == endCursor.line

    val leftPx = if (sameLine) {
        lineLayoutCache.columnX(startCursor.line, startCursor.offset)
    } else {
        0f
    }
    val rightPx = if (sameLine) {
        lineLayoutCache.columnX(endCursor.line, endCursor.offset).coerceAtLeast(leftPx + 1f)
    } else {
        viewportSnapshot.horizontalScrollPx + viewportSnapshot.contentViewportWidthPx
    }
    val topPx = startCursor.line * canvasMetrics.lineHeightPx
    val bottomPx = (endCursor.line + 1) * canvasMetrics.lineHeightPx

    val viewportLeft = viewportSnapshot.contentXToViewportX(leftPx)
        .coerceIn(viewportSnapshot.contentViewportLeftPx, viewportSnapshot.contentViewportRightPx)
    val viewportRight = viewportSnapshot.contentXToViewportX(rightPx)
        .coerceIn(
            viewportLeft + 1f,
            viewportSnapshot.contentViewportRightPx.coerceAtLeast(viewportLeft + 1f),
        )
    val viewportTop = viewportSnapshot.contentYToViewportY(topPx)
        .coerceIn(0f, viewportSnapshot.viewportHeightPx)
    val viewportBottom = viewportSnapshot.contentYToViewportY(bottomPx)
        .coerceIn(viewportTop + 1f, viewportSnapshot.viewportHeightPx.coerceAtLeast(viewportTop + 1f))

    return Rect(
        left = contentBoundsInWindow.left + viewportLeft,
        top = contentBoundsInWindow.top + viewportTop,
        right = contentBoundsInWindow.left + viewportRight,
        bottom = contentBoundsInWindow.top + viewportBottom,
    )
}
