package io.github.dexclub.codeview.runtime.fallback

import io.github.dexclub.codeview.core.api.InternalCodeViewApi
import io.github.dexclub.codeview.core.document.CodeDocumentSnapshot
import io.github.dexclub.codeview.core.text.TextOffsetRange
import io.github.dexclub.codeview.core.token.CodeTokenKind
import io.github.dexclub.codeview.core.token.CodeTokenSpan

@InternalCodeViewApi
internal object PlainTextFallback {
    fun generateTokens(snapshot: CodeDocumentSnapshot): List<CodeTokenSpan> =
        if (snapshot.textLength == 0) {
            emptyList()
        } else {
            listOf(
                CodeTokenSpan(
                    range = TextOffsetRange(0, snapshot.textLength),
                    kind = CodeTokenKind.PlainText,
                )
            )
        }
}
