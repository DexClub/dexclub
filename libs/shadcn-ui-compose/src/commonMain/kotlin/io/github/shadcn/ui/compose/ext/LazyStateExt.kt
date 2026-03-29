package io.github.shadcn.ui.compose.ext


import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState

// 判断 LazyListState 是否滚动到底部
fun LazyListState.isAtBottom() = isScrolledToEnd(0)

fun LazyListState.isScrolledToEnd(threshold: Int = 1) =
    layoutInfo.visibleItemsInfo.lastOrNull()
        ?.let { lastVisibleItem -> lastVisibleItem.index >= layoutInfo.totalItemsCount - 1 - threshold }
        ?: false

// 判断 LazyGridState 是否滚动到底部
fun LazyGridState.isAtBottom() = isScrolledToEnd(0)

fun LazyGridState.isScrolledToEnd(threshold: Int = 1) =
    layoutInfo.visibleItemsInfo.lastOrNull()
        ?.let { lastVisibleItem -> lastVisibleItem.index >= layoutInfo.totalItemsCount - 1 - threshold }
        ?: false