package io.github.dexclub.codeview.language.session

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.document.CodeDocumentSnapshot
import io.github.dexclub.codeview.core.token.CodeTokenSpan

@CodeViewApi
public interface CodeLanguageSession {
    public suspend fun highlightTokens(snapshot: CodeDocumentSnapshot): List<CodeTokenSpan>

    public suspend fun annotations(snapshot: CodeDocumentSnapshot): List<CodeAnnotation> = emptyList()

    public fun close()
}
