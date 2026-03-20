package io.github.dexclub.codeview.core.text

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public data class CodeSelection(
    public val anchorOffset: Int,
    public val caretOffset: Int,
) {
    init {
        require(anchorOffset >= 0) { "anchorOffset 不能为负数: $anchorOffset" }
        require(caretOffset >= 0) { "caretOffset 不能为负数: $caretOffset" }
    }


    public val range: TextOffsetRange
        get() = TextOffsetRange(
            start = minOf(anchorOffset, caretOffset),
            end = maxOf(anchorOffset, caretOffset),
        )


    public val isCollapsed: Boolean
        get() = anchorOffset == caretOffset


    public val isReversed: Boolean
        get() = caretOffset < anchorOffset


    public companion object {
        public fun collapsed(offset: Int): CodeSelection = CodeSelection(
            anchorOffset = offset,
            caretOffset = offset,
        )
    }
}
