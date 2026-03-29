package io.github.shadcn.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.foundation.rememberShadcnIndication

enum class SelectFieldColor {
    Normal,
    Success,
    Error
}

data class SelectFieldColors(
    val backgroundColor: Color,
    val textColor: Color,
    val borderColor: Color,
    val ringColor: Color,
)

@Composable
fun defaultSelectFieldColors(
    color: SelectFieldColor = SelectFieldColor.Normal,
): SelectFieldColors {
    val theme = ShadcnTheme.colors
    return when (color) {
        SelectFieldColor.Normal -> SelectFieldColors(
            backgroundColor = theme.background,
            textColor = theme.foreground,
            borderColor = theme.border,
            ringColor = Color.Transparent
        )

        SelectFieldColor.Success -> SelectFieldColors(
            backgroundColor = theme.background,
            textColor = theme.foreground,
            borderColor = Color(0x22, 0x16, 0x34),
            ringColor = Color.Transparent
        )

        SelectFieldColor.Error -> SelectFieldColors(
            backgroundColor = theme.background,
            textColor = theme.foreground,
            borderColor = Color(0xEF, 0x44, 0x44),
            ringColor = Color(0xEF, 0x44, 0x44).copy(alpha = 0.2f)
        )
    }
}

@Composable
fun SelectField(
    modifier: Modifier = Modifier,
    colors: SelectFieldColors = defaultSelectFieldColors(color = SelectFieldColor.Normal),
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(6.dp),
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 27.dp)
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberShadcnIndication(),
                enabled = enabled,
                onClick = onClick,
            )
            .shadcnRing(
                ringColor = colors.ringColor,
                ringWidth = 3.dp,
                shape = shape
            )
            .background(colors.backgroundColor, shape)
            .border(1.dp, colors.borderColor, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}

private fun Modifier.shadcnRing(
    ringColor: Color,
    shape: Shape,
    ringWidth: Dp,
): Modifier = this.drawWithCache {
    onDrawBehind {
        if (ringColor != Color.Transparent) {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                color = ringColor,
                style = Stroke(width = ringWidth.toPx() * 2f)
            )
        }
    }
}