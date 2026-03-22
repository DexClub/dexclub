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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.dexclub.codeview.compose.CodeViewerCursorTarget
import io.github.dexclub.codeview.compose.CodeViewerInteractionOptions
import io.github.dexclub.codeview.compose.rememberPlatformEditorBridge
import io.github.dexclub.codeview.compose.rememberPlatformSelectionToolbarBridge
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerCanvas
import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.core.document.DocumentRevision
import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection

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
    val selectionToolbarBridge = rememberPlatformSelectionToolbarBridge()
    val inputAnchorState = rememberCodeEditorInputAnchorState(documentId)
    val imeFocusRequester = remember(documentId) { FocusRequester() }
    var preferredColumn by remember(documentId) { mutableStateOf<Int?>(null) }
    var pendingAnchorFocusRequest by remember(documentId) { mutableStateOf(false) }
    var suppressTouchSelectionToolbar by remember(documentId) { mutableStateOf(false) }
    var selectionToolbarRequestToken by remember(documentId) { mutableStateOf(0L) }
    var contentBoundsInWindow by remember(documentId) { mutableStateOf(Rect.Zero) }
    val composingOverlay = inputAnchorState.toComposingOverlayOrNull()
    val showFloatingInputAnchor = !readOnly && platformEditorBridge.useFloatingInputAnchor
    val showTouchSelectionOverlays = !readOnly && platformEditorBridge.useTouchSelectionGestures

    fun requestImeFocus() {
        pendingAnchorFocusRequest = true
    }

    fun interruptInputAnchor() {
        inputAnchorState.clear()
    }

    fun resetPreferredColumn() {
        preferredColumn = null
    }

    fun updateSelection(nextSelection: TextRange) {
        onFieldValueChange(
            fieldValue.copy(
                selection = nextSelection,
                composition = null,
            )
        )
    }

    fun handleTouchSelectionInteractionStart() {
        suppressTouchSelectionToolbar = true
        resetPreferredColumn()
        interruptInputAnchor()
    }

    fun handleTouchSelectionInteractionEnd() {
        suppressTouchSelectionToolbar = false
    }

    fun requestSelectionToolbar() {
        suppressTouchSelectionToolbar = false
        selectionToolbarRequestToken += 1L
    }

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

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            contentBoundsInWindow = coordinates.boundsInWindow()
        },
    ) {
        if (!showFloatingInputAnchor && !readOnly) {
            platformEditorBridge.InputHost(
                modifier = Modifier.fillMaxSize(),
                inputAnchorState = inputAnchorState,
                fieldValue = fieldValue,
                layoutSnapshot = layoutSnapshot,
                clipboard = clipboard,
                scope = scope,
                preferredColumn = preferredColumn,
                onPreferredColumnChange = { preferredColumn = it },
                onInterruptInputAnchor = ::interruptInputAnchor,
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
                    baseModifier.codeEditorInteractionModifier(
                        platformEditorBridge = platformEditorBridge,
                        useFallbackLongPressContextMenu = !selectionToolbarBridge.usePlatformSelectionToolbar,
                        layoutSnapshot = layoutSnapshot,
                        lineLayoutCache = lineLayoutCache,
                        documentId = documentId,
                        documentRevision = documentRevision,
                        lineHeightPx = canvasMetrics.lineHeightPx,
                        fieldValue = fieldValue,
                        clipboard = clipboard,
                        scope = scope,
                        preferredColumn = preferredColumn,
                        onPreferredColumnChange = { preferredColumn = it },
                        onRequestImeFocus = ::requestImeFocus,
                        onInterruptInputAnchor = ::interruptInputAnchor,
                        onAnyPointerEditing = ::resetPreferredColumn,
                        onTapInsideSelection = ::requestSelectionToolbar,
                        onContextMenu = onContextMenu,
                        onFieldValueChange = onFieldValueChange,
                    )
                }
            },
            floatingUnderlayContent = if (!showFloatingInputAnchor) {
                null
            } else {
                { canvasMetrics, lineLayoutCache, viewportSnapshot ->
                    val anchorModifier = inputAnchorModifier(
                        density = LocalDensity.current,
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
                        onInterruptInputAnchor = ::interruptInputAnchor,
                        textStyle = textStyle,
                        focusRequester = imeFocusRequester,
                        onFieldValueChange = onFieldValueChange,
                    )
                }
            },
            floatingContent = if (readOnly || (!showFloatingInputAnchor && !showTouchSelectionOverlays)) {
                null
            } else {
                { canvasMetrics, lineLayoutCache, viewportSnapshot ->
                    if (showTouchSelectionOverlays) {
                        CodeEditorTouchInteractionOverlays(
                            selectionToolbarBridge = selectionToolbarBridge,
                            showSelectionToolbar = !suppressTouchSelectionToolbar,
                            showSelectionToolbarRequestToken = selectionToolbarRequestToken,
                            layoutSnapshot = layoutSnapshot,
                            lineLayoutCache = lineLayoutCache,
                            canvasMetrics = canvasMetrics,
                            viewportSnapshot = viewportSnapshot,
                            contentBoundsInWindow = contentBoundsInWindow,
                            fieldValue = fieldValue,
                            clipboard = clipboard,
                            onSelectAllRequested = {
                                updateSelection(TextRange(0, fieldValue.text.length))
                            },
                            onSelectionChange = ::updateSelection,
                            onHandleInteractionStart = ::handleTouchSelectionInteractionStart,
                            onHandleInteractionEnd = ::handleTouchSelectionInteractionEnd,
                        )
                    }
                    if (SHOW_INPUT_ANCHOR_DEBUG_OVERLAY) {
                        Box(
                            modifier = inputAnchorModifier(
                                density = LocalDensity.current,
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
