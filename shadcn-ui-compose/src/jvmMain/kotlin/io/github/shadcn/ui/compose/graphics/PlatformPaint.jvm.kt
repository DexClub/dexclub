package io.github.shadcn.ui.compose.graphics

import androidx.compose.ui.graphics.Paint
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter

actual fun Paint.setPlatformBlur(radius: Float) {
    if (radius <= 0f) return
    val frameworkPaint = this.asFrameworkPaint()
    frameworkPaint.maskFilter = MaskFilter.makeBlur(
        FilterBlurMode.NORMAL,
        radius,
        false
    )
}