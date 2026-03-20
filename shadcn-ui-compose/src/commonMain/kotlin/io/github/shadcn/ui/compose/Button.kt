package io.github.shadcn.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.foundation.rememberShadcnIndication

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = ShadcnTheme.shapes.medium,
    colors: ButtonColors = ShadcnButtonDefaults.primaryColors(),
    borderWidth: Dp = 0.dp,
    elevation: Dp = 0.dp,
    contentPadding: PaddingValues = ShadcnButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
    val borderColor = if (enabled) colors.borderColor else colors.disabledBorderColor
    val accentColor = ShadcnTheme.colors.accent.copy(0.3f)

    val mergedStyle = LocalTextStyle.current.merge(
        ShadcnTheme.textStyles.labelMedium.copy(
            color = contentColor,
            textAlign = TextAlign.Center
        )
    )

    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides mergedStyle
    ) {
        Box(
            modifier = modifier
                .semantics { role = Role.Button }
                .shadow(elevation, shape)
                .clip(shape)
                .background(containerColor)
                .then(
                    if (borderWidth > 0.dp) {
                        Modifier.border(width = borderWidth, color = borderColor, shape = shape)
                    } else {
                        Modifier
                    }
                )
                .combinedClickable(
                    interactionSource = actualInteractionSource,
                    indication = rememberShadcnIndication(accentColor),
                    enabled = enabled,
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(
                        minWidth = ShadcnButtonDefaults.MinWidth,
                        minHeight = ShadcnButtonDefaults.MinHeight
                    )
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        enabled = enabled,
        colors = ShadcnButtonDefaults.secondaryColors(),
        content = content
    )
}

@Composable
fun DestructiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        enabled = enabled,
        colors = ShadcnButtonDefaults.destructiveColors(),
        content = content
    )
}

@Composable
fun OutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        enabled = enabled,
        colors = ShadcnButtonDefaults.outlineColors(),
        borderWidth = 1.dp,
        content = content
    )
}

@Composable
fun GhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        enabled = enabled,
        colors = ShadcnButtonDefaults.ghostColors(),
        content = content
    )
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ShadcnButtonDefaults.ghostColors(),
    borderWidth: Dp = 0.dp,
    icon: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        onLongClick = onLongClick,
        enabled = enabled,
        colors = colors,
        contentPadding = PaddingValues(0.dp),
        borderWidth = borderWidth,
        elevation = 0.dp
    ) {
        icon()
    }
}

@Composable
fun RoundedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ShadcnButtonDefaults.outlineColors(),
    borderWidth: Dp = 1.dp,
    icon: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        onLongClick = onLongClick,
        enabled = enabled,
        colors = colors,
        contentPadding = PaddingValues(0.dp),
        borderWidth = borderWidth,
        elevation = 0.dp,
        shape = CircleShape,
    ) {
        icon()
    }
}

@Composable
fun LoadingButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ShadcnButtonDefaults.primaryColors(),
    borderWidth: Dp = 0.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        enabled = enabled && !isLoading, // 加载中禁用点击
        colors = colors,
        borderWidth = borderWidth,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current // 自动适配当前按钮文字颜色
            )
            Spacer(Modifier.width(8.dp))
        }
        content()
    }
}

@Immutable
data class ButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val borderColor: Color = Color.Transparent,
    val disabledBorderColor: Color = Color.Transparent,
)

object ShadcnButtonDefaults {
    val MinWidth = 48.dp
    val MinHeight = 24.dp
    val ContentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)

    @Composable
    @ReadOnlyComposable
    fun primaryColors() = ButtonColors(
        containerColor = ShadcnTheme.colors.primary,
        contentColor = ShadcnTheme.colors.primaryForeground,
        disabledContainerColor = ShadcnTheme.colors.primary.copy(alpha = 0.5f),
        disabledContentColor = ShadcnTheme.colors.primaryForeground.copy(alpha = 0.38f)
    )

    @Composable
    @ReadOnlyComposable
    fun secondaryColors() = ButtonColors(
        containerColor = ShadcnTheme.colors.secondary,
        contentColor = ShadcnTheme.colors.secondaryForeground,
        disabledContainerColor = ShadcnTheme.colors.secondary.copy(alpha = 0.5f),
        disabledContentColor = ShadcnTheme.colors.secondaryForeground.copy(alpha = 0.38f)
    )

    @Composable
    @ReadOnlyComposable
    fun destructiveColors() = ButtonColors(
        containerColor = ShadcnTheme.colors.destructive,
        contentColor = Color.White,
        disabledContainerColor = ShadcnTheme.colors.destructive.copy(alpha = 0.5f),
        disabledContentColor = Color.White.copy(alpha = 0.38f)
    )

    @Composable
    @ReadOnlyComposable
    fun outlineColors() = ButtonColors(
        containerColor = ShadcnTheme.colors.background,
        contentColor = ShadcnTheme.colors.accentForeground,
        disabledContainerColor = ShadcnTheme.colors.background.copy(alpha = 0.5f),
        disabledContentColor = ShadcnTheme.colors.accentForeground.copy(alpha = 0.38f),
        borderColor = ShadcnTheme.colors.border,
        disabledBorderColor = ShadcnTheme.colors.border.copy(alpha = 0.5f)
    )

    @Composable
    @ReadOnlyComposable
    fun ghostColors() = ButtonColors(
        containerColor = ShadcnTheme.colors.background,
        contentColor = ShadcnTheme.colors.accentForeground,
        disabledContainerColor = ShadcnTheme.colors.background.copy(alpha = 0.5f),
        disabledContentColor = ShadcnTheme.colors.accentForeground.copy(alpha = 0.38f),
    )
}