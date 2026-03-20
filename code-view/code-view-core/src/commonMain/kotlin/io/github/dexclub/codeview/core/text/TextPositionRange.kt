package io.github.dexclub.codeview.core.text

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public data class TextPositionRange(
    public val start: TextPosition,
    public val end: TextPosition,
) {
    init {
        require(end >= start) {
            "position range 必须满足 [start, end) 语义: start=$start, end=$end"
        }
    }


    public val isCollapsed: Boolean
        get() = start == end


    public companion object {
        public fun collapsed(position: TextPosition): TextPositionRange = TextPositionRange(
            start = position,
            end = position,
        )
    }
}
