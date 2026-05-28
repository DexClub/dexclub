package io.github.dexclub.codeview.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.PlatformSpanStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.codeview.core.text.LineSelection

@CodeViewApi
public object CodeViewDefaults {
    /**
     * Number of extra blank lines reserved below the last document line.
     * Values less than or equal to zero disable the reserve completely.
     */
    public const val ScrollPastEnd: Int = 5

    public val CodeTextStyle: TextStyle = TextStyle(
        color = Color(0xFF080808),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily.Monospace,
        platformStyle = PlatformTextStyle(
            spanStyle = PlatformSpanStyle.Default,
            paragraphStyle = PlatformParagraphStyle.Default,
        ),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    public val LineNumberOptions: CodeLineNumberOptions = CodeLineNumberOptions()

    public val GutterOptions: CodeGutterOptions = CodeGutterOptions()

    public val ContentOptions: CodeContentOptions = CodeContentOptions()

    public val DecorationOptions: CodeDecorationOptions = CodeDecorationOptions()
}

/**
 * CodeViewer 的交互选项，控制节点点击行为。
 */
@CodeViewApi
public data class CodeViewerInteractionOptions(
    /** 用于识别可交互注解的 tag，对应 AnnotatedString 中的 annotation tag。 */
    public val annotationTag: String = "NODE_ID",
)

@CodeViewApi
public data class CodeGutterOptions(
    public val visible: Boolean = true,
    public val backgroundColor: Color = Color(0xFFFFFFFF),
    public val dividerColor: Color = Color(0x1FD0D0D0),
    public val lineNumbers: CodeLineNumberOptions = CodeLineNumberOptions(),
)

@CodeViewApi
public data class CodeLineNumberOptions(
    public val visible: Boolean = true,
    public val minDigits: Int = 2,
    public val startPadding: Dp = 12.dp,
    public val endPadding: Dp = 12.dp,
    public val textColor: Color = Color(0xFFAEB3C2),
    public val activeTextColor: Color = Color(0xFF767A8A),
) {
    init {
        require(minDigits >= 1) { "minDigits 必须大于等于 1: $minDigits" }
        require(startPadding.value >= 0f) { "startPadding 不能为负数: $startPadding" }
        require(endPadding.value >= 0f) { "endPadding 不能为负数: $endPadding" }
    }
}

@CodeViewApi
public data class CodeContentOptions(
    public val startPadding: Dp = 2.dp,
    public val endPadding: Dp = 0.dp,
) {
    init {
        require(startPadding.value >= 0f) { "startPadding 不能为负数: $startPadding" }
        require(endPadding.value >= 0f) { "endPadding 不能为负数: $endPadding" }
    }
}

@CodeViewApi
public data class CodeDecorationOptions(
    public val selectionColor: Color = Color(0x334096FF),
    public val searchHighlightColor: Color = Color(0x40F4D03F),
    public val inactiveSearchHighlightColor: Color = Color(0x20F4D03F),
    public val cursorColor: Color = Color(0xFF000000),
)

/**
 * CodeViewer 的光标定位请求，用于将视口滚动到指定行。
 */
@CodeViewApi
public data class CodeViewerCursorTarget(
    public val line: Int,
    public val offset: Int,
    /** 用于去重的 token，相同 token 不重复触发滚动。 */
    public val token: Long,
)
