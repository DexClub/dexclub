package io.github.dexclub.codeview.core.text

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.language.CodeLanguageId

@CodeViewApi
public data class CodeTextValue(
    public val text: String,
    public val language: CodeLanguageId? = null,
    public val sourceName: String? = null,
)
