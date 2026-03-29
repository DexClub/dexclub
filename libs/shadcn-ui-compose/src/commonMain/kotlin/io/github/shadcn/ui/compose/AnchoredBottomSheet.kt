package io.github.shadcn.ui.compose

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs

interface BottomSheetScope {
    fun Modifier.draggableMarker(): Modifier
}

interface BottomSheetStateListener {
    fun onShowStarted() {}

    fun onShowFinished() {}

    fun onDismissStarted() {}

    fun onDismissFinished() {}
}

internal enum class ButtonSheetAnchor {
    Hidden,
    Expanded,
}

internal class BottomSheetScopeImpl(
    private val anchoredDraggableState: AnchoredDraggableState<ButtonSheetAnchor>,
) : BottomSheetScope {
    override fun Modifier.draggableMarker(): Modifier = this.then(
        Modifier.anchoredDraggable(
            state = anchoredDraggableState,
            orientation = Orientation.Vertical,
            enabled = true,
        )
    )
}

internal data class BottomSheetDraggableState<A, B, C, D, E>(
    val first: A,
    val second: B,
    val three: C,
    val four: D,
    val five: E
)

@Composable
private fun rememberAnchoredBottomSheetScope(
    anchoredDraggableState: AnchoredDraggableState<ButtonSheetAnchor>,
) = remember(anchoredDraggableState) {
    BottomSheetScopeImpl(anchoredDraggableState)
}

@Composable
private fun rememberNestedScrollConnection(
    anchoredDraggableState: AnchoredDraggableState<ButtonSheetAnchor>,
    draggableCloseable: Boolean,
) = remember(anchoredDraggableState, draggableCloseable) {
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!draggableCloseable) return super.onPreScroll(available, source)

            val delta = available.y
            return if (delta < 0 && source == NestedScrollSource.UserInput) {
                val currentOffset = anchoredDraggableState.offset
                if (!currentOffset.isNaN() && currentOffset > 0f) {
                    Offset(0f, anchoredDraggableState.dispatchRawDelta(delta))
                } else Offset.Zero
            } else Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (!draggableCloseable) return super.onPostScroll(consumed, available, source)

            val delta = available.y
            return if (delta > 0 && source == NestedScrollSource.UserInput) {
                Offset(0f, anchoredDraggableState.dispatchRawDelta(delta))
            } else Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!draggableCloseable) return super.onPreFling(available)

            val toFling = available.y
            return if (toFling < 0 && anchoredDraggableState.offset > 0f) {
                anchoredDraggableState.settle(animationSpec = spring())
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (!draggableCloseable) return super.onPostFling(consumed, available)

            anchoredDraggableState.settle(animationSpec = spring())
            return available
        }
    }
}

@Composable
fun BottomSheetDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    outsideCloseable: Boolean = true, // 是否允许点击外部阴影部分关闭
    draggableCloseable: Boolean = true, // 是否允许拖动内容关闭(与draggableMarker不同, draggableMarker始终允许拖动关闭)
    maskLayerColor: Color = Color.Black.copy(alpha = 0.3f),
    containerColor: Color = Color.White,
    maxHeightFraction: Float = 1f,
    shape: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    stateListener: BottomSheetStateListener? = null,
    content: @Composable BottomSheetScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = ButtonSheetAnchor.Hidden,
        )
    }

    val bottomSheetScope = rememberAnchoredBottomSheetScope(anchoredDraggableState)

    val nestedScrollConnection = rememberNestedScrollConnection(anchoredDraggableState, draggableCloseable)

    var hasAnimated by remember { mutableStateOf(false) }
    var lastStartedTarget by remember { mutableStateOf<ButtonSheetAnchor?>(null) }
    var lastFinishedTarget by remember { mutableStateOf<ButtonSheetAnchor?>(null) }

    LaunchedEffect(anchoredDraggableState) {
        snapshotFlow {
            val offset = if (anchoredDraggableState.offset.isNaN()) -1f else anchoredDraggableState.offset
            val target = anchoredDraggableState.targetValue
            val current = anchoredDraggableState.currentValue
            val isRunning = anchoredDraggableState.isAnimationRunning
            val targetPos = if (anchoredDraggableState.anchors.hasPositionFor(target)) {
                anchoredDraggableState.anchors.positionOf(target)
            } else -1f
            BottomSheetDraggableState(target, current, offset, targetPos, isRunning)
        }
            .drop(1) // 跳过初始状态
            .collect { (target, current, offset, targetPos, isRunning) ->
                if (offset == -1f || targetPos == -1f) return@collect
                // println("target: $target, current: $current, offset: $offset, targetPos: $targetPos, isRunning: $isRunning, hasAnimated: $hasAnimated")

                // --- 1. 处理 Started 回调 ---
                if (isRunning && lastStartedTarget != target) {
                    if (target == ButtonSheetAnchor.Expanded) {
                        stateListener?.onShowStarted()
                        lastStartedTarget = ButtonSheetAnchor.Expanded
                    } else if (target == ButtonSheetAnchor.Hidden && hasAnimated) {
                        stateListener?.onDismissStarted()
                        lastStartedTarget = ButtonSheetAnchor.Hidden
                    }
                }

                // --- 2. 处理 Finished 回调 ---
                val isAtTarget = abs(offset - targetPos) < 0.5f
                if (!isRunning && isAtTarget && lastFinishedTarget != target) {
                    if (target == ButtonSheetAnchor.Expanded) {
                        stateListener?.onShowFinished()
                        lastFinishedTarget = ButtonSheetAnchor.Expanded
                    } else if (target == ButtonSheetAnchor.Hidden && hasAnimated) {
                        stateListener?.onDismissFinished()
                        lastFinishedTarget = ButtonSheetAnchor.Hidden
                        onDismissRequest()
                    }
                }
            }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fullHeight = constraints.maxHeight.toFloat()
        var contentHeight by remember { mutableFloatStateOf(0f) }

        val progress by remember {
            derivedStateOf {
                val offset = anchoredDraggableState.offset
                if (offset.isNaN() || contentHeight <= 0f) {
                    if (hasAnimated) 0f else 0f
                } else {
                    (1f - (offset / contentHeight)).coerceIn(0f, 1f)
                }
            }
        }

        val dismiss: () -> Unit = {
            coroutineScope.launch {
                anchoredDraggableState.animateTo(ButtonSheetAnchor.Hidden)
            }
        }

        // 背景遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress }
                .background(maskLayerColor)
                .clickable(
                    enabled = outsideCloseable,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { dismiss() }
        )

        Box(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxHeightFraction.let { (maxHeight * it) })
                .onSizeChanged { size ->
                    val sheetHeight = size.height.toFloat()
                    if (sheetHeight > 0f && sheetHeight != contentHeight) {
                        contentHeight = sheetHeight
                        val newAnchors = DraggableAnchors {
                            ButtonSheetAnchor.Hidden at sheetHeight
                            ButtonSheetAnchor.Expanded at 0f
                        }
                        anchoredDraggableState.updateAnchors(newAnchors)

                        if (!hasAnimated) {
                            hasAnimated = true
                            coroutineScope.launch {
                                anchoredDraggableState.animateTo(ButtonSheetAnchor.Expanded)
                            }
                        }
                    }
                }
                .graphicsLayer {
                    val offset = anchoredDraggableState.offset
                    translationY = if (offset.isNaN()) {
                        if (!hasAnimated) fullHeight else 0f
                    } else {
                        offset
                    }
                }
                .clip(shape)
                .background(containerColor)
                .nestedScroll(nestedScrollConnection)
                .then(
                    if (draggableCloseable) {
                        Modifier.anchoredDraggable(
                            state = anchoredDraggableState,
                            orientation = Orientation.Vertical,
                            flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                                state = anchoredDraggableState,
                                positionalThreshold = { distance -> distance * 0.5f },
                                animationSpec = spring(),
                            )
                        )
                    } else Modifier
                ),
        ) {
            content(bottomSheetScope)
        }
    }
}