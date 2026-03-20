package io.github.shadcn.ui.compose

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.unit.Dp
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.copyText(text: CharSequence, label: CharSequence) {
    val entry = ClipEntry(StringSelection(text.toString()))
    setClipEntry(entry)
}

actual fun Modifier.shadcnShadow(
    color: Color,
    borderRadius: Dp,
    blurRadius: Dp,
    offsetY: Dp,
    spread: Dp,
): Modifier = drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        paint.asFrameworkPaint().maskFilter =
            MaskFilter.makeBlur(FilterBlurMode.NORMAL, blurRadius.toPx())
        paint.color = color
        val s = spread.toPx()
        canvas.drawRoundRect(
            left = -s, top = offsetY.toPx() - s,
            right = size.width + s, bottom = size.height + s,
            radiusX = borderRadius.toPx(), radiusY = borderRadius.toPx(),
            paint = paint,
        )
    }
}