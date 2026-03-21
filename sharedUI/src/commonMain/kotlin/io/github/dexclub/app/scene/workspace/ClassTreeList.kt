package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.HorizontalScrollbar
import io.github.shadcn.ui.compose.VerticalScrollbar
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun SideLazyColumn(
    uiState: WorkspaceSidePanelUiState,
    callbacks: WorkspaceSidePanelCallbacks,
    showScrollbars: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = uiState.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = uiState.firstVisibleItemScrollOffset,
    )
    val scrollState = rememberScrollState(initial = uiState.horizontalScrollOffset)
    var maxContentWidthPx by remember { mutableIntStateOf(0) }
    val contentWidthCache = remember { mutableStateMapOf<String, Int>() }
    val flattenedItemKeys = remember(uiState.flattenedItems) {
        uiState.flattenedItems.map { it.node.workspaceSideStableKey() }
    }
    val density = LocalDensity.current

    LaunchedEffect(flattenedItemKeys) {
        maxContentWidthPx = resolveSidePanelMaxContentWidthPx(
            itemKeys = flattenedItemKeys,
            widthCache = contentWidthCache,
        )
    }

    LaunchedEffect(
        uiState.firstVisibleItemIndex,
        uiState.firstVisibleItemScrollOffset,
        uiState.flattenedItems.size,
    ) {
        if (uiState.flattenedItems.isEmpty()) return@LaunchedEffect
        val targetIndex = uiState.firstVisibleItemIndex.coerceIn(0, uiState.flattenedItems.lastIndex)
        val targetOffset = uiState.firstVisibleItemScrollOffset.coerceAtLeast(0)
        if (
            listState.firstVisibleItemIndex != targetIndex ||
            listState.firstVisibleItemScrollOffset != targetOffset
        ) {
            listState.scrollToItem(targetIndex, targetOffset)
        }
    }

    LaunchedEffect(uiState.horizontalScrollOffset) {
        val target = uiState.horizontalScrollOffset.coerceAtLeast(0)
        if (scrollState.value != target) {
            scrollState.scrollTo(target)
        }
    }

    LaunchedEffect(listState, scrollState) {
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                scrollState.value,
            )
        }.collectLatest { (firstVisibleItemIndex, firstVisibleItemScrollOffset, horizontalScrollOffset) ->
            callbacks.onScrollOffsetsChange(
                firstVisibleItemIndex,
                firstVisibleItemScrollOffset,
                horizontalScrollOffset,
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val viewportWidthDp = maxWidth
            val viewportWidthPx = with(density) { viewportWidthDp.roundToPx() }
            val scrollableWidthDp = with(density) { maxOf(viewportWidthPx, maxContentWidthPx).toDp() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState),
            ) {
                Box(
                    modifier = Modifier
                        .width(scrollableWidthDp)
                        .fillMaxHeight(),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp),
                    ) {
                        items(
                            items = uiState.flattenedItems,
                            key = { it.node.workspaceSideStableKey() },
                        ) { item ->
                            val itemKey = item.node.workspaceSideStableKey()
                            SideTreeRow(
                                flattenedNode = item,
                                expandedPaths = uiState.expandedPaths,
                                isSelected = uiState.selection?.matches(item.node) == true,
                                onClick = callbacks.onNodeClick,
                                onContentWidthChanged = { rowWidthPx ->
                                    maxContentWidthPx = updateSidePanelContentWidthCache(
                                        itemKey = itemKey,
                                        widthPx = rowWidthPx,
                                        visibleItemKeys = flattenedItemKeys,
                                        widthCache = contentWidthCache,
                                        currentMaxWidthPx = maxContentWidthPx,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        if (showScrollbars) {
            VerticalScrollbar(
                state = listState,
                autoHide = false,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(vertical = 12.dp, horizontal = 2.dp),
            )

            HorizontalScrollbar(
                state = scrollState,
                autoHide = false,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(vertical = 2.dp, horizontal = 12.dp),
            )
        }
    }
}

private fun resolveSidePanelMaxContentWidthPx(
    itemKeys: List<String>,
    widthCache: Map<String, Int>,
): Int {
    return itemKeys.maxOfOrNull { widthCache[it] ?: 0 } ?: 0
}

private fun updateSidePanelContentWidthCache(
    itemKey: String,
    widthPx: Int,
    visibleItemKeys: List<String>,
    widthCache: MutableMap<String, Int>,
    currentMaxWidthPx: Int,
): Int {
    val previous = widthCache.put(itemKey, widthPx)
    return when {
        widthPx > currentMaxWidthPx -> widthPx
        previous == null || previous == widthPx -> currentMaxWidthPx
        previous < currentMaxWidthPx -> currentMaxWidthPx
        else -> resolveSidePanelMaxContentWidthPx(visibleItemKeys, widthCache)
    }
}
