package io.github.dexclub.codeview.core.transaction

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public data class DocumentChangeSet(
    public val edits: List<TextEdit>,
) {
    init {
        require(edits.isNotEmpty()) { "changes.edits 不能为空" }
    }
}
