package io.github.dexclub.codeview.core.document

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.core.text.TextOffsetRange

@CodeViewApi
public data class CodeDocumentSnapshot(
    public val documentId: DocumentId,
    public val revision: DocumentRevision,
    public val text: String,
    public val languageId: CodeLanguageId,
) {
    public val textLength: Int
        get() = text.length


    public fun substring(range: TextOffsetRange): String {
        require(range.end <= text.length) {
            "range 超出文本边界: range=$range, textLength=${text.length}"
        }
        return text.substring(range.start, range.end)
    }
}
