package io.github.dexclub.codeview.runtime.session

import io.github.dexclub.codeview.core.api.InternalCodeViewApi
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.language.addon.CodeAddons
import io.github.dexclub.codeview.language.session.CodeLanguageSession
import io.github.dexclub.codeview.runtime.resolver.LanguageResolver

@InternalCodeViewApi
internal class DefaultLanguageSessionHost : LanguageSessionHost {
    private val sessions = mutableMapOf<DocumentId, CodeLanguageSession>()
    private val resolver = LanguageResolver()

    override suspend fun getOrCreateSession(
        document: CodeDocument,
        addons: CodeAddons,
    ): CodeLanguageSession? {
        return sessions.getOrPut(document.documentId) {
            val language = resolver.resolve(document, addons) ?: return null
            language.providerFactory.create(document)
        }
    }

    override fun invalidateSession(documentId: DocumentId) {
        closeSession(documentId)
    }

    override fun releaseSession(documentId: DocumentId) {
        invalidateSession(documentId)
    }

    override fun releaseAll() {
        sessions.keys.toList().forEach(::closeSession)
        sessions.clear()
    }

    private fun closeSession(documentId: DocumentId) {
        sessions.remove(documentId)?.close()
    }
}
