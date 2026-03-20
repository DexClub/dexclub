package io.github.dexclub.codeview.compose.internal.interaction

import androidx.compose.ui.geometry.Offset
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot

internal fun resolveTextOffsetForPosition(
    layoutSnapshot: CodeLayoutSnapshot,
    position: Offset,
    charWidthPx: Float,
    lineHeightPx: Float,
    clampToLineEnd: Boolean,
): Int? {
    if (lineHeightPx <= 0f || charWidthPx <= 0f) return null
    if (position.x < 0f || position.y < 0f) return null

    val contentHeightPx = layoutSnapshot.lineCount * lineHeightPx
    if (position.y >= contentHeightPx) return null

    val lineIndex = (position.y / lineHeightPx).toInt().coerceIn(0, layoutSnapshot.lineCount - 1)
    val line = layoutSnapshot.lineAt(lineIndex)
    val lineWidthPx = line.length * charWidthPx

    if (!clampToLineEnd && position.x > lineWidthPx) {
        return null
    }

    val column = (position.x / charWidthPx).toInt().coerceIn(0, line.length)
    return layoutSnapshot.positionToOffset(lineIndex, column)
}
