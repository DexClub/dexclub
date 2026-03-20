package io.github.dexclub.codeview.core.presentation

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public data class CodeTextStyle(
    public val colorArgb: Long? = null,
    public val bold: Boolean = false,
    public val italic: Boolean = false,
    public val underline: Boolean = false,
)
