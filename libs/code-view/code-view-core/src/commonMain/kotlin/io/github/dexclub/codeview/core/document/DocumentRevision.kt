package io.github.dexclub.codeview.core.document

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
@JvmInline
public value class DocumentRevision(public val value: Long) : Comparable<DocumentRevision> {
    init {
        require(value >= 0L) { "revision 不能为负数: $value" }
    }


    public override fun compareTo(other: DocumentRevision): Int = value.compareTo(other.value)


    public operator fun plus(delta: Long): DocumentRevision {
        require(delta >= 0L) { "revision 增量不能为负数: $delta" }
        return DocumentRevision(value + delta)
    }
}
