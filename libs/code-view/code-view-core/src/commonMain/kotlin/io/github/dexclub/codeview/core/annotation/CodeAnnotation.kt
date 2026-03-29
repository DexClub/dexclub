package io.github.dexclub.codeview.core.annotation

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.text.TextOffsetRange

@CodeViewApi
public data class CodeAnnotation(
    public val range: TextOffsetRange,
    public val kind: String,
    public val schemaId: String,
    public val schemaVersion: Int,
    public val payload: String,
) {
    init {
        require(kind.isNotBlank()) { "annotation.kind 不能为空" }
        require(schemaId.isNotBlank()) { "annotation.schemaId 不能为空" }
        require(schemaVersion >= 0) { "annotation.schemaVersion 不能为负数: $schemaVersion" }
    }
}
