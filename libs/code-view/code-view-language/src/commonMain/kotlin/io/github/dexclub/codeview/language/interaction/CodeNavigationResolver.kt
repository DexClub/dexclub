package io.github.dexclub.codeview.language.interaction

import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.language.annotation.CodeDecodedAnnotation

@CodeViewApi
public sealed interface CodeNavigationRequest

@CodeViewApi
public interface CodeNavigationResolver {
    public fun canResolve(decoded: CodeDecodedAnnotation): Boolean

    public fun resolve(
        hit: CodeAnnotationHit,
        decoded: CodeDecodedAnnotation,
    ): CodeNavigationRequest?
}
