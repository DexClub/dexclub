package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.compose.EIconButton
import io.github.dexclub.app.model.OPEN_TAB_TARGET_TYPE_CLASS
import io.github.dexclub.app.model.OpenTabMode
import io.github.dexclub.app.model.OpenTabUiModel
import io.github.dexclub.app.res.IconRes
import io.github.dexclub.app.res.resolveClassIcon
import io.github.dexclub.app.res.resolveMixedClassIcon
import io.github.dexclub.app.res.icons.SmaliClass
import io.github.dexclub.utils.SignatureUtils
import io.github.shadcn.ui.compose.DropdownMenu
import io.github.shadcn.ui.compose.DropdownMenuAlignment
import io.github.shadcn.ui.compose.DropdownMenuItem
import io.github.shadcn.ui.compose.DropdownMenuSeparator
import io.github.shadcn.ui.compose.Icon
import io.github.shadcn.ui.compose.LocalSonner
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.copyText
import io.github.shadcn.ui.compose.ext.shadcnClickable
import io.github.shadcn.ui.compose.icons.Close
import io.github.shadcn.ui.compose.icons.Icons
import kotlinx.coroutines.launch

@Composable
internal fun TabBarItem(
    itemState: WorkspaceTabBarItemUiState,
    onToggle: (OpenTabUiModel) -> Unit,
    onClose: (OpenTabUiModel) -> Unit,
    onCloseAll: () -> Unit,
    onCloseOthers: (OpenTabUiModel) -> Unit,
    onCloseTabsToRight: (OpenTabUiModel) -> Unit,
    onCloseTabsToLeft: (OpenTabUiModel) -> Unit,
    onToggleViewType: (OpenTabUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tab = itemState.tab
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val sonnerState = LocalSonner.current
    var contextMenuExpanded by remember(tab.tabId) { mutableStateOf(false) }
    val menuTextStyle = ShadcnTheme.textStyles.bodySmall
    val canCopyClassInfo = tab.targetType == OPEN_TAB_TARGET_TYPE_CLASS
    val classNameToCopy = tab.targetKey
    val classSignatureToCopy = SignatureUtils.typeSignature(classNameToCopy)
    val icon = resolveTabIcon(tab)

    fun openContextMenu() {
        if (!itemState.isSelected) {
            onToggle(tab)
        }
        contextMenuExpanded = true
    }

    fun dismissThen(action: () -> Unit): () -> Unit {
        return {
            contextMenuExpanded = false
            action()
        }
    }

    fun copyValue(
        text: String,
        label: String,
        successPrefix: String,
    ): () -> Unit {
        return dismissThen {
            scope.launch {
                clipboard.copyText(text, label = label)
                sonnerState.sonner("$successPrefix: $text")
            }
        }
    }

    DropdownMenu(
        expanded = contextMenuExpanded,
        onDismissRequest = { contextMenuExpanded = false },
        alignment = DropdownMenuAlignment.End,
        trigger = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .padding(end = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (itemState.isSelected) Modifier.background(ShadcnTheme.colors.mutedForeground.copy(0.2f))
                        else Modifier
                    )
                    .tabContextMenuTrigger(onOpenContextMenu = ::openContextMenu)
                    .shadcnClickable(indicationColor = ShadcnTheme.colors.mutedForeground.copy(0.2f)) {
                        onToggle(tab)
                    }
                    .padding(start = 4.dp, end = 6.dp),
            ) {
                EIconButton(
                    contentPadding = PaddingValues.Zero,
                    onClick = { onToggleViewType(tab) },
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = null,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .widthIn(min = 90.dp, max = 240.dp),
                ) {
                    Text(
                        text = tab.title,
                        maxLines = 1,
                        style = ShadcnTheme.textStyles.labelMedium,
                    )
                }

                EIconButton(
                    contentPadding = PaddingValues(2.dp),
                    onClick = { onClose(tab) },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Filled.Close,
                        contentDescription = null,
                        tint = null,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        },
    ) {
        DropdownMenuItem(
            text = "关闭",
            textStyle = menuTextStyle,
            onClick = dismissThen { onClose(tab) },
        )
        if (itemState.hasOtherTabs) {
            DropdownMenuItem(
                text = "关闭其它",
                textStyle = menuTextStyle,
                onClick = dismissThen { onCloseOthers(tab) },
            )
        }
        if (itemState.hasOtherTabs) {
            DropdownMenuItem(
                text = "关闭全部",
                textStyle = menuTextStyle,
                onClick = dismissThen(onCloseAll),
            )
        }
        if (itemState.canCloseRightTabs) {
            DropdownMenuItem(
                text = "关闭右侧",
                textStyle = menuTextStyle,
                onClick = dismissThen { onCloseTabsToRight(tab) },
            )
        }
        if (itemState.canCloseLeftTabs) {
            DropdownMenuItem(
                text = "关闭左侧",
                textStyle = menuTextStyle,
                onClick = dismissThen { onCloseTabsToLeft(tab) },
            )
        }
        if (canCopyClassInfo) {
            DropdownMenuSeparator()
            DropdownMenuItem(
                text = "复制类名",
                textStyle = menuTextStyle,
                onClick = copyValue(
                    text = classNameToCopy,
                    label = "class_name",
                    successPrefix = "已复制类名",
                ),
            )
            DropdownMenuItem(
                text = "复制类签名",
                textStyle = menuTextStyle,
                onClick = copyValue(
                    text = classSignatureToCopy,
                    label = "class_signature",
                    successPrefix = "已复制类签名",
                ),
            )
        }
    }
}

private fun resolveTabIcon(
    tab: OpenTabUiModel,
) = when {
    tab.targetType != OPEN_TAB_TARGET_TYPE_CLASS -> when (tab.mode) {
        OpenTabMode.SMALI -> IconRes.SmaliClass
        OpenTabMode.JAVA -> resolveClassIcon(null)
        OpenTabMode.MIXED -> resolveMixedClassIcon(null)
    }

    tab.mode == OpenTabMode.SMALI -> IconRes.SmaliClass
    tab.mode == OpenTabMode.MIXED -> resolveMixedClassIcon(tab.classVisualKind)
    else -> resolveClassIcon(tab.classVisualKind)
}

private fun Modifier.tabContextMenuTrigger(
    onOpenContextMenu: () -> Unit,
): Modifier {
    return this
        .pointerInput(onOpenContextMenu) {
            detectTapGestures(
                onLongPress = { onOpenContextMenu() },
            )
        }
        .pointerInput(onOpenContextMenu) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        onOpenContextMenu()
                    }
                }
            }
        }
}
