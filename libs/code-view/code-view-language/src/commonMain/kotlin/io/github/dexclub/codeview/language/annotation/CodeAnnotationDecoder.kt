package io.github.dexclub.codeview.language.annotation

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public interface CodeDecodedAnnotation

@CodeViewApi
public sealed interface CodeAnnotationDecodeResult {
    @CodeViewApi
    public data class Success(
        public val value: CodeDecodedAnnotation,
    ) : CodeAnnotationDecodeResult

    @CodeViewApi
    public data class Failure(
        public val reason: String,
    ) : CodeAnnotationDecodeResult {
        init {
            require(reason.isNotBlank()) { "reason 不能为空" }
        }
    }
}

@CodeViewApi
public interface CodeAnnotationDecoder {
    public val schemaId: String

    public fun decode(annotation: CodeAnnotation): CodeAnnotationDecodeResult
}
