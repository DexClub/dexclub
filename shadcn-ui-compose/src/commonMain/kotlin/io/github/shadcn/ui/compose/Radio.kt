package io.github.shadcn.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun RadioButton(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: (() -> Unit)?,
    enabled: Boolean = true,
) {
    RadioButtonImpl(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
fun RadioButton(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: (() -> Unit)?,
    enabled: Boolean = true,
    label: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.clickable(
            enabled = enabled,
            onClick = { onClick?.invoke() },
            role = Role.RadioButton,
            interactionSource = null,
            indication = null
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButtonImpl(
            modifier = Modifier.padding(end = Space),
            selected = selected,
            onClick = onClick,
            enabled = enabled,
        )
        label()
    }
}

@Composable
private fun RadioButtonImpl(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: (() -> Unit)?,
    enabled: Boolean = true,
) {
    val dotRadius = animateDpAsState(
        targetValue = if (selected) RadioButtonDotSize / 2 else 0.dp,
        animationSpec = tween(durationMillis = RadioAnimationDuration)
    )
    val boxColor = boxColor(enabled, selected)
    val radioColor = radioColor(enabled, selected)
    val strokeColor = strokeColor(enabled, selected)
    val selectableModifier =
        if (onClick != null) {
            Modifier.selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = null,
                indication = null
            )
        } else {
            Modifier
        }
    Canvas(
        modifier
            .then(selectableModifier)
            .wrapContentSize(Alignment.Center)
            .requiredSize(IconSize)
    ) {
        // Draw the radio button
        val strokeWidth = RadioStrokeWidth.toPx()
        drawCircle(
            color = boxColor.value,
            radius = (IconSize / 2).toPx()
        )
        drawCircle(
            color = strokeColor.value,
            radius = (IconSize / 2).toPx() - strokeWidth / 2,
            style = Stroke(strokeWidth)
        )
        if (dotRadius.value > 0.dp) {
            drawCircle(
                color = radioColor.value,
                radius = dotRadius.value.toPx() - strokeWidth / 2,
                style = Fill
            )
        }
    }
}

@Composable
internal fun radioColor(enabled: Boolean, selected: Boolean): State<Color> {
    val target =
        when {
            enabled && selected -> ShadcnTheme.colors.primary
            enabled && !selected -> ShadcnTheme.colors.border
            !enabled && selected -> ShadcnTheme.colors.border
            else -> ShadcnTheme.colors.secondary // Disabled and not selected
        }

    // If not enabled 'snap' to the disabled state, as there should be no animations between
    // enabled / disabled.
    return if (enabled) {
        animateColorAsState(target, tween(durationMillis = RadioAnimationDuration))
    } else {
        rememberUpdatedState(target)
    }
}

@Composable
internal fun boxColor(enabled: Boolean, selected: Boolean): State<Color> {
    val target =
        when {
            enabled && selected -> ShadcnTheme.colors.background
            enabled && !selected -> ShadcnTheme.colors.background
            !enabled && selected -> ShadcnTheme.colors.secondary
            else -> ShadcnTheme.colors.secondary // Disabled and not selected
        }

    // If not enabled 'snap' to the disabled state, as there should be no animations between
    // enabled / disabled.
    return if (enabled) {
        animateColorAsState(target, tween(durationMillis = RadioAnimationDuration))
    } else {
        rememberUpdatedState(target)
    }
}

@Composable
internal fun strokeColor(enabled: Boolean, selected: Boolean): State<Color> {
    val target =
        when {
            enabled && selected -> ShadcnTheme.colors.border
            enabled && !selected -> ShadcnTheme.colors.border
            !enabled && selected -> ShadcnTheme.colors.border
            else -> ShadcnTheme.colors.border // Disabled and not selected
        }

    // If not enabled 'snap' to the disabled state, as there should be no animations between
    // enabled / disabled.
    return if (enabled) {
        animateColorAsState(target, tween(durationMillis = RadioAnimationDuration))
    } else {
        rememberUpdatedState(target)
    }
}

private const val RadioAnimationDuration = 100

private val IconSize = 16.0.dp
private val Space = 8.dp

private val RadioButtonDotSize = 8.dp
private val RadioStrokeWidth = 1.dp