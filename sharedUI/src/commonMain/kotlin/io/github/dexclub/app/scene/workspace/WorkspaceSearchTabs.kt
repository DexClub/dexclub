package io.github.dexclub.app.scene.workspace

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.ext.shadcnClickable

enum class WorkspaceSearchTab(
    val label: String,
    val itemLabel: String,
    val placeholder: String,
) {
    ClassName(
        label = "类名",
        itemLabel = "类",
        placeholder = "输入完整类名或简单类名，例如 MainActivity",
    ),
    StringLiteral(
        label = "字符串",
        itemLabel = "方法",
        placeholder = "输入字符串常量片段，例如 userInfo",
    ),
    ;

    companion object {
        val segmentTabs = listOf(
            ClassName,
            StringLiteral,
        )
    }
}

@Composable
internal fun WorkspaceSearchTabs(
    currentTab: WorkspaceSearchTab,
    onTabSelected: (WorkspaceSearchTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = WorkspaceSearchTab.segmentTabs

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        val tabContainerWidth = (maxWidth * 0.68f)
            .coerceAtMost(260.dp)
            .coerceAtLeast(200.dp)
            .coerceAtMost(maxWidth)
        val indicatorInset = 3.dp
        val indicatorWidth = (tabContainerWidth - indicatorInset * 2) / tabs.size.toFloat()
        val selectedIndex = tabs.indexOf(currentTab).coerceAtLeast(0)
        val indicatorOffset by animateDpAsState(
            targetValue = indicatorWidth * selectedIndex.toFloat(),
            animationSpec = tween(
                durationMillis = 260,
                easing = FastOutSlowInEasing,
            ),
            label = "workspaceSearchTabIndicatorOffset",
        )

        Box(
            modifier = Modifier
                .width(tabContainerWidth)
                .height(36.dp)
                .clip(CircleShape)
                .background(ShadcnTheme.colors.muted.copy(alpha = 0.85f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(indicatorInset),
            ) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = indicatorOffset.roundToPx(),
                                y = 0,
                            )
                        }
                        .width(indicatorWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(ShadcnTheme.colors.primary),
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    tabs.forEach { tab ->
                        val selected = tab == currentTab
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .shadcnClickable(
                                    role = Role.Tab,
                                    indicationColor = ShadcnTheme.colors.accent.copy(alpha = 0.22f),
                                ) {
                                    if (!selected) {
                                        onTabSelected(tab)
                                    }
                                },
                        ) {
                            Text(
                                text = tab.label,
                                style = ShadcnTheme.textStyles.labelLarge.copy(
                                    color = if (selected) {
                                        ShadcnTheme.colors.primaryForeground
                                    } else {
                                        ShadcnTheme.colors.mutedForeground
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
