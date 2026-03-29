package io.github.dexclub.codeview.compose.internal.viewer

import androidx.compose.ui.text.TextLayoutResult

internal data class CodeViewerTextMetricsProfile(
    val lineHeightPx: Float,
    val contentHeightPx: Float,
    val contentTopPaddingPx: Float,
    val baselinePx: Float,
)

internal fun resolveLineContentTopPx(
    lineTop: Float,
    contentTopPaddingPx: Float,
): Float {
    return lineTop + contentTopPaddingPx
}

internal fun resolveLineContentBottomPx(
    lineTop: Float,
    contentTopPaddingPx: Float,
    contentHeightPx: Float,
): Float {
    return resolveLineContentTopPx(
        lineTop = lineTop,
        contentTopPaddingPx = contentTopPaddingPx,
    ) + contentHeightPx
}

internal fun resolveSegmentTextTopPx(
    lineTop: Float,
    baselinePx: Float,
    layout: TextLayoutResult,
): Float {
    return lineTop + (baselinePx - layout.getLineBaseline(0)).coerceAtLeast(0f)
}
