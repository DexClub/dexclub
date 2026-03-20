package io.github.dexclub.codeview.core.text

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public data class Cursor(
    public val line: Int,
    public val offset: Int,
) {
    init {
        require(line >= 0) { "line 不能为负数: $line" }
        require(offset >= 0) { "offset 不能为负数: $offset" }
    }
}
