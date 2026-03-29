package io.github.dexclub.codeview.core.transaction

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.text.TextOffsetRange

@CodeViewApi
public data class TextEdit(
    public val range: TextOffsetRange,
    public val newText: String,
)
