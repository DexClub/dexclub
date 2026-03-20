package io.github.dexclub.codeview.treesitter.java.session

import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.language.session.CodeLanguageProviderFactory
import io.github.dexclub.codeview.language.session.CodeLanguageSession

public object JavaLanguageProviderFactory : CodeLanguageProviderFactory {
    override fun create(document: CodeDocument): CodeLanguageSession {
        return JavaLanguageSession(document)
    }
}
