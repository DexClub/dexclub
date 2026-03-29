package io.github.shadcn.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class LazyListScrollbarInfo(
    val thumbSizePercent: Float,
    val dragToScrollRatio: Float,
    val averageItemSize: Float,
    val scrollableRange: Float,
)

private fun LazyListState.computeScrollbarInfo(): LazyListScrollbarInfo? {
    val layoutInfo = layoutInfo
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    if (visibleItemsInfo.isEmpty()) return null
    val viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    val firstItem = visibleItemsInfo.first()
    val lastItem = visibleItemsInfo.last()
    val averageItemSize = (lastItem.offset + lastItem.size - firstItem.offset).toFloat() / visibleItemsInfo.size
    val estimatedTotal = averageItemSize * layoutInfo.totalItemsCount
    if (estimatedTotal <= viewportSize) return null
    val scrollableRange = estimatedTotal - viewportSize
    val thumbSizePercent = (viewportSize / estimatedTotal).coerceIn(0.1f, 1f)
    val thumbTrackRangePx = viewportSize * (1f - thumbSizePercent)
    val dragToScrollRatio = if (thumbTrackRangePx > 0) scrollableRange / thumbTrackRangePx else 0f
    return LazyListScrollbarInfo(thumbSizePercent, dragToScrollRatio, averageItemSize, scrollableRange)
}

@Composable
private fun rememberScrollbarVisuals(
    isScrolling: Boolean,
    isTrackHovering: Boolean,
    isThumbHovering: Boolean,
    hasContent: Boolean,
    autoHide: Boolean,
    color: Color,
    activeColor: Color,
): Triple<MutableState<Boolean>, Color, Float> {
    val isDragging = remember { mutableStateOf(false) }
    // 滑块高亮：仅滚动中、拖动中、鼠标悬停在滑块上时触发
    val isColorActive = isScrolling || isDragging.value || isThumbHovering
    // 显示触发：以上三种 + 鼠标悬停在轨道上
    val isShowActive = isColorActive || isTrackHovering
    val animatedColor by animateColorAsState(
        targetValue = if (isColorActive) activeColor else color,
        animationSpec = tween(500),
        label = "ScrollbarColor"
    )
    var showByTouch by remember { mutableStateOf(false) }
    LaunchedEffect(isShowActive) {
        if (isShowActive) showByTouch = true
        else { delay(1500); showByTouch = false }
    }
    val alpha by animateFloatAsState(
        targetValue = if (hasContent && (!autoHide || showByTouch)) 1f else 0f,
        animationSpec = tween(if (isShowActive) 100 else 500),
        label = "ScrollbarAlpha"
    )
    return Triple(isDragging, animatedColor, alpha)
}

@Composable
private fun LazyListScrollbar(
    state: LazyListState,
    isVertical: Boolean,
    thickness: Dp,
    modifier: Modifier,
    color: Color,
    activeColor: Color,
    autoHide: Boolean,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollbarInfo by remember { derivedStateOf { state.computeScrollbarInfo() } }
    val trackInteractionSource = remember { MutableInteractionSource() }
    val thumbInteractionSource = remember { MutableInteractionSource() }
    val isTrackHovering by trackInteractionSource.collectIsHoveredAsState()
    val isThumbHovering by thumbInteractionSource.collectIsHoveredAsState()
    val (isDragging, thumbColor, alpha) = rememberScrollbarVisuals(
        isScrolling = state.isScrollInProgress,
        isTrackHovering = isTrackHovering,
        isThumbHovering = isThumbHovering,
        hasContent = scrollbarInfo != null,
        autoHide = autoHide,
        color = color,
        activeColor = activeColor,
    )

    // 外层 Box 始终存在，用于 hover 检测（桌面端鼠标扫过即触发显示）
    // 隐藏时内层不渲染，确保不拦截下方内容的点击事件
    Box(
        modifier = modifier
            .then(if (isVertical) Modifier.fillMaxHeight().width(thickness) else Modifier.fillMaxWidth().height(thickness))
            .hoverable(trackInteractionSource)
    ) {
        if (alpha > 0f) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            scrollbarInfo ?: return@detectTapGestures
                            val ratio = if (isVertical) offset.y / size.height else offset.x / size.width
                            coroutineScope.launch {
                                state.scrollToItem((state.layoutInfo.totalItemsCount * ratio.coerceIn(0f, 1f)).toInt())
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        var scrollJob: Job? = null
                        detectDragGestures(
                            onDragStart = { isDragging.value = true },
                            onDragEnd = { isDragging.value = false },
                            onDragCancel = { isDragging.value = false },
                            onDrag = { change, dragAmount ->
                                val info = scrollbarInfo ?: return@detectDragGestures
                                change.consume()
                                val delta = if (isVertical) dragAmount.y else dragAmount.x
                                val currentPx = state.firstVisibleItemIndex * info.averageItemSize + state.firstVisibleItemScrollOffset
                                val targetPx = (currentPx + delta * info.dragToScrollRatio).coerceIn(0f, info.scrollableRange)
                                val targetIndex = (targetPx / info.averageItemSize).toInt()
                                    .coerceIn(0, state.layoutInfo.totalItemsCount - 1)
                                val targetOffset = (targetPx - targetIndex * info.averageItemSize).toInt().coerceAtLeast(0)
                                scrollJob?.cancel()
                                scrollJob = coroutineScope.launch {
                                    state.scrollToItem(targetIndex, targetOffset)
                                }
                            }
                        )
                    }
            ) {
                val info = scrollbarInfo ?: return@BoxWithConstraints
                val trackPx = if (isVertical) constraints.maxHeight.toFloat() else constraints.maxWidth.toFloat()
                val thumbTrackRangePx = trackPx * (1f - info.thumbSizePercent)
                val currentPx = state.firstVisibleItemIndex * info.averageItemSize + state.firstVisibleItemScrollOffset
                val thumbOffset = ((currentPx / info.scrollableRange).coerceIn(0f, 1f) * thumbTrackRangePx).roundToInt()
                Box(
                    modifier = Modifier
                        .offset { if (isVertical) IntOffset(0, thumbOffset) else IntOffset(thumbOffset, 0) }
                        .then(
                            if (isVertical) Modifier.fillMaxHeight(info.thumbSizePercent).fillMaxWidth().padding(horizontal = 2.dp)
                            else Modifier.fillMaxWidth(info.thumbSizePercent).fillMaxHeight().padding(vertical = 2.dp)
                        )
                        .hoverable(thumbInteractionSource)
                        .background(thumbColor, RoundedCornerShape(thickness / 2))
                )
            }
        }
    }
}

@Composable
fun VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    width: Dp = 8.dp,
    color: Color = Color.Gray.copy(alpha = 0.3f),
    activeColor: Color = Color.Gray.copy(alpha = 0.7f),
    autoHide: Boolean = true,
) = LazyListScrollbar(state, isVertical = true, thickness = width, modifier = modifier, color = color, activeColor = activeColor, autoHide = autoHide)

@Composable
fun HorizontalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color = Color.Gray.copy(alpha = 0.3f),
    activeColor: Color = Color.Gray.copy(alpha = 0.7f),
    autoHide: Boolean = true,
) = LazyListScrollbar(state, isVertical = false, thickness = height, modifier = modifier, color = color, activeColor = activeColor, autoHide = autoHide)

@Composable
private fun ScrollStateScrollbar(
    state: ScrollState,
    isVertical: Boolean,
    thickness: Dp,
    modifier: Modifier,
    color: Color,
    activeColor: Color,
    autoHide: Boolean,
) {
    val coroutineScope = rememberCoroutineScope()
    val maxValuePx = state.maxValue
    val trackInteractionSource = remember { MutableInteractionSource() }
    val thumbInteractionSource = remember { MutableInteractionSource() }
    val isTrackHovering by trackInteractionSource.collectIsHoveredAsState()
    val isThumbHovering by thumbInteractionSource.collectIsHoveredAsState()
    val (isDragging, thumbColor, alpha) = rememberScrollbarVisuals(
        isScrolling = state.isScrollInProgress,
        isTrackHovering = isTrackHovering,
        isThumbHovering = isThumbHovering,
        hasContent = maxValuePx > 0,
        autoHide = autoHide,
        color = color,
        activeColor = activeColor,
    )

    Box(
        modifier = modifier
            .then(if (isVertical) Modifier.fillMaxHeight().width(thickness) else Modifier.fillMaxWidth().height(thickness))
            .hoverable(trackInteractionSource)
    ) {
        if (alpha > 0f) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha }
            ) {
                if (maxValuePx <= 0) return@BoxWithConstraints
                val viewportPx = if (isVertical) constraints.maxHeight.toFloat() else constraints.maxWidth.toFloat()
                val maxPx = maxValuePx.toFloat()
                val thumbSizePercent = (viewportPx / (maxPx + viewportPx)).coerceIn(0.1f, 1f)
                val thumbTrackRangePx = viewportPx * (1f - thumbSizePercent)
                val dragToScrollRatio = if (thumbTrackRangePx > 0) maxPx / thumbTrackRangePx else 0f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val ratio = if (isVertical) offset.y / size.height else offset.x / size.width
                                coroutineScope.launch { state.scrollTo((state.maxValue * ratio.coerceIn(0f, 1f)).toInt()) }
                            }
                        }
                        .pointerInput(dragToScrollRatio) {
                            detectDragGestures(
                                onDragStart = { isDragging.value = true },
                                onDragEnd = { isDragging.value = false },
                                onDragCancel = { isDragging.value = false },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val delta = if (isVertical) dragAmount.y else dragAmount.x
                                    state.dispatchRawDelta(delta * dragToScrollRatio)
                                }
                            )
                        }
                ) {
                    val thumbOffsetPx = (state.value.toFloat() / maxPx * thumbTrackRangePx).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { if (isVertical) IntOffset(0, thumbOffsetPx) else IntOffset(thumbOffsetPx, 0) }
                            .then(
                                if (isVertical) Modifier.fillMaxHeight(thumbSizePercent).fillMaxWidth().padding(horizontal = 2.dp)
                                else Modifier.fillMaxWidth(thumbSizePercent).fillMaxHeight().padding(vertical = 2.dp)
                            )
                            .hoverable(thumbInteractionSource)
                            .background(thumbColor, RoundedCornerShape(thickness / 2))
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
    width: Dp = 8.dp,
    color: Color = Color.Gray.copy(alpha = 0.3f),
    activeColor: Color = Color.Gray.copy(alpha = 0.7f),
    autoHide: Boolean = true,
) = ScrollStateScrollbar(state, isVertical = true, thickness = width, modifier = modifier, color = color, activeColor = activeColor, autoHide = autoHide)

@Composable
fun HorizontalScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color = Color.Gray.copy(alpha = 0.3f),
    activeColor: Color = Color.Gray.copy(alpha = 0.7f),
    autoHide: Boolean = true,
) = ScrollStateScrollbar(state, isVertical = false, thickness = height, modifier = modifier, color = color, activeColor = activeColor, autoHide = autoHide)
