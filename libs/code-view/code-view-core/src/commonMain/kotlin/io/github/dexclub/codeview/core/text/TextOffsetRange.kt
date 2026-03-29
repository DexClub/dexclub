package io.github.dexclub.codeview.core.text

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public data class TextOffsetRange(
    public val start: Int,
    public val end: Int,
) {
    init {
        require(start >= 0) { "range.start 不能为负数: $start" }
        require(end >= start) { "range 必须满足 [start, end) 语义: start=$start, end=$end" }
    }


    public val isCollapsed: Boolean
        get() = start == end


    public fun contains(offset: Int): Boolean = offset in start until end


    public companion object {
        public fun collapsed(offset: Int): TextOffsetRange {
            require(offset >= 0) { "offset 不能为负数: $offset" }
            return TextOffsetRange(start = offset, end = offset)
        }
    }
}
