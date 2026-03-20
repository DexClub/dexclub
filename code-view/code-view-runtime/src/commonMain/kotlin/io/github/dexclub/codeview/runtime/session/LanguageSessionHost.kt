package io.github.dexclub.codeview.runtime.session

import io.github.dexclub.codeview.core.api.InternalCodeViewApi
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.language.addon.CodeAddons
import io.github.dexclub.codeview.language.session.CodeLanguageSession

@InternalCodeViewApi
internal interface LanguageSessionHost {
    suspend fun getOrCreateSession(
        document: CodeDocument,
        addons: CodeAddons,
    ): CodeLanguageSession?

    fun releaseSession(documentId: DocumentId)

    fun releaseAll()
}
