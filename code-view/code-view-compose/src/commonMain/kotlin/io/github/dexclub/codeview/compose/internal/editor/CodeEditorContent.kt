package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.dexclub.codeview.compose.CodeViewerCursorTarget
import io.github.dexclub.codeview.compose.CodeViewerInteractionOptions
import io.github.dexclub.codeview.compose.PlatformEditorBridge
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerCanvas
import io.github.dexclub.codeview.compose.rememberPlatformEditorBridge
import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.core.document.DocumentRevision
import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection
import kotlinx.coroutines.CoroutineScope

// 调试开关：用于临时显示输入锚点边框，定位 Desktop IME 锚点问题。
// 保留该常量，后续如需继续排查输入法候选窗问题可直接打开；不要直接删除。
private const val SHOW_INPUT_ANCHOR_DEBUG_OVERLAY: Boolean = false

@Composable
internal fun CodeEditorContent(
    documentId: DocumentId,
    documentRevision: DocumentRevision,
    layoutSnapshot: CodeLayoutSnapshot,
    modifier: Modifier,
    textStyle: TextStyle,
    readOnly: Boolean,
    searchHighlight: LineSelection?,
    initialFirstVisibleLine: Int,
    initialScrollOffsetX: Int,
    selection: LineSelection?,
    cursor: Cursor?,
    cursorTarget: CodeViewerCursorTarget?,
    interactionOptions: CodeViewerInteractionOptions,
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)?,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)?,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)?,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)?,
    followCursorToken: Long?,
    fieldValue: TextFieldValue,
    onFieldValueChange: (TextFieldValue) -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val platformEditorBridge = rememberPlatformEditorBridge()
    val inputAnchorState = rememberCodeEditorInputAnchorState(documentId)
    val imeFocusRequester = remember(documentId) { FocusRequester() }
    var preferredColumn by remember(documentId) { mutableStateOf<Int?>(null) }
    var pendingAnchorFocusRequest by remember(documentId) { mutableStateOf(false) }
    val composingOverlay = inputAnchorState.toComposingOverlayOrNull()

    LaunchedEffect(
        pendingAnchorFocusRequest,
        platformEditorBridge.useFloatingInputAnchor,
        fieldValue.selection.start,
        fieldValue.selection.end,
        layoutSnapshot.text.length,
    ) {
        if (!pendingAnchorFocusRequest) return@LaunchedEffect
        platformEditorBridge.requestInputFocus(imeFocusRequester)
        pendingAnchorFocusRequest = false
    }

    Box(modifier = modifier) {
        if (!readOnly && !platformEditorBridge.useFloatingInputAnchor) {
            platformEditorBridge.InputHost(
                modifier = Modifier.fillMaxSize(),
                inputAnchorState = inputAnchorState,
                fieldValue = fieldValue,
                layoutSnapshot = layoutSnapshot,
                clipboard = clipboard,
                scope = scope,
                preferredColumn = preferredColumn,
                onPreferredColumnChange = { preferredColumn = it },
                onInterruptInputAnchor = { inputAnchorState.clear() },
                textStyle = textStyle,
                focusRequester = imeFocusRequester,
                onFieldValueChange = onFieldValueChange,
            )
        }

        CodeViewerCanvas(
            documentKey = documentId,
            documentRevision = documentRevision,
            layoutSnapshot = layoutSnapshot,
            modifier = Modifier.fillMaxSize(),
            textStyle = textStyle,
            initialFirstVisibleLine = initialFirstVisibleLine,
            initialScrollOffsetX = initialScrollOffsetX,
            selection = selection,
            cursor = cursor,
            searchHighlight = searchHighlight,
            composingOverlay = composingOverlay,
            cursorTarget = cursorTarget,
            interactionOptions = interactionOptions,
            onAnnotationHit = onAnnotationHit,
            onContextMenu = onContextMenu,
            onScrollChange = onScrollChange,
            onViewportChange = onViewportChange,
            followCursorToken = followCursorToken,
            enablePrimaryAnnotationClick = readOnly,
            enableLongPressContextMenu = readOnly,
            enableSecondaryClickContextMenu = true,
            contentModifierTransform = if (readOnly) {
                null
            } else {
                { baseModifier, canvasMetrics, lineLayoutCache ->
                    baseModifier
                        .codeEditorPointerInput(
                            layoutSnapshot = layoutSnapshot,
                            lineLayoutCache = lineLayoutCache,
                            lineHeightPx = canvasMetrics.lineHeightPx,
                            onFieldValueChange = onFieldValueChange,
                            requestContentFocus = {},
                            requestImeFocus = {
                                pendingAnchorFocusRequest = true
                            },
                            onInterruptInputAnchor = { inputAnchorState.clear() },
                            onAnyPointerEditing = { preferredColumn = null },
                        )
                        .bindPlatformEditorInput(
                            bridge = platformEditorBridge,
                            fieldValue = fieldValue,
                            layoutSnapshot = layoutSnapshot,
                            clipboard = clipboard,
                            scope = scope,
                            preferredColumn = preferredColumn,
                            onPreferredColumnChange = { preferredColumn = it },
                            onInterruptInputAnchor = { inputAnchorState.clear() },
                            onFieldValueChange = onFieldValueChange,
                        )
                }
            },
            floatingUnderlayContent = if (readOnly || !platformEditorBridge.useFloatingInputAnchor) {
                null
            } else {
                { canvasMetrics, lineLayoutCache, viewportSnapshot ->
                    val density = LocalDensity.current
                    val anchorModifier = inputAnchorModifier(
                        density = density,
                        layoutSnapshot = layoutSnapshot,
                        canvasMetrics = canvasMetrics,
                        lineLayoutCache = lineLayoutCache,
                        viewportSnapshot = viewportSnapshot,
                        fieldSelection = fieldValue.selection,
                        composingOverlay = composingOverlay,
                    )
                    platformEditorBridge.InputHost(
                        modifier = anchorModifier,
                        inputAnchorState = inputAnchorState,
                        fieldValue = fieldValue,
                        layoutSnapshot = layoutSnapshot,
                        clipboard = clipboard,
                        scope = scope,
                        preferredColumn = preferredColumn,
                        onPreferredColumnChange = { preferredColumn = it },
                        onInterruptInputAnchor = { inputAnchorState.clear() },
                        textStyle = textStyle,
                        focusRequester = imeFocusRequester,
                        onFieldValueChange = onFieldValueChange,
                    )
                }
            },
            floatingContent = if (readOnly || !platformEditorBridge.useFloatingInputAnchor) {
                null
            } else {
                { canvasMetrics, lineLayoutCache, viewportSnapshot ->
                    val density = LocalDensity.current
                    if (SHOW_INPUT_ANCHOR_DEBUG_OVERLAY) {
                        Box(
                            modifier = inputAnchorModifier(
                                density = density,
                                layoutSnapshot = layoutSnapshot,
                                canvasMetrics = canvasMetrics,
                                lineLayoutCache = lineLayoutCache,
                                viewportSnapshot = viewportSnapshot,
                                fieldSelection = fieldValue.selection,
                                composingOverlay = composingOverlay,
                            ).border(
                                width = 1.dp,
                                color = Color.Red,
                            ),
                        )
                    }
                }
            },
            underlayContent = null,
            overlayContent = null,
        )
    }
}

private fun Modifier.bindPlatformEditorInput(
    bridge: PlatformEditorBridge,
    fieldValue: TextFieldValue,
    layoutSnapshot: CodeLayoutSnapshot,
    clipboard: Clipboard,
    scope: CoroutineScope,
    preferredColumn: Int?,
    onPreferredColumnChange: (Int?) -> Unit,
    onInterruptInputAnchor: () -> Unit,
    onFieldValueChange: (TextFieldValue) -> Unit,
): Modifier = with(bridge) {
    bindEditorInput(
        fieldValue = fieldValue,
        layoutSnapshot = layoutSnapshot,
        clipboard = clipboard,
        scope = scope,
        preferredColumn = preferredColumn,
        onPreferredColumnChange = onPreferredColumnChange,
        onInterruptInputAnchor = onInterruptInputAnchor,
        onFieldValueChange = onFieldValueChange,
    )
}
