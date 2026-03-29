package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dexclub.codeview.compose.PlatformEditorBridge
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerScrollController
import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.codeview.core.annotation.CodeInteractionTrigger
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.core.document.DocumentRevision
import kotlinx.coroutines.CoroutineScope

internal fun Modifier.codeEditorInteractionModifier(
    platformEditorBridge: PlatformEditorBridge,
    useFallbackLongPressContextMenu: Boolean,
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    documentId: DocumentId,
    documentRevision: DocumentRevision,
    lineHeightPx: Float,
    contentStartPaddingPx: Float,
    fieldValue: TextFieldValue,
    clipboard: Clipboard,
    scope: CoroutineScope,
    preferredColumn: Int?,
    onPreferredColumnChange: (Int?) -> Unit,
    onRequestImeFocus: () -> Unit,
    isSoftwareKeyboardVisible: () -> Boolean,
    onInterruptInputAnchor: () -> Unit,
    onAnyPointerEditing: () -> Unit,
    scrollController: CodeViewerScrollController,
    onTapInsideSelection: () -> Unit,
    onLongPressSelectionGestureStart: (selection: TextRange, viewportPosition: Offset) -> Unit,
    onLongPressSelectionGestureMove: (viewportPosition: Offset) -> Unit,
    onLongPressSelectionGestureEnd: () -> Unit,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)?,
    onFieldValueChange: (TextFieldValue) -> Unit,
): Modifier {
    val pointerModifier = if (platformEditorBridge.useTouchSelectionGestures) {
        codeEditorTouchPointerInput(
            layoutSnapshot = layoutSnapshot,
            lineLayoutCache = lineLayoutCache,
            lineHeightPx = lineHeightPx,
            contentStartPaddingPx = contentStartPaddingPx,
            selection = fieldValue.selection,
            onFieldValueChange = onFieldValueChange,
            requestContentFocus = {},
            requestImeFocusOnTap = onRequestImeFocus,
            isSoftwareKeyboardVisibleOnTap = isSoftwareKeyboardVisible,
            onInterruptInputAnchor = onInterruptInputAnchor,
            onAnyPointerEditing = onAnyPointerEditing,
            scrollController = scrollController,
            onTapInsideSelection = onTapInsideSelection,
            onLongPressSelectionGestureStart = onLongPressSelectionGestureStart,
            onLongPressSelectionGestureMove = onLongPressSelectionGestureMove,
            onLongPressSelectionGestureEnd = onLongPressSelectionGestureEnd,
            onLongPressSelectionComplete = if (useFallbackLongPressContextMenu) {
                { textOffset: Int, _: TextRange, position: Offset ->
                    onContextMenu?.invoke(
                        buildCodeEditorContextMenuAnnotationHit(
                            layoutSnapshot = layoutSnapshot,
                            documentId = documentId,
                            documentRevision = documentRevision,
                            textOffset = textOffset,
                        ),
                        position,
                    )
                }
            } else {
                null
            },
        )
    } else {
        codeEditorDesktopPointerInput(
            layoutSnapshot = layoutSnapshot,
            lineLayoutCache = lineLayoutCache,
            lineHeightPx = lineHeightPx,
            contentStartPaddingPx = contentStartPaddingPx,
            onFieldValueChange = onFieldValueChange,
            requestContentFocus = {},
            requestImeFocus = onRequestImeFocus,
            onInterruptInputAnchor = onInterruptInputAnchor,
            onAnyPointerEditing = onAnyPointerEditing,
        )
    }

    return with(platformEditorBridge) {
        pointerModifier.bindEditorInput(
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
}

internal fun buildCodeEditorContextMenuAnnotationHit(
    layoutSnapshot: CodeLayoutSnapshot,
    documentId: DocumentId,
    documentRevision: DocumentRevision,
    textOffset: Int,
): CodeAnnotationHit? {
    val annotation = layoutSnapshot.findAnnotationAtOffset(textOffset) ?: return null
    return CodeAnnotationHit(
        annotation = annotation,
        range = annotation.range,
        trigger = CodeInteractionTrigger.ContextMenu,
        documentId = documentId,
        revision = documentRevision,
    )
}
