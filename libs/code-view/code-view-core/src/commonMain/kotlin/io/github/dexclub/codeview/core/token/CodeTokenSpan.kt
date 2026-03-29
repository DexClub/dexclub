package io.github.dexclub.codeview.core.token

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.text.TextOffsetRange

@CodeViewApi
public data class CodeTokenSpan(
    public val range: TextOffsetRange,
    public val kind: CodeTokenKind,
)
