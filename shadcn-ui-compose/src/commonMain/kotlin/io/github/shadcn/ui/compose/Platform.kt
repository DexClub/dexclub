package io.github.shadcn.ui.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

expect suspend fun Clipboard.copyText(text: CharSequence, label: CharSequence = "copy")

expect fun Modifier.shadcnShadow(
    color: Color = Color.Black.copy(alpha = 0.1f),
    borderRadius: Dp = 6.dp,
    blurRadius: Dp = 6.dp,
    offsetY: Dp = 4.dp,
    spread: Dp = (-1).dp,
): Modifier
