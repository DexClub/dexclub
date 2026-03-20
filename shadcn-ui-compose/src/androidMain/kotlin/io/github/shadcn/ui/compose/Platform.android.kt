package io.github.shadcn.ui.compose

import android.content.ClipData
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.unit.Dp

actual suspend fun Clipboard.copyText(text: CharSequence, label: CharSequence) {
    val entry = ClipEntry(ClipData.newPlainText(label, text))
    setClipEntry(entry)
}

actual fun Modifier.shadcnShadow(
    color: Color,
    borderRadius: Dp,
    blurRadius: Dp,
    offsetY: Dp,
    spread: Dp,
): Modifier = shadow(
    elevation = blurRadius / 2,
    shape = RoundedCornerShape(borderRadius),
    ambientColor = color,
    spotColor = color,
)