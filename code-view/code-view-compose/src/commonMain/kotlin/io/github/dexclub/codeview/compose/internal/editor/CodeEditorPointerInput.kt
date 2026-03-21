package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache

internal fun Modifier.codeEditorPointerInput(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    lineHeightPx: Float,
    onFieldValueChange: (TextFieldValue) -> Unit,
    requestContentFocus: () -> Unit,
    requestImeFocus: () -> Unit,
    onInterruptInputAnchor: () -> Unit,
    onAnyPointerEditing: () -> Unit,
): Modifier {
    return pointerInput(layoutSnapshot.text, lineHeightPx) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            requestContentFocus()
            requestImeFocus()
            onInterruptInputAnchor()
            onAnyPointerEditing()

            val anchorOffset = resolveEditorTextOffset(
                layoutSnapshot = layoutSnapshot,
                lineLayoutCache = lineLayoutCache,
                lineHeightPx = lineHeightPx,
                position = down.position,
            )
            onFieldValueChange(
                TextFieldValue(
                    text = layoutSnapshot.text,
                    selection = TextRange(anchorOffset),
                )
            )

            drag(down.id) { change ->
                val targetOffset = resolveEditorTextOffset(
                    layoutSnapshot = layoutSnapshot,
                    lineLayoutCache = lineLayoutCache,
                    lineHeightPx = lineHeightPx,
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
        }
    }
}

internal fun resolveEditorTextOffset(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    lineHeightPx: Float,
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
        xPx = position.x,
        clampToLineEnd = true,
    ) ?: 0
    return layoutSnapshot.positionToOffset(lineIndex, lineOffset)
}
