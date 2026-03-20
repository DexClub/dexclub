package io.github.dexclub.codeview.compose.internal.layout

import io.github.dexclub.codeview.core.text.TextOffsetRange
import io.github.dexclub.codeview.core.token.CodeTokenKind

internal data class CodeLineTokenSpan(
    val range: TextOffsetRange,
    val kind: CodeTokenKind,
    val startColumn: Int,
    val endColumn: Int,
)
