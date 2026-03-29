package io.github.dexclub.codeview.runtime.reconcile

import io.github.dexclub.codeview.core.api.InternalCodeViewApi
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.core.document.DocumentRevision

@InternalCodeViewApi
internal class CapabilityResultReconciler {
    private var sequenceCounter = 0L
    private val lastAccepted = mutableMapOf<DocumentId, AcceptedRequest>()

    fun nextSequence(): Long = ++sequenceCounter

    fun shouldAccept(
        documentId: DocumentId,
        revision: DocumentRevision,
        requestId: Long,
    ): Boolean {
        val last = lastAccepted[documentId]
        
        if (last == null || requestId > last.requestId) {
            lastAccepted[documentId] = AcceptedRequest(revision, requestId)
            return true
        }
        
        return false
    }

    private data class AcceptedRequest(
        val revision: DocumentRevision,
        val requestId: Long,
    )
}
