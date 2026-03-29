package io.github.shadcn.ui.compose.ext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.foundation.rememberShadcnIndication

fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
) = this.composed {
    this.clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick
    )
}

fun Modifier.shadcnClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    indicationColor: Color? = null,
    onClick: () -> Unit,
) = this.composed {
    this.clickable(
        indication = rememberShadcnIndication(indicationColor ?: ShadcnTheme.colors.accent),
        interactionSource = remember { MutableInteractionSource() },
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick
    )
}