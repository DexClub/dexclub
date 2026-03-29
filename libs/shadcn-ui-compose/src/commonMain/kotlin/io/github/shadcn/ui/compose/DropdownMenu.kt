package io.github.shadcn.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.shadcn.ui.compose.foundation.ShadcnPopupPositionProvider
import io.github.shadcn.ui.compose.foundation.rememberShadcnIndication

enum class DropdownMenuAlignment {
    Start,
    End,
}

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    popupPadding: PaddingValues = PaddingValues.Zero,
    width: Dp = 192.dp,
    alignment: DropdownMenuAlignment = DropdownMenuAlignment.Start,
    trigger: @Composable BoxScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier) {
        trigger()

        if (expanded) {
            val density = LocalDensity.current
            val offsetPx = with(density) { 4.dp.roundToPx() }
            val popupPositionProvider = remember(offsetPx, alignment) {
                ShadcnPopupPositionProvider(
                    offsetPx = offsetPx,
                    alignment = alignment,
                )
            }

            Popup(
                onDismissRequest = onDismissRequest,
                popupPositionProvider = popupPositionProvider,
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    modifier = Modifier
                        .padding(popupPadding)
                        .width(width)
                        .heightIn(max = PopupContentMaxHeight)
                        .shadcnShadow()
                        .background(ShadcnTheme.colors.popover, MenuRoundedCornerShape)
                        .border(MenuBorderWidth, ShadcnTheme.colors.border, MenuRoundedCornerShape)
                        .clip(MenuRoundedCornerShape)
                        .padding(MenuPadding)
                        .verticalScroll(rememberScrollState()),
                    content = content
                )
            }
        }
    }
}

@Composable
fun DropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    dangerous: Boolean = false,
) {
    val colors = ShadcnTheme.colors
    val enabledContentColor = if (dangerous) colors.destructive else colors.popoverForeground
    val disabledContentColor = colors.mutedForeground
    val textColor = if (enabled) enabledContentColor else disabledContentColor
    val iconColor = if (enabled) enabledContentColor else disabledContentColor
    val trailingColor = if (enabled) iconColor.copy(alpha = 0.6f) else disabledContentColor
    val hoverColor = if (dangerous) colors.destructive.copy(alpha = 0.1f) else colors.accent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MenuItemRoundedCornerShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberShadcnIndication(hoverColor),
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (leading != null) {
                CompositionLocalProvider(LocalContentColor provides iconColor) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(16.dp)
                    ) {
                        leading()
                    }
                }
            }

            Text(
                text = text,
                style = textStyle.copy(color = textColor)
            )
        }

        if (trailing != null) {
            CompositionLocalProvider(LocalContentColor provides trailingColor) {
                Box(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    trailing()
                }
            }
        }
    }
}

@Composable
fun DropdownMenuLabel(
    text: String,
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        style = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, // font-semibold
            color = ShadcnTheme.colors.foreground
        )
    )
}

@Composable
fun DropdownMenuSeparator() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        thickness = 1.dp,
        color = ShadcnTheme.colors.border
    )
}
