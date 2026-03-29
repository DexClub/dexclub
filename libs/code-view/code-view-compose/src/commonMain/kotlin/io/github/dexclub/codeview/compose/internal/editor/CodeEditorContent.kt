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
import androidx.compose.runtime.withFrameNanos
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
import io.github.dexclub.codeview.compose.CodeContentOptions
import io.github.dexclub.codeview.compose.CodeDecorationOptions
import io.github.dexclub.codeview.compose.CodeGutterOptions
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
    scrollPastEnd: Int,
    selection: LineSelection?,
    cursor: Cursor?,
    cursorTarget: CodeViewerCursorTarget?,
    interactionOptions: CodeViewerInteractionOptions,
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)?,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)?,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)?,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)?,
    gutterOptions: CodeGutterOptions,
    contentOptions: CodeContentOptions,
    decorationOptions: CodeDecorationOptions,
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
    var touchInteractionState by remember(documentId) {
        mutableStateOf<TouchSelectionInteractionState>(TouchSelectionInteractionState.Idle)
    }
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
        if (fieldValue.selection == nextSelection) return
        onFieldValueChange(
            fieldValue.copy(
                selection = nextSelection,
                composition = null,
            )
        )
    }

    fun handleTouchSelectionInteractionStart() {
        resetPreferredColumn()
        interruptInputAnchor()
        touchInteractionState = TouchSelectionInteractionState.DraggingHandle()
    }

    fun handleTouchSelectionInteractionEnd() {
        touchInteractionState = TouchSelectionInteractionState.Idle
    }

    fun handleTouchSelectionHandleAutoScrollStart(
        target: TouchHandleAutoScrollTarget,
        viewportPosition: Offset,
    ) {
        val session = HandleTouchSelectionAutoScrollSession(
            target = target,
            initialViewportPosition = viewportPosition,
        )
        touchInteractionState = TouchSelectionInteractionState.DraggingHandle(
            autoScrollSession = session,
        )
    }

    fun updateTouchAutoScrollViewportPosition(viewportPosition: Offset) {
        touchInteractionState.autoScrollSession?.viewportPosition = viewportPosition
    }

    fun clearTouchAutoScrollSession() {
        val currentState = touchInteractionState
        touchInteractionState = when (currentState) {
            is TouchSelectionInteractionState.DraggingHandle -> currentState.copy(
                autoScrollSession = null,
            )

            else -> currentState
        }
    }

    fun handleTouchSelectionGestureStart(
        initialSelection: TextRange,
        viewportPosition: Offset,
    ) {
        touchInteractionState = TouchSelectionInteractionState.LongPressSelecting(
            autoScrollSession = LongPressTouchSelectionAutoScrollSession(
                initialSelection = initialSelection,
                initialViewportPosition = viewportPosition,
            )
        )
        resetPreferredColumn()
        interruptInputAnchor()
    }

    fun handleTouchSelectionGestureMove(viewportPosition: Offset) {
        updateTouchAutoScrollViewportPosition(viewportPosition)
    }

    fun handleTouchSelectionGestureEnd() {
        touchInteractionState = TouchSelectionInteractionState.Idle
    }

    fun requestSelectionToolbar() {
        touchInteractionState = TouchSelectionInteractionState.Idle
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
            scrollPastEnd = scrollPastEnd,
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
            gutterOptions = gutterOptions,
            contentOptions = contentOptions,
            decorationOptions = decorationOptions,
            enablePrimaryAnnotationClick = readOnly,
            enableLongPressContextMenu = readOnly,
            enableSecondaryClickContextMenu = true,
            contentModifierTransform = if (readOnly) {
                null
            } else {
                { baseModifier, canvasMetrics, lineLayoutCache, scrollController ->
                    baseModifier.codeEditorInteractionModifier(
                        platformEditorBridge = platformEditorBridge,
                        useFallbackLongPressContextMenu = !selectionToolbarBridge.usePlatformSelectionToolbar,
                        layoutSnapshot = layoutSnapshot,
                        lineLayoutCache = lineLayoutCache,
                        documentId = documentId,
                        documentRevision = documentRevision,
                        lineHeightPx = canvasMetrics.lineHeightPx,
                        contentStartPaddingPx = canvasMetrics.contentStartPaddingPx,
                        fieldValue = fieldValue,
                        clipboard = clipboard,
                        scope = scope,
                        preferredColumn = preferredColumn,
                        onPreferredColumnChange = { preferredColumn = it },
                        onRequestImeFocus = ::requestImeFocus,
                        isSoftwareKeyboardVisible = platformEditorBridge::isSoftwareKeyboardVisible,
                        onInterruptInputAnchor = ::interruptInputAnchor,
                        onAnyPointerEditing = ::resetPreferredColumn,
                        scrollController = scrollController,
                        onTapInsideSelection = ::requestSelectionToolbar,
                        onLongPressSelectionGestureStart = ::handleTouchSelectionGestureStart,
                        onLongPressSelectionGestureMove = ::handleTouchSelectionGestureMove,
                        onLongPressSelectionGestureEnd = ::handleTouchSelectionGestureEnd,
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
                { canvasMetrics, lineLayoutCache, viewportSnapshot, scrollController ->
                    val activeTouchAutoScrollSession = touchInteractionState.autoScrollSession
                    if (activeTouchAutoScrollSession != null) {
                        LaunchedEffect(
                            activeTouchAutoScrollSession,
                            scrollController,
                            layoutSnapshot,
                            canvasMetrics.lineHeightPx,
                        ) {
                            var previousFrameTimeNanos = 0L
                            while (touchInteractionState.autoScrollSession === activeTouchAutoScrollSession) {
                                val frameTimeNanos = withFrameNanos { it }
                                val frameDurationNanos = if (previousFrameTimeNanos == 0L) {
                                    DEFAULT_TOUCH_AUTO_SCROLL_FRAME_DURATION_NANOS
                                } else {
                                    frameTimeNanos - previousFrameTimeNanos
                                }
                                previousFrameTimeNanos = frameTimeNanos
                                val autoScrollDelta = resolveTouchAutoScrollDelta(
                                    viewportPosition = activeTouchAutoScrollSession.viewportPosition,
                                    scrollController = scrollController,
                                    frameDurationNanos = frameDurationNanos,
                                )
                                if (autoScrollDelta != Offset.Zero) {
                                    scrollController.scrollBy(
                                        horizontalDeltaPx = autoScrollDelta.x,
                                        verticalDeltaPx = autoScrollDelta.y,
                                    )
                                    updateSelection(
                                        activeTouchAutoScrollSession.resolveSelection(
                                            layoutSnapshot = layoutSnapshot,
                                            lineLayoutCache = lineLayoutCache,
                                            lineHeightPx = canvasMetrics.lineHeightPx,
                                            scrollController = scrollController,
                                        )
                                    )
                                }
                            }
                        }
                    }
                    if (showTouchSelectionOverlays) {
                        CodeEditorTouchInteractionOverlays(
                            selectionToolbarBridge = selectionToolbarBridge,
                            showSelectionToolbar = touchInteractionState.showSelectionToolbar,
                            showSelectionHandles = touchInteractionState.showSelectionHandles,
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
                            onHandleAutoScrollStart = ::handleTouchSelectionHandleAutoScrollStart,
                            onHandleAutoScrollMove = ::updateTouchAutoScrollViewportPosition,
                            onHandleAutoScrollEnd = ::clearTouchAutoScrollSession,
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
