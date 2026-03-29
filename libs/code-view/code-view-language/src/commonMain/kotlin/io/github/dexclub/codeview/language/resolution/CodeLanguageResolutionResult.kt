package io.github.dexclub.codeview.language.resolution

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.language.install.CodeLanguageInstallable

@CodeViewApi
public data class CodeLanguageResolutionResult(
    public val language: CodeLanguageInstallable?,
    public val matchedBy: MatchSource,
) {
    init {
        if (language == null) {
            require(matchedBy == MatchSource.PlainTextFallback) {
                "language 为空时 matchedBy 必须是 PlainTextFallback: $matchedBy"
            }
        } else {
            require(matchedBy != MatchSource.PlainTextFallback) {
                "language 非空时 matchedBy 不能是 PlainTextFallback"
            }
        }
    }


    @CodeViewApi
    public enum class MatchSource {
        ExplicitLanguage,
        SourceName,
        Matcher,
        PlainTextFallback,
    }


    public companion object {
        public fun plainTextFallback(): CodeLanguageResolutionResult = CodeLanguageResolutionResult(
            language = null,
            matchedBy = MatchSource.PlainTextFallback,
        )
    }
}
