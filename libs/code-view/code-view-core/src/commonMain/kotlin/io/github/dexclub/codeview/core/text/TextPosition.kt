package io.github.dexclub.codeview.core.text

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public data class TextPosition(
    public val lineIndex: Int,
    public val columnIndex: Int,
) : Comparable<TextPosition> {
    init {
        require(lineIndex >= 0) { "lineIndex 不能为负数: $lineIndex" }
        require(columnIndex >= 0) { "columnIndex 不能为负数: $columnIndex" }
    }


    public override fun compareTo(other: TextPosition): Int {
        val lineComparison = lineIndex.compareTo(other.lineIndex)
        if (lineComparison != 0) {
            return lineComparison
        }
        return columnIndex.compareTo(other.columnIndex)
    }
}
