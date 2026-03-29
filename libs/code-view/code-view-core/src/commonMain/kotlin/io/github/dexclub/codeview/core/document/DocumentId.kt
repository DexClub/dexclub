package io.github.dexclub.codeview.core.document

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
@JvmInline
public value class DocumentId internal constructor(public val value: String) {
    init {
        require(value.isNotBlank()) { "documentId 不能为空" }
    }


    override fun toString(): String = value


    internal companion object {
        internal fun fromRaw(value: String): DocumentId = DocumentId(value)
    }
}
