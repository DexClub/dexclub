package io.github.dexclub.codeview.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.PlatformSpanStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
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
        color = Color(0xFF1F2328),
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
}

/**
 * CodeViewer 的交互选项，控制节点点击行为。
 */
@CodeViewApi
public data class CodeViewerInteractionOptions(
    /** 用于识别可交互注解的 tag，对应 AnnotatedString 中的 annotation tag。 */
    public val annotationTag: String = "NODE_ID",
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
