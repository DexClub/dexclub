package io.github.dexclub.codeview.core.transaction

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.document.DocumentRevision

@CodeViewApi
public data class DocumentTransaction(
    public val transactionId: String,
    public val baseRevision: DocumentRevision,
    public val changes: DocumentChangeSet,
) {
    init {
        require(transactionId.isNotBlank()) { "transactionId 不能为空" }
    }
}
