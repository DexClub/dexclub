package io.github.dexclub.app.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.foundation.rememberShadcnIndication

@Composable
fun EIconButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    indicationColor: Color = ShadcnTheme.colors.mutedForeground.copy(alpha = 0.4f),
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .clickable(
                indication = rememberShadcnIndication(indicationColor),
                interactionSource = remember { MutableInteractionSource() },
                enabled = enabled,
                onClick = onClick,
            )
            .padding(contentPadding)
    ) {
        content()
    }
}