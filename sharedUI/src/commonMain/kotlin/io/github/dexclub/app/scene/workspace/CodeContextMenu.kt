package io.github.dexclub.app.scene.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboard
import io.github.dexclub.core.navigation.NavigateRequestContext
import io.github.shadcn.ui.compose.ContextMenu
import io.github.shadcn.ui.compose.ContextMenuItem
import io.github.shadcn.ui.compose.copyText
import kotlinx.coroutines.launch

@Composable
internal fun CodeContextMenu(
    selectedText: String,
    onSelectAll: () -> Unit,
    expanded: Boolean,
    position: Offset,
    navigateContext: NavigateRequestContext?,
    onNavigateToDefinition: (NavigateRequestContext) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    ContextMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        position = position,
        items = buildList {
            add(
                ContextMenuItem.Item(
                    label = "复制",
                    onClick = {
                        if (selectedText.isNotEmpty()) {
                            scope.launch { clipboard.copyText(selectedText) }
                        }
                    },
                )
            )
            add(
                ContextMenuItem.Item(
                    label = "全选",
                    onClick = onSelectAll,
                )
            )
            if (navigateContext != null) {
                add(
                    ContextMenuItem.Item(
                        label = "更多操作",
                        children = listOf(
                            ContextMenuItem.Item(
                                label = "跳转定义",
                                onClick = { onNavigateToDefinition(navigateContext) },
                            ),
                            ContextMenuItem.Item(label = "查找引用", onClick = {}),
                        ),
                    )
                )
            }
        },
    )
}
