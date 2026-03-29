package io.github.shadcn.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    Popup(
        onDismissRequest = onDismissRequest,
        popupPositionProvider = remember(posPx) { ContextMenuPositionProvider(posPx) },
        properties = PopupProperties(focusable = true),
    ) {
        MenuContent(items = items, onDismissAll = onDismissRequest, width = width)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MenuContent(
    items: List<ContextMenuItem>,
    onDismissAll: () -> Unit,
    width: Dp,
    onEnter: (() -> Unit)? = null,
) {
    var openSubmenuIndex by remember { mutableStateOf(-1) }
    val scope = rememberCoroutineScope()
    var closeJob by remember { mutableStateOf<Job?>(null) }

    Column(
        modifier = Modifier
            .width(width)
            .heightIn(max = PopupContentMaxHeight)
            .shadcnShadow()
            .clip(MenuRoundedCornerShape)
            .background(ShadcnTheme.colors.popover)
            .border(MenuBorderWidth, ShadcnTheme.colors.border, MenuRoundedCornerShape)
            .padding(MenuPadding)
            .verticalScroll(rememberScrollState())
            .onPointerEvent(PointerEventType.Enter) {
                closeJob?.cancel()
                onEnter?.invoke()
            }
            .onPointerEvent(PointerEventType.Exit) {
                closeJob = scope.launch {
                    delay(150)
                    openSubmenuIndex = -1
                }
            },
    ) {
        items.forEachIndexed { index, item ->
            when (item) {
                is ContextMenuItem.Item -> MenuItem(
                    item = item,
                    onDismissAll = onDismissAll,
                    width = width,
                    submenuOpen = item.children.isNotEmpty() && openSubmenuIndex == index,
                    onHover = { openSubmenuIndex = index },
                    onSubmenuEnter = { closeJob?.cancel() },
                )

                is ContextMenuItem.Label -> MenuLabel(item.text)
                ContextMenuItem.Separator -> MenuSeparator()
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MenuItem(
    item: ContextMenuItem.Item,
    onDismissAll: () -> Unit,
    width: Dp,
    submenuOpen: Boolean,
    onHover: () -> Unit,
    onSubmenuEnter: () -> Unit,
) {
    val hasChildren = item.children.isNotEmpty()
    Box(modifier = Modifier.onPointerEvent(PointerEventType.Enter) { onHover() }) {
        MenuItemRow(
            item = item,
            modifier = Modifier.fillMaxWidth(),
            showChevron = hasChildren,
            onClick = {
                if (!hasChildren) {
                    item.onClick?.invoke(); onDismissAll()
                } else onHover()
            },
        )
        if (submenuOpen) {
            val density = LocalDensity.current
            val subMenuProvider = remember(density) { SubMenuPositionProvider(with(density) { 4.dp.roundToPx() }) }
            Popup(
                popupPositionProvider = subMenuProvider,
                properties = PopupProperties(focusable = false),
            ) {
                MenuContent(
                    items = item.children,
                    onDismissAll = onDismissAll,
                    width = width,
                    onEnter = onSubmenuEnter,
                )
            }
        }
    }
}

private class ContextMenuPositionProvider(private val localPos: IntOffset) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect, windowSize: IntSize,
        layoutDirection: LayoutDirection, popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.left + localPos.x)
            .coerceAtMost(windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val y = (anchorBounds.top + localPos.y)
            .coerceAtMost(windowSize.height - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

private class SubMenuPositionProvider(private val offsetPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect, windowSize: IntSize,
        layoutDirection: LayoutDirection, popupContentSize: IntSize,
    ): IntOffset {
        val x = if (anchorBounds.right + popupContentSize.width + offsetPx <= windowSize.width)
            anchorBounds.right + offsetPx
        else
            anchorBounds.left - popupContentSize.width - offsetPx
        val y = anchorBounds.top.coerceAtMost(windowSize.height - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}
