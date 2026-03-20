package io.github.dexclub.codeview.core.transaction

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.document.DocumentRevision

@CodeViewApi
public data class DocumentTransactionFeedback(
    public val transactionId: String,
    public val acceptedRevision: DocumentRevision?,
    public val rejectedReason: String?,
) {
    init {
        require(transactionId.isNotBlank()) { "transactionId 不能为空" }
    }
}
