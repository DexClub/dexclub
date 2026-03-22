package io.github.dexclub.codeview.compose.internal.viewer

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import kotlin.math.max

internal data class CodeViewerTextMetrics(
    val charWidthPx: Float,
    val profile: CodeViewerTextMetricsProfile,
)

internal fun measureCodeViewerTextMetrics(
    textMeasurer: TextMeasurer,
    density: Density,
    textStyle: TextStyle,
): CodeViewerTextMetrics {
    val charWidthPx = measureAverageCharacterWidthPx(
        textMeasurer = textMeasurer,
        textStyle = textStyle,
    )
    val latinMetrics = measureSingleLineTextMetrics(
        textMeasurer = textMeasurer,
        textStyle = textStyle,
        sampleText = "M",
    )
    val cjkMetrics = measureSingleLineTextMetrics(
        textMeasurer = textMeasurer,
        textStyle = textStyle,
        sampleText = CJK_LINE_HEIGHT_SAMPLE_CHAR.toString(),
    )
    val digitMetrics = measureSingleLineTextMetrics(
        textMeasurer = textMeasurer,
        textStyle = textStyle,
        sampleText = "0",
    )
    val punctuationMetrics = measureSingleLineTextMetrics(
        textMeasurer = textMeasurer,
        textStyle = textStyle,
        sampleText = "_",
    )
    val maxAscentPx = max(
        max(latinMetrics.baselinePx, cjkMetrics.baselinePx),
        max(digitMetrics.baselinePx, punctuationMetrics.baselinePx),
    ).coerceAtLeast(0f)
    val maxDescentPx = max(
        max(
            latinMetrics.heightPx - latinMetrics.baselinePx,
            cjkMetrics.heightPx - cjkMetrics.baselinePx,
        ),
        max(
            digitMetrics.heightPx - digitMetrics.baselinePx,
            punctuationMetrics.heightPx - punctuationMetrics.baselinePx,
        ),
    ).coerceAtLeast(0f)
    val contentHeightPx = (maxAscentPx + maxDescentPx).coerceAtLeast(1f)
    val lineHeightPx = max(
        measureAverageLineHeightPx(
            textMeasurer = textMeasurer,
            density = density,
            textStyle = textStyle,
        ),
        contentHeightPx,
    ).coerceAtLeast(1f)
    val contentTopPaddingPx = ((lineHeightPx - contentHeightPx) / 2f).coerceAtLeast(0f)
    return CodeViewerTextMetrics(
        charWidthPx = charWidthPx,
        profile = CodeViewerTextMetricsProfile(
            lineHeightPx = lineHeightPx,
            contentHeightPx = contentHeightPx,
            contentTopPaddingPx = contentTopPaddingPx,
            baselinePx = (contentTopPaddingPx + maxAscentPx).coerceIn(0f, lineHeightPx),
        ),
    )
}

private fun measureAverageCharacterWidthPx(
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
): Float {
    val sampleText = "M".repeat(TEXT_METRICS_CHARACTER_SAMPLE_COUNT)
    val measuredWidthPx = textMeasurer.measure(
        text = AnnotatedString(sampleText),
        style = textStyle,
    ).size.width.toFloat()
    return (measuredWidthPx / TEXT_METRICS_CHARACTER_SAMPLE_COUNT).coerceAtLeast(1f)
}

private fun measureAverageLineHeightPx(
    textMeasurer: TextMeasurer,
    density: Density,
    textStyle: TextStyle,
): Float {
    val latinSampleText = buildString {
        repeat(TEXT_METRICS_LINE_SAMPLE_COUNT) { index ->
            if (index > 0) append('\n')
            append('M')
        }
    }
    val cjkSampleText = buildString {
        repeat(TEXT_METRICS_LINE_SAMPLE_COUNT) { index ->
            if (index > 0) append('\n')
            append(CJK_LINE_HEIGHT_SAMPLE_CHAR)
        }
    }
    val latinMeasuredHeightPx = textMeasurer.measure(
        text = AnnotatedString(latinSampleText),
        style = textStyle,
    ).size.height.toFloat()
    val cjkMeasuredHeightPx = textMeasurer.measure(
        text = AnnotatedString(cjkSampleText),
        style = textStyle,
    ).size.height.toFloat()
    val averageMeasuredLineHeightPx = max(
        latinMeasuredHeightPx / TEXT_METRICS_LINE_SAMPLE_COUNT,
        cjkMeasuredHeightPx / TEXT_METRICS_LINE_SAMPLE_COUNT,
    )
    val fallbackLineHeightPx = with(density) {
        textStyle.lineHeight.toPx()
    }
    return max(averageMeasuredLineHeightPx, fallbackLineHeightPx).coerceAtLeast(1f)
}

private fun measureSingleLineTextMetrics(
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
    sampleText: String,
): SingleLineTextMetrics {
    val layout = textMeasurer.measure(
        text = AnnotatedString(sampleText),
        style = textStyle,
        softWrap = false,
    )
    return SingleLineTextMetrics(
        heightPx = layout.size.height.toFloat(),
        baselinePx = layout.getLineBaseline(0),
    )
}

private data class SingleLineTextMetrics(
    val heightPx: Float,
    val baselinePx: Float,
)

private const val TEXT_METRICS_CHARACTER_SAMPLE_COUNT: Int = 64
private const val TEXT_METRICS_LINE_SAMPLE_COUNT: Int = 32
private const val CJK_LINE_HEIGHT_SAMPLE_CHAR: Char = '国'
