package io.github.dexclub.codeview.language.interaction

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public data class CodeContextAction(
    public val label: String,
    public val action: () -> Unit,
    public val icon: String? = null,
    public val enabled: Boolean = true,
    public val shortcut: String? = null,
) {
    init {
        require(label.isNotBlank()) { "label 不能为空" }
    }
}
