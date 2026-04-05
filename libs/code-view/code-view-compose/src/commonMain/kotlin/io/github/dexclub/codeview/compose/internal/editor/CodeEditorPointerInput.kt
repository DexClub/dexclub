package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerScrollController
import kotlin.math.pow

internal fun Modifier.codeEditorDesktopPointerInput(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    lineHeightPx: Float,
    contentStartPaddingPx: Float,
    onFieldValueChange: (TextFieldValue) -> Unit,
    requestContentFocus: () -> Unit,
    requestImeFocus: () -> Unit,
    onInterruptInputAnchor: () -> Unit,
    onAnyPointerEditing: () -> Unit,
): Modifier {
    return pointerInput(layoutSnapshot.text, lineHeightPx) {
        var previousClickUptimeMillis = Long.MIN_VALUE
        var previousClickPosition = Offset.Unspecified
        var previousClickCount = 0

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val clickCount = resolveDesktopMultiClickCount(
                clickUptimeMillis = down.uptimeMillis,
                clickPosition = down.position,
                previousClickUptimeMillis = previousClickUptimeMillis,
                previousClickPosition = previousClickPosition,
                previousClickCount = previousClickCount,
                multiClickTimeoutMillis = viewConfiguration.doubleTapTimeoutMillis,
                multiClickSlopPx = viewConfiguration.touchSlop,
            )
            requestContentFocus()
            requestImeFocus()
            onInterruptInputAnchor()
            onAnyPointerEditing()

            val anchorOffset = resolveEditorTextOffset(
                layoutSnapshot = layoutSnapshot,
                lineLayoutCache = lineLayoutCache,
                lineHeightPx = lineHeightPx,
                contentStartPaddingPx = contentStartPaddingPx,
                position = down.position,
            )
            onFieldValueChange(
                TextFieldValue(
                    text = layoutSnapshot.text,
                    selection = resolveDesktopClickSelection(
                        layoutSnapshot = layoutSnapshot,
                        clickCount = clickCount,
                        anchorOffset = anchorOffset,
                    ),
                )
            )

            var dragged = false
            if (clickCount == 1) {
                drag(down.id) { change ->
                    dragged = true
                    val targetOffset = resolveEditorTextOffset(
                        layoutSnapshot = layoutSnapshot,
                        lineLayoutCache = lineLayoutCache,
                        lineHeightPx = lineHeightPx,
                        contentStartPaddingPx = contentStartPaddingPx,
                        position = change.position,
                    )
                    onFieldValueChange(
                        TextFieldValue(
                            text = layoutSnapshot.text,
                            selection = TextRange(anchorOffset, targetOffset),
                        )
                    )
                    change.consume()
                }
            } else {
                waitForUpOrCancellation()
            }

            if (dragged) {
                previousClickUptimeMillis = Long.MIN_VALUE
                previousClickPosition = Offset.Unspecified
                previousClickCount = 0
            } else {
                previousClickUptimeMillis = down.uptimeMillis
                previousClickPosition = down.position
                previousClickCount = clickCount
            }
        }
    }
}

internal fun Modifier.codeEditorTouchPointerInput(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    lineHeightPx: Float,
    contentStartPaddingPx: Float,
    selection: TextRange,
    onFieldValueChange: (TextFieldValue) -> Unit,
    requestContentFocus: () -> Unit,
    requestImeFocusOnTap: () -> Unit,
    isSoftwareKeyboardVisibleOnTap: () -> Boolean,
    onInterruptInputAnchor: () -> Unit,
    onAnyPointerEditing: () -> Unit,
    scrollController: CodeViewerScrollController,
    onTapInsideSelection: (() -> Unit)? = null,
    onLongPressSelectionGestureStart: ((selection: TextRange, viewportPosition: Offset) -> Unit)? = null,
    onLongPressSelectionGestureMove: ((viewportPosition: Offset) -> Unit)? = null,
    onLongPressSelectionGestureEnd: (() -> Unit)? = null,
    onLongPressSelectionComplete: ((textOffset: Int, selection: TextRange, position: Offset) -> Unit)? = null,
): Modifier {
    return this
        .pointerInput(layoutSnapshot.text, lineHeightPx, selection.start, selection.end) {
            detectTapGestures(
                onTap = { position ->
                    requestContentFocus()
                    val tappedOffset = resolveEditorTextOffset(
                        layoutSnapshot = layoutSnapshot,
                        lineLayoutCache = lineLayoutCache,
                        lineHeightPx = lineHeightPx,
                        contentStartPaddingPx = contentStartPaddingPx,
                        position = position,
                    )
                    val tapSelectionAction = resolveSelectionTapAction(
                        selection = selection,
                        tappedOffset = tappedOffset,
                        isSoftwareKeyboardVisible = isSoftwareKeyboardVisibleOnTap(),
                    )
                    requestImeFocusOnTap()
                    onInterruptInputAnchor()
                    onAnyPointerEditing()

                    when (tapSelectionAction) {
                        SelectionTapAction.KeepSelection -> {
                            onTapInsideSelection?.invoke()
                        }

                        SelectionTapAction.CollapseSelection -> {
                            onFieldValueChange(
                                TextFieldValue(
                                    text = layoutSnapshot.text,
                                    selection = TextRange(tappedOffset),
                                )
                            )
                        }
                    }
                },
            )
        }
        .pointerInput(layoutSnapshot.text, lineHeightPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture

                requestContentFocus()
                onInterruptInputAnchor()
                onAnyPointerEditing()

                val initialOffset = resolveEditorTextOffset(
                    layoutSnapshot = layoutSnapshot,
                    lineLayoutCache = lineLayoutCache,
                    lineHeightPx = lineHeightPx,
                    contentStartPaddingPx = contentStartPaddingPx,
                    position = longPress.position,
                )
                val initialSelection = resolveSelectionWordRange(
                    text = layoutSnapshot.text,
                    rawOffset = initialOffset,
                )
                var latestOffset = initialOffset
                var latestSelection = initialSelection
                var latestPosition = longPress.position
                var latestViewportPosition = resolveViewportPositionFromContentPosition(
                    contentPosition = longPress.position,
                    scrollController = scrollController,
                )

                longPress.consume()
                // Touch selection keeps the initial word as the anchor and defers toolbar/handle
                // presentation until the gesture finishes.
                onLongPressSelectionGestureStart?.invoke(initialSelection, latestViewportPosition)

                onFieldValueChange(
                    TextFieldValue(
                        text = layoutSnapshot.text,
                        selection = initialSelection,
                    )
                )

                drag(longPress.id) { change ->
                    latestPosition = change.position
                    latestViewportPosition = resolveViewportPositionFromContentPosition(
                        contentPosition = latestPosition,
                        scrollController = scrollController,
                    )
                    onLongPressSelectionGestureMove?.invoke(latestViewportPosition)
                    latestOffset = resolveEditorTextOffset(
                        layoutSnapshot = layoutSnapshot,
                        lineLayoutCache = lineLayoutCache,
                        lineHeightPx = lineHeightPx,
                        contentStartPaddingPx = contentStartPaddingPx,
                        position = latestPosition,
                    )
                    latestSelection = resolveLongPressDragSelection(
                        initialSelection = initialSelection,
                        draggedTextOffset = latestOffset,
                    )
                    onFieldValueChange(
                        TextFieldValue(
                            text = layoutSnapshot.text,
                            selection = latestSelection,
                        )
                    )
                    change.consume()
                }

                onLongPressSelectionGestureEnd?.invoke()
                if (!latestSelection.collapsed) {
                    onLongPressSelectionComplete?.invoke(
                        latestOffset,
                        latestSelection,
                        latestViewportPosition,
                    )
                }
            }
        }
}

internal fun resolveViewportPositionFromContentPosition(
    contentPosition: Offset,
    scrollController: CodeViewerScrollController,
): Offset {
    return Offset(
        x = scrollController.contentLeftInsetPx + contentPosition.x - scrollController.horizontalScrollPx,
        y = contentPosition.y - scrollController.verticalScrollPx,
    )
}

internal fun resolveLongPressDragSelectionState(
    viewportPosition: Offset,
    scrollController: CodeViewerScrollController,
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    lineHeightPx: Float,
    initialSelection: TextRange,
): LongPressDragSelectionState {
    val contentPosition = Offset(
        x = (viewportPosition.x - scrollController.contentLeftInsetPx) + scrollController.horizontalScrollPx,
        y = viewportPosition.y + scrollController.verticalScrollPx,
    )
    val textOffset = resolveEditorTextOffset(
        layoutSnapshot = layoutSnapshot,
        lineLayoutCache = lineLayoutCache,
        lineHeightPx = lineHeightPx,
        contentStartPaddingPx = scrollController.contentStartPaddingPx,
        position = contentPosition,
    )
    return LongPressDragSelectionState(
        contentPosition = contentPosition,
        textOffset = textOffset,
        selection = resolveLongPressDragSelection(
            initialSelection = initialSelection,
            draggedTextOffset = textOffset,
        ),
    )
}

internal fun resolveEditorTextOffset(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    lineHeightPx: Float,
    contentStartPaddingPx: Float,
    position: Offset,
): Int {
    if (lineHeightPx <= 0f) return 0
    val maxContentHeight = (layoutSnapshot.lineCount * lineHeightPx).coerceAtLeast(0f)
    val safeY = position.y.coerceIn(0f, maxContentHeight)
    val lineIndex = (safeY / lineHeightPx)
        .toInt()
        .coerceIn(0, layoutSnapshot.lineCount - 1)
    val lineOffset = lineLayoutCache.offsetForPosition(
        lineIndex = lineIndex,
        xPx = (position.x - contentStartPaddingPx).coerceAtLeast(0f),
        clampToLineEnd = true,
    ) ?: 0
    return layoutSnapshot.positionToOffset(lineIndex, lineOffset)
}

internal fun resolveSelectionWordRange(
    text: String,
    rawOffset: Int,
): TextRange {
    if (text.isEmpty()) return TextRange.Zero

    val safeOffset = rawOffset.coerceIn(0, text.length)
    val anchorIndex = when {
        safeOffset >= text.length -> text.lastIndex
        text[safeOffset].isSelectionWordChar() || !text[safeOffset].isWhitespace() -> safeOffset
        safeOffset > 0 && text[safeOffset - 1].isSelectionWordChar() -> safeOffset - 1
        safeOffset > 0 && !text[safeOffset - 1].isWhitespace() -> safeOffset - 1
        else -> safeOffset
    }

    if (anchorIndex !in text.indices) {
        return TextRange(safeOffset)
    }

    val anchorChar = text[anchorIndex]
    if (anchorChar == '\n' || anchorChar == '\r' || anchorChar.isWhitespace()) {
        return TextRange(safeOffset)
    }

    val predicate: (Char) -> Boolean = when {
        anchorChar.isSelectionWordChar() -> { char -> char.isSelectionWordChar() }
        else -> { char -> !char.isWhitespace() && char != '\n' && char != '\r' && !char.isSelectionWordChar() }
    }

    var start = anchorIndex
    while (start > 0 && predicate(text[start - 1])) {
        start -= 1
    }

    var end = anchorIndex + 1
    while (end < text.length && predicate(text[end])) {
        end += 1
    }

    return TextRange(start, end)
}

internal fun resolveSelectionLineRange(
    layoutSnapshot: CodeLayoutSnapshot,
    rawOffset: Int,
): TextRange {
    if (layoutSnapshot.text.isEmpty()) return TextRange.Zero

    val position = layoutSnapshot.offsetToPosition(rawOffset)
    val lineIndex = position.lineIndex
    val start = layoutSnapshot.positionToOffset(lineIndex, 0)
    val end = if (lineIndex < layoutSnapshot.lineCount - 1) {
        layoutSnapshot.lineAt(lineIndex + 1).startOffset
    } else {
        layoutSnapshot.positionToOffset(lineIndex, layoutSnapshot.lineLength(lineIndex))
    }

    return TextRange(start, end)
}

internal fun resolveDesktopClickSelection(
    layoutSnapshot: CodeLayoutSnapshot,
    clickCount: Int,
    anchorOffset: Int,
): TextRange {
    return when (clickCount) {
        2 -> resolveSelectionWordRange(
            text = layoutSnapshot.text,
            rawOffset = anchorOffset,
        )

        3 -> resolveSelectionLineRange(
            layoutSnapshot = layoutSnapshot,
            rawOffset = anchorOffset,
        )

        else -> TextRange(anchorOffset)
    }
}

internal fun resolveDesktopMultiClickCount(
    clickUptimeMillis: Long,
    clickPosition: Offset,
    previousClickUptimeMillis: Long,
    previousClickPosition: Offset,
    previousClickCount: Int,
    multiClickTimeoutMillis: Long,
    multiClickSlopPx: Float,
): Int {
    if (previousClickUptimeMillis == Long.MIN_VALUE || previousClickPosition == Offset.Unspecified) {
        return 1
    }

    val elapsedMillis = clickUptimeMillis - previousClickUptimeMillis
    if (elapsedMillis < 0L || elapsedMillis > multiClickTimeoutMillis) {
        return 1
    }

    val maxDistanceSquared = multiClickSlopPx.pow(2)
    val distanceSquared = (clickPosition.x - previousClickPosition.x).pow(2) +
        (clickPosition.y - previousClickPosition.y).pow(2)
    if (distanceSquared > maxDistanceSquared) {
        return 1
    }

    return if (previousClickCount in 1..2) previousClickCount + 1 else 1
}

private fun Char.isSelectionWordChar(): Boolean {
    return isLetterOrDigit() || this == '_' || this == '$'
}

internal data class LongPressDragSelectionState(
    val contentPosition: Offset,
    val textOffset: Int,
    val selection: TextRange,
)

internal fun resolveLongPressDragSelection(
    initialSelection: TextRange,
    draggedTextOffset: Int,
): TextRange {
    val safeDraggedTextOffset = draggedTextOffset.coerceAtLeast(0)
    if (initialSelection.collapsed) {
        val anchorOffset = initialSelection.start.coerceAtLeast(0)
        return TextRange(anchorOffset, safeDraggedTextOffset)
    }

    val normalizedStart = initialSelection.normalizedStart
    val normalizedEnd = initialSelection.normalizedEnd
    // Preserve the active edge in TextRange.end so later caret-based behaviors can distinguish
    // forward and backward selections instead of seeing only a normalized range.
    return when {
        safeDraggedTextOffset < normalizedStart -> TextRange(normalizedEnd, safeDraggedTextOffset)
        safeDraggedTextOffset > normalizedEnd -> TextRange(normalizedStart, safeDraggedTextOffset)
        else -> TextRange(normalizedStart, normalizedEnd)
    }
}

internal enum class SelectionTapAction {
    KeepSelection,
    CollapseSelection,
}

internal fun resolveSelectionTapAction(
    selection: TextRange,
    tappedOffset: Int,
    isSoftwareKeyboardVisible: Boolean,
): SelectionTapAction {
    if (selection.collapsed) return SelectionTapAction.CollapseSelection
    val normalizedStart = selection.normalizedStart
    val normalizedEnd = selection.normalizedEnd
    if (tappedOffset !in normalizedStart..normalizedEnd) {
        return SelectionTapAction.CollapseSelection
    }
    return if (isSoftwareKeyboardVisible) {
        SelectionTapAction.CollapseSelection
    } else {
        SelectionTapAction.KeepSelection
    }
}
