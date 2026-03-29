package io.github.shadcn.ui.compose.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

internal fun Modifier.shadowMedium(
    elevation: Dp,
    shape: Shape,
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = Color(0f, 0f, 0f, 0.45f),
        spotColor = Color(0f, 0f, 0f, 0.45f),
    )