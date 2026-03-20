package io.github.dexclub.codeview.core.document

import io.github.dexclub.codeview.core.api.InternalCodeViewApi
import io.github.dexclub.codeview.core.language.CodeLanguageId

@InternalCodeViewApi
public object CodeDocumentTestingFactory {
    public fun create(
        text: String,
        languageId: CodeLanguageId,
        documentId: String = "test-document",
    ): CodeDocument = CodeDocument(
        documentId = DocumentId.fromRaw(documentId),
        languageId = languageId,
        initialText = text,
    )
}
