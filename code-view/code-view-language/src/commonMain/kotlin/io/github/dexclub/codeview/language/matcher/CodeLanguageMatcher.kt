package io.github.dexclub.codeview.language.matcher

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.text.CodeTextValue

@CodeViewApi
public fun interface CodeLanguageMatcher {
    public fun matches(value: CodeTextValue): Boolean
}
