package io.github.dexclub.codeview.runtime

import io.github.dexclub.codeview.core.api.InternalCodeViewApi
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.language.addon.CodeAddons
import io.github.dexclub.codeview.runtime.session.LanguageSessionHost
import io.github.dexclub.codeview.runtime.session.DefaultLanguageSessionHost
import io.github.dexclub.codeview.runtime.surface.CodeSurfaceController
import io.github.dexclub.codeview.runtime.surface.DefaultCodeSurfaceController

@InternalCodeViewApi
internal class DefaultCodeRuntime : CodeRuntime {
    private val sessionHost: LanguageSessionHost = DefaultLanguageSessionHost()
    private val controllers = mutableMapOf<DocumentId, DefaultCodeSurfaceController>()

    override fun getSurfaceController(
        document: CodeDocument,
        addons: CodeAddons,
    ): CodeSurfaceController {
        return controllers.getOrPut(document.documentId) {
            DefaultCodeSurfaceController(document, addons, sessionHost)
        }
    }

    override fun releaseDocument(documentId: DocumentId) {
        controllers.remove(documentId)?.close()
        sessionHost.releaseSession(documentId)
    }

    override fun close() {
        controllers.values.forEach { it.close() }
        controllers.clear()
        sessionHost.releaseAll()
    }
}
