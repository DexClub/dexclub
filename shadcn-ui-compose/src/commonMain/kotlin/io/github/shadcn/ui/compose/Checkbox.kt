package io.github.shadcn.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.floor
import kotlin.math.max

@Composable
fun Checkbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean = true,
) {
    TriStateCheckbox(
        modifier = modifier,
        state = ToggleableState(checked),
        onClick =
            if (onCheckedChange != null) {
                { onCheckedChange(!checked) }
            } else {
                null
            },
        enabled = enabled,
    )
}

@Composable
fun Checkbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean = true,
    label: @Composable (() -> Unit),
) {
    val contentColor = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
    Row(
        modifier = modifier.clickable(
            enabled = enabled,
            interactionSource = null,
            indication = null,
            role = Role.Checkbox,
            onClick = { if (onCheckedChange != null) onCheckedChange(!checked) },
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            state = ToggleableState(checked),
            onClick =
                if (onCheckedChange != null) {
                    { onCheckedChange(!checked) }
                } else {
                    null
                },
            enabled = enabled,
            modifier = Modifier.padding(end = LabelPadding)
        )
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            label()
        }
    }
}

@Composable
private fun TriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val toggleableModifier =
        if (onClick != null) {
            Modifier.triStateToggleable(
                state = state,
                onClick = onClick,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = null
            )
        } else {
            Modifier
        }
    CheckboxImpl(
        enabled = enabled,
        value = state,
        modifier = modifier.then(toggleableModifier),
    )
}

@Composable
private fun CheckboxImpl(
    enabled: Boolean,
    value: ToggleableState,
    modifier: Modifier,
) {
    val transition = updateTransition(value)
    val checkDrawFraction =
        transition.animateFloat(
            transitionSpec = {
                when {
                    initialState == ToggleableState.Off -> tween(CheckAnimationDuration)
                    targetState == ToggleableState.Off -> snap(BoxOutDuration)
                    else -> spring()
                }
            }
        ) {
            when (it) {
                ToggleableState.On -> 1f
                ToggleableState.Off -> 0f
                ToggleableState.Indeterminate -> 1f
            }
        }

    val checkCenterGravitationShiftFraction =
        transition.animateFloat(
            transitionSpec = {
                when {
                    initialState == ToggleableState.Off -> snap()
                    targetState == ToggleableState.Off -> snap(BoxOutDuration)
                    else -> tween(durationMillis = CheckAnimationDuration)
                }
            }
        ) {
            when (it) {
                ToggleableState.On -> 0f
                ToggleableState.Off -> 0f
                ToggleableState.Indeterminate -> 1f
            }
        }
    val checkCache = remember { CheckDrawingCache() }
    val checkColor = checkmarkColor(enabled, value)
    val boxColor = boxColor(enabled, value)
    val borderColor = borderColor(enabled, value)
    Canvas(
        modifier
            .wrapContentSize(Alignment.Center)
            .requiredSize(CheckboxSize)
    ) {
        val strokeWidthPx = floor(StrokeWidth.toPx())
        drawBox(
            boxColor = boxColor.value,
            borderColor = borderColor.value,
            radius = RadiusSize.toPx(),
            strokeWidth = strokeWidthPx
        )
        drawCheck(
            checkColor = checkColor.value,
            checkFraction = checkDrawFraction.value,
            crossCenterGravitation = checkCenterGravitationShiftFraction.value,
            strokeWidthPx = strokeWidthPx,
            drawingCache = checkCache
        )
    }
}

@Composable
internal fun checkmarkColor(enabled: Boolean, state: ToggleableState): State<Color> {
    val target =
        if (enabled) {
            when (state) {
                ToggleableState.On -> ShadcnTheme.colors.primaryForeground
                ToggleableState.Indeterminate -> ShadcnTheme.colors.primaryForeground
                ToggleableState.Off -> ShadcnTheme.colors.background
            }
        } else {
            when (state) {
                ToggleableState.On -> ShadcnTheme.colors.border
                ToggleableState.Indeterminate -> ShadcnTheme.colors.border
                ToggleableState.Off -> ShadcnTheme.colors.border
            }
        }

    // If not enabled 'snap' to the disabled state, as there should be no animations between
    // enabled / disabled.
    return if (enabled) {
        val duration = if (state == ToggleableState.Off) BoxOutDuration else BoxInDuration
        animateColorAsState(target, tween(durationMillis = duration))
    } else {
        rememberUpdatedState(target)
    }
}

@Composable
internal fun boxColor(enabled: Boolean, state: ToggleableState): State<Color> {
    val target =
        if (enabled) {
            when (state) {
                ToggleableState.On -> ShadcnTheme.colors.primary
                ToggleableState.Indeterminate -> ShadcnTheme.colors.primary
                ToggleableState.Off -> ShadcnTheme.colors.background
            }
        } else {
            when (state) {
                ToggleableState.On -> ShadcnTheme.colors.secondary
                ToggleableState.Indeterminate -> ShadcnTheme.colors.secondary
                ToggleableState.Off -> ShadcnTheme.colors.secondary
            }
        }

    // If not enabled 'snap' to the disabled state, as there should be no animations between
    // enabled / disabled.
    return if (enabled) {
        val duration = if (state == ToggleableState.Off) BoxOutDuration else BoxInDuration
        animateColorAsState(target, tween(durationMillis = duration))
    } else {
        rememberUpdatedState(target)
    }
}

@Composable
internal fun borderColor(enabled: Boolean, state: ToggleableState): State<Color> {
    val target =
        if (enabled) {
            when (state) {
                ToggleableState.On -> ShadcnTheme.colors.primary
                ToggleableState.Indeterminate -> ShadcnTheme.colors.primary
                ToggleableState.Off -> ShadcnTheme.colors.border
            }
        } else {
            when (state) {
                ToggleableState.Indeterminate -> ShadcnTheme.colors.secondary
                ToggleableState.On -> ShadcnTheme.colors.secondary
                ToggleableState.Off -> ShadcnTheme.colors.secondary
            }
        }

    // If not enabled 'snap' to the disabled state, as there should be no animations between
    // enabled / disabled.
    return if (enabled) {
        val duration = if (state == ToggleableState.Off) BoxOutDuration else BoxInDuration
        animateColorAsState(target, tween(durationMillis = duration))
    } else {
        rememberUpdatedState(target)
    }
}

private fun DrawScope.drawBox(
    boxColor: Color,
    borderColor: Color,
    radius: Float,
    strokeWidth: Float,
) {
    val halfStrokeWidth = strokeWidth / 2.0f
    val stroke = Stroke(strokeWidth)
    val checkboxSize = size.width
    if (boxColor == borderColor) {
        drawRoundRect(
            boxColor,
            size = Size(checkboxSize, checkboxSize),
            cornerRadius = CornerRadius(radius),
            style = Fill
        )
    } else {
        drawRoundRect(
            boxColor,
            topLeft = Offset(strokeWidth, strokeWidth),
            size = Size(checkboxSize - strokeWidth * 2, checkboxSize - strokeWidth * 2),
            cornerRadius = CornerRadius(max(0f, radius - strokeWidth)),
            style = Fill
        )
        drawRoundRect(
            borderColor,
            topLeft = Offset(halfStrokeWidth, halfStrokeWidth),
            size = Size(checkboxSize - strokeWidth, checkboxSize - strokeWidth),
            cornerRadius = CornerRadius(radius - halfStrokeWidth),
            style = stroke
        )
    }
}

private fun DrawScope.drawCheck(
    checkColor: Color,
    checkFraction: Float,
    crossCenterGravitation: Float,
    strokeWidthPx: Float,
    drawingCache: CheckDrawingCache,
) {
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Square)
    val width = size.width
    val checkCrossX = 0.4f
    val checkCrossY = 0.7f
    val leftX = 0.2f
    val leftY = 0.5f
    val rightX = 0.8f
    val rightY = 0.3f

    val gravitatedCrossX = lerp(checkCrossX, 0.5f, crossCenterGravitation)
    val gravitatedCrossY = lerp(checkCrossY, 0.5f, crossCenterGravitation)
    // gravitate only Y for end to achieve center line
    val gravitatedLeftY = lerp(leftY, 0.5f, crossCenterGravitation)
    val gravitatedRightY = lerp(rightY, 0.5f, crossCenterGravitation)

    with(drawingCache) {
        checkPath.reset()
        checkPath.moveTo(width * leftX, width * gravitatedLeftY)
        checkPath.lineTo(width * gravitatedCrossX, width * gravitatedCrossY)
        checkPath.lineTo(width * rightX, width * gravitatedRightY)
        // TODO: replace with proper declarative non-android alternative when ready (b/158188351)
        pathMeasure.setPath(checkPath, false)
        pathToDraw.reset()
        pathMeasure.getSegment(0f, pathMeasure.length * checkFraction, pathToDraw, true)
    }
    drawPath(drawingCache.pathToDraw, checkColor, style = stroke)
}

@Immutable
private class CheckDrawingCache(
    val checkPath: Path = Path(),
    val pathMeasure: PathMeasure = PathMeasure(),
    val pathToDraw: Path = Path(),
)

private const val BoxInDuration = 50
private const val BoxOutDuration = 100
private const val CheckAnimationDuration = 100

private val CheckboxSize = 16.dp
private val StrokeWidth = 1.dp
private val RadiusSize = 4.dp
private val LabelPadding = 8.dp