package io.github.shadcn.ui.compose.graphics

import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Paint

actual fun Paint.setPlatformBlur(radius: Float) {
    if (radius <= 0f) return
    val frameworkPaint = this.asFrameworkPaint()
    frameworkPaint.maskFilter = BlurMaskFilter(
        radius,
        BlurMaskFilter.Blur.NORMAL
    )
}