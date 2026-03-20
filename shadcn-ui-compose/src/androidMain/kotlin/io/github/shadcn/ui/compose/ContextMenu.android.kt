package io.github.shadcn.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.shadcn.ui.compose.foundation.rememberShadcnIndication
import io.github.shadcn.ui.compose.icons.Icons
import io.github.shadcn.ui.compose.icons.MoreHoriz

@Composable
actual fun ContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    width: Dp,
    position: Offset,
    items: List<ContextMenuItem>,
) {
    if (!expanded) return
    val posPx = IntOffset(position.x.toInt(), position.y.toInt())
    val actionItems = remember(items) {
        items.flatMap { item ->
            if (item is ContextMenuItem.Item && item.children.isNotEmpty())
                item.children.filterIsInstance<ContextMenuItem.Item>()
            else listOfNotNull(item as? ContextMenuItem.Item)
        }
    }
    Popup(
        onDismissRequest = onDismissRequest,
        popupPositionProvider = remember(posPx) { SelectionToolbarPositionProvider(posPx) },
        properties = PopupProperties(focusable = true),
    ) {
        SelectionToolbar(actionItems, onDismissRequest)
    }
}

@Composable
private fun SelectionToolbar(
    actionItems: List<ContextMenuItem.Item>,
    onDismissAll: () -> Unit,
) {
    val colors = ShadcnTheme.colors
    var overflowExpanded by remember { mutableStateOf(false) }
    var overflowItems by remember { mutableStateOf(emptyList<ContextMenuItem.Item>()) }

    SubcomposeLayout(
        modifier = Modifier
            .padding(horizontal = HorizontalPadding)
            .height(40.dp)
            .shadcnShadow()
            .clip(MenuRoundedCornerShape)
            .background(colors.popover)
            .border(MenuBorderWidth, colors.border, MenuRoundedCornerShape),
    ) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val maxW = constraints.maxWidth
        val h = if (constraints.maxHeight == Constraints.Infinity) 0 else constraints.maxHeight

        val itemPlaceables = actionItems.indices.map { i ->
            subcompose("item_$i") { ToolbarItemText(actionItems[i], onDismissAll) }
                .first().measure(loose)
        }
        val divW = subcompose("divTpl") { ToolbarDivider() }.first().measure(loose).width
        val overflowP = subcompose("overflow") {
            OverflowButton(overflowExpanded, overflowItems, onDismissAll) {
                overflowExpanded = !overflowExpanded
            }
        }.first().measure(loose)

        // 计算可见项数
        val allFit = itemPlaceables.sumOf { it.width } +
                divW * (itemPlaceables.size - 1).coerceAtLeast(0) <= maxW
        var visibleCount = if (allFit) itemPlaceables.size else 0
        if (!allFit) {
            var w = 0
            for (i in itemPlaceables.indices) {
                if (w + itemPlaceables[i].width + divW + overflowP.width <= maxW) {
                    w += itemPlaceables[i].width + divW
                    visibleCount++
                } else break
            }
        }
        val needsOverflow = visibleCount < itemPlaceables.size
        overflowItems = if (needsOverflow) actionItems.drop(visibleCount) else emptyList()

        // 创建分隔线
        val divCount = (visibleCount - 1).coerceAtLeast(0) + if (needsOverflow) 1 else 0
        val divPlaceables = (0 until divCount).map { i ->
            subcompose("div_$i") { ToolbarDivider() }.first().measure(loose)
        }

        val totalW = itemPlaceables.take(visibleCount).sumOf { it.width } +
                divPlaceables.sumOf { it.width } +
                if (needsOverflow) overflowP.width else 0

        layout(totalW, h) {
            var x = 0
            var dIdx = 0
            for (i in 0 until visibleCount) {
                itemPlaceables[i].place(x, (h - itemPlaceables[i].height) / 2)
                x += itemPlaceables[i].width
                if (dIdx < divPlaceables.size) {
                    divPlaceables[dIdx].place(x, (h - divPlaceables[dIdx].height) / 2)
                    x += divPlaceables[dIdx].width
                    dIdx++
                }
            }
            if (needsOverflow) {
                overflowP.place(x, (h - overflowP.height) / 2)
            }
        }
    }
}

/** Places the toolbar above the touch point, centered horizontally. */
private class SelectionToolbarPositionProvider(private val touchPos: IntOffset) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect, windowSize: IntSize,
        layoutDirection: LayoutDirection, popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.left + touchPos.x - popupContentSize.width / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val y = (anchorBounds.top + touchPos.y - popupContentSize.height - 16)
            .coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

@Composable
private fun ToolbarItemText(item: ContextMenuItem.Item, onDismissAll: () -> Unit) {
    val colors = ShadcnTheme.colors
    val textColor = if (item.dangerous) colors.destructive else colors.popoverForeground
    Text(
        text = item.label,
        style = ShadcnTheme.textStyles.bodySmall.copy(color = textColor),
        modifier = Modifier
            .clickable(
                enabled = item.enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberShadcnIndication(colors.accent),
            ) { item.onClick?.invoke(); onDismissAll() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun ToolbarDivider() {
    VerticalDivider(Modifier.height(20.dp).width(1.dp), color = ShadcnTheme.colors.border)
}

@Composable
private fun OverflowButton(
    expanded: Boolean,
    items: List<ContextMenuItem.Item>,
    onDismissAll: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = ShadcnTheme.colors
    Box {
        Icon(
            imageVector = Icons.Rounded.Filled.MoreHoriz,
            contentDescription = "More",
            tint = colors.popoverForeground,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberShadcnIndication(colors.accent),
                    onClick = onClick,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        if (expanded && items.isNotEmpty()) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, 120),
                properties = PopupProperties(focusable = false),
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = HorizontalPadding)
                        .heightIn(max = PopupContentMaxHeight)
                        .shadcnShadow()
                        .clip(MenuRoundedCornerShape)
                        .background(colors.popover)
                        .border(MenuBorderWidth, colors.border, MenuRoundedCornerShape)
                        .padding(MenuPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    items.forEach { item ->
                        MenuItemRow(
                            item = item,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { item.onClick?.invoke(); onDismissAll() },
                        )
                    }
                }
            }
        }
    }
}

private val HorizontalPadding = 24.dp