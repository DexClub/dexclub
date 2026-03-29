package io.github.dexclub.codeview.core.presentation

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.token.CodeTokenKind

@CodeViewApi
public interface CodeTheme {
    public fun styleFor(tokenKind: CodeTokenKind): CodeTextStyle
}
