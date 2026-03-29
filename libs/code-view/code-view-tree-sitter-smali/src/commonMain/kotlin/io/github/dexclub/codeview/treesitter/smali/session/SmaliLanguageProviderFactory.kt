package io.github.dexclub.codeview.treesitter.smali.session

import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.language.session.CodeLanguageProviderFactory
import io.github.dexclub.codeview.language.session.CodeLanguageSession

public object SmaliLanguageProviderFactory : CodeLanguageProviderFactory {
    override fun create(document: CodeDocument): CodeLanguageSession {
        return SmaliLanguageSession(document)
    }
}
