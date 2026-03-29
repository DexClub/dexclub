package io.github.dexclub.codeview.core.diagnostic

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public data class CodeDiagnostic(
    public val level: Level,
    public val code: String,
    public val message: String,
) {
    init {
        require(code.isNotBlank()) { "diagnostic.code 不能为空" }
        require(message.isNotBlank()) { "diagnostic.message 不能为空" }
    }


    @CodeViewApi
    public enum class Level {
        Info,
        Warning,
        Error,
    }
}
