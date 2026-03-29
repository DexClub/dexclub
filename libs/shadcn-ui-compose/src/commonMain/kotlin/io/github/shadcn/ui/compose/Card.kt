package io.github.shadcn.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.foundation.shadowMedium

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedShape,
    elevation: Dp = Elevation,
    borderWidth: Dp = BorderWidth,
    contentPadding: PaddingValues = PaddingValues(ContentPadding),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shadowMedium(elevation, shape)
            .border(borderWidth, ShadcnTheme.colors.border, shape)
            .background(ShadcnTheme.colors.card, shape)
            .padding(contentPadding)
    ) {
        content()
    }
}

private val RoundedShape = RoundedCornerShape(12.dp)
private val Elevation = 2.dp
private val BorderWidth = 1.dp
private val ContentPadding = 24.dp