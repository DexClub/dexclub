package io.github.dexclub.codeview.compose.internal.layout

internal data class CodeLineLayout(
    val lineIndex: Int,
    val startOffset: Int,
    val endOffsetExclusive: Int,
    val content: String,
) {
    val length: Int
        get() = endOffsetExclusive - startOffset
}
