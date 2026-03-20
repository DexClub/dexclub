package io.github.dexclub.codeview.core.text

import io.github.dexclub.codeview.core.api.CodeViewApi

/**
 * 行级坐标选区，用于 UI 层的选区状态保存与恢复。
 *
 * 与 [CodeSelection]（全局字符偏移）不同，[LineSelection] 使用行号 + 行内偏移表示，
 * 便于与代码行列表直接对应，适合滚动位置恢复、搜索高亮等场景。
 */
@CodeViewApi
public data class LineSelection(
    public val startLine: Int,
    public val startOffset: Int,
    public val endLine: Int,
    public val endOffset: Int,
) {
    public fun normalized(): LineSelection {
        return if (startLine < endLine || (startLine == endLine && startOffset <= endOffset)) {
            this
        } else {
            LineSelection(
                startLine = endLine,
                startOffset = endOffset,
                endLine = startLine,
                endOffset = startOffset,
            )
        }
    }

    public val isCollapsed: Boolean
        get() = startLine == endLine && startOffset == endOffset

    public companion object {
        public fun collapsed(line: Int, offset: Int): LineSelection =
            LineSelection(startLine = line, startOffset = offset, endLine = line, endOffset = offset)
    }
}
