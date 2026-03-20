package io.github.shadcn.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.foundation.rememberShadcnIndication
import io.github.shadcn.ui.compose.icons.ChevronRight
import io.github.shadcn.ui.compose.icons.Icons

sealed interface ContextMenuItem {
    data class Item(
        val label: String,
        val icon: (@Composable () -> Unit)? = null,
        val shortcut: String? = null,
        val onClick: (() -> Unit)? = null,
        val children: List<ContextMenuItem> = emptyList(),
        val enabled: Boolean = true,
        val dangerous: Boolean = false,
    ) : ContextMenuItem

    data class Label(val text: String) : ContextMenuItem

    data object Separator : ContextMenuItem
}

@Composable
expect fun ContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    width: Dp = 192.dp,
    position: Offset,
    items: List<ContextMenuItem>,
)

@Composable
internal fun MenuItemRow(
    item: ContextMenuItem.Item,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = ShadcnTheme.colors
    val textColor = if (item.dangerous) colors.destructive else colors.popoverForeground
    val hoverColor = if (item.dangerous) colors.destructive.copy(alpha = 0.1f) else colors.accent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .then(if (!item.enabled) Modifier.alpha(0.5f) else Modifier)
            .clip(MenuItemRoundedCornerShape)
            .clickable(
                enabled = item.enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberShadcnIndication(hoverColor),
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        if (item.icon != null) {
            CompositionLocalProvider(LocalContentColor provides textColor) {
                Box(Modifier.padding(end = 8.dp).size(16.dp)) { item.icon() }
            }
        }
        Text(
            text = item.label,
            style = ShadcnTheme.textStyles.bodySmall.copy(color = textColor),
            modifier = Modifier.weight(1f),
        )
        if (item.shortcut != null) {
            Text(
                text = item.shortcut,
                modifier = Modifier.padding(start = 16.dp),
                style = ShadcnTheme.textStyles.bodySmall.copy(
                    color = colors.mutedForeground,
                    fontSize = ShadcnTheme.textStyles.bodySmall.fontSize * 0.9f,
                ),
            )
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Rounded.Filled.ChevronRight,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 8.dp).size(20.dp),
            )
        }
    }
}

@Composable
internal fun MenuLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        style = ShadcnTheme.textStyles.bodySmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = ShadcnTheme.colors.foreground,
        ),
    )
}

@Composable
internal fun MenuSeparator() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        thickness = 1.dp,
        color = ShadcnTheme.colors.border,
    )
}

internal val MenuRoundedCornerShape = RoundedCornerShape(10.dp)
internal val MenuItemRoundedCornerShape = RoundedCornerShape(8.dp)
internal val MenuBorderWidth = 1.dp
internal val MenuPadding = 4.dp
internal val PopupContentMaxHeight = 284.dp
