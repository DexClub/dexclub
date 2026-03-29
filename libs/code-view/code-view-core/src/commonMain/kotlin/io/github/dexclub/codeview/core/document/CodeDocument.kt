package io.github.dexclub.codeview.core.document

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.language.CodeLanguageId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@CodeViewApi
public class CodeDocument internal constructor(
    public val documentId: DocumentId,
    public val languageId: CodeLanguageId,
    initialText: String,
) {
    private val _snapshots = MutableStateFlow(
        CodeDocumentSnapshot(
            documentId = documentId,
            revision = DocumentRevision(0L),
            text = initialText,
            languageId = languageId,
        ),
    )

    public val snapshots: StateFlow<CodeDocumentSnapshot> = _snapshots.asStateFlow()


    public fun update(newText: String): Unit {
        val current = _snapshots.value
        _snapshots.value = current.copy(
            revision = current.revision + 1L,
            text = newText,
        )
    }

    public companion object {
        private var documentCounter = 0L

        public fun create(
            languageId: CodeLanguageId,
            initialText: String,
        ): CodeDocument {
            val id = DocumentId("doc-${++documentCounter}")
            return CodeDocument(id, languageId, initialText)
        }
    }
}
