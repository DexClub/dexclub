package io.github.dexclub.codeview.language.session

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.document.CodeDocument

@CodeViewApi
public interface CodeLanguageProviderFactory {
    public fun create(document: CodeDocument): CodeLanguageSession
}
