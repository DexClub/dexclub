package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.res.IconRes
import io.github.dexclub.app.res.resolveClassIcon
import io.github.dexclub.app.res.icons.PackageFold
import io.github.dexclub.app.res.icons.PackageFolder
import io.github.dexclub.app.res.icons.PackageUnfold
import io.github.dexclub.node.ClassTreeNode
import io.github.dexclub.node.FlattenedNode
import io.github.shadcn.ui.compose.Icon
import io.github.shadcn.ui.compose.ShadcnTheme

@Composable
internal fun SideTreeRow(
    flattenedNode: FlattenedNode,
    expandedPaths: Set<String>,
    isSelected: Boolean,
    onClick: (ClassTreeNode) -> Unit,
    onContentWidthChanged: (Int) -> Unit,
) {
    val node = flattenedNode.node
    val backgroundColor = if (isSelected) {
        ShadcnTheme.colors.primary.copy(alpha = 0.14f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .sidePanelManualClickable { onClick(node) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .onSizeChanged { onContentWidthChanged(it.width) }
                .padding(
                    start = ContentHorizontalPadding + (flattenedNode.depth * 16).dp,
                    end = ContentHorizontalPadding,
                    top = 4.dp,
                    bottom = 4.dp,
                ),
        ) {
            when (node) {
                is ClassTreeNode.PackageNode -> {
                    Icon(
                        imageVector = if (node.fullPath in expandedPaths) IconRes.PackageFold else IconRes.PackageUnfold,
                        contentDescription = null,
                        tint = null,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(10.dp),
                    )

                    Icon(
                        imageVector = IconRes.PackageFolder,
                        contentDescription = null,
                        tint = null,
                        modifier = Modifier.size(16.dp),
                    )
                }

                is ClassTreeNode.ClassNode -> {
                    Spacer(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(10.dp),
                    )

                    Icon(
                        imageVector = resolveClassIcon(node.classVisualKind),
                        contentDescription = null,
                        tint = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = when (node) {
                    is ClassTreeNode.PackageNode -> node.name
                    is ClassTreeNode.ClassNode -> node.displayName
                },
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                style = ShadcnTheme.textStyles.bodySmall,
            )
        }
    }
}

internal fun Modifier.sidePanelManualClickable(onClick: () -> Unit) = pointerInput(onClick) {
    detectTapGestures(
        onTap = { onClick() },
    )
}
