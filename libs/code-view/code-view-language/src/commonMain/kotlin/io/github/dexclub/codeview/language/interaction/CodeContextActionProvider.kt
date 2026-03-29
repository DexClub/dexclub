package io.github.dexclub.codeview.language.interaction

import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.language.annotation.CodeDecodedAnnotation

@CodeViewApi
public interface CodeContextActionProvider {
    public fun canProvide(decoded: CodeDecodedAnnotation): Boolean

    public fun provide(
        hit: CodeAnnotationHit,
        decoded: CodeDecodedAnnotation,
    ): List<CodeContextAction>
}
