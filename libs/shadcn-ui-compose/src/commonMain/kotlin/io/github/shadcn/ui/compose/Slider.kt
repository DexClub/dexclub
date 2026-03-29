package io.github.shadcn.ui.compose

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun Slider(
    value: Float, // 0.0 ~ 1.0
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fraction = value.coerceIn(0f, 1f)

    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    val inactiveTrackColor = if (enabled) ShadcnTheme.colors.muted else ShadcnTheme.colors.muted.copy(alpha = 0.5f)
    val activeTrackColor = if (enabled) ShadcnTheme.colors.primary else ShadcnTheme.colors.primary.copy(alpha = 0.5f)
    val thumbColor = ShadcnTheme.colors.background
    val thumbBorderColor = if (enabled) ShadcnTheme.colors.primary else ShadcnTheme.colors.primary.copy(alpha = 0.5f)
    val haloColor = ShadcnTheme.colors.primary

    // 交互状态：是否正在拖拽/按下, 扩散动画
    var isDragging by remember { mutableStateOf(false) }
    val haloScale by animateFloatAsState(
        targetValue = if (isDragging && enabled) 1f else 0.5f, // 按下时扩散到 1.0，平时收缩
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow), // 使用弹簧效果让扩散更自然
        label = "haloScale"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (isDragging && enabled) 0.1f else 0f, // 按下时显示 10% 透明度
        animationSpec = tween(durationMillis = 200),
        label = "haloAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .heightIn(min = ThumbSize.coerceAtLeast(32.dp))
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                if (!enabled) disabled()
                // 无障碍服务调用 setProgress 时，输入也是 0~1 的比例
                setProgress(
                    action = { targetValue ->
                        val newValue = targetValue.coerceIn(0f, 1f)
                        if (newValue != fraction) {
                            onValueChange(newValue)
                            true
                        } else {
                            false
                        }
                    }
                )
            }
            .pointerInput(enabled, isRtl) { // key 加入 isRtl
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isDragging = true // 触发动画
                    try { // try onValueChange
                        val width = size.width.toFloat()

                        fun calculateValue(x: Float): Float {
                            if (width <= 0) return 0f
                            val rawFraction = (x / width).coerceIn(0f, 1f)
                            // 如果是 RTL，0 在最右边，所以要用 1 减去比例
                            return if (isRtl) 1f - rawFraction else rawFraction
                        }

                        onValueChange(calculateValue(down.position.x))

                        drag(down.id) { change ->
                            change.consume()
                            onValueChange(calculateValue(change.position.x))
                        }
                    } finally {
                        isDragging = false // 关闭动画
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val thumbRadiusPx = with(LocalDensity.current) { (ThumbSize / 2).toPx() }
        val visualFraction = if (isRtl) 1f - fraction else fraction
        val thumbCenterPx = visualFraction * totalWidth
        val thumbOffsetPx = (thumbCenterPx - thumbRadiusPx).roundToInt()

        // 1. 轨道背景 (Inactive Track)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TrackHeight)
                .align(Alignment.Center)
                .clip(TrackCornerRadius)
                .background(inactiveTrackColor)
        )

        // 2. 激活轨道 (Active Track / Range)
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(TrackHeight)
                .align(Alignment.CenterStart)
                .clip(TrackCornerRadius)
                .background(activeTrackColor)
        )

        // 3. 滑块 (Thumb)
        Box(
            modifier = Modifier
                .offset { IntOffset(x = thumbOffsetPx, y = 0) }
                .align(Alignment.CenterStart)
                .size(ThumbSize),
            contentAlignment = Alignment.Center
        ) {
            // 3.1 扩散光环 (Halo) - 绘制在底层
            Box(
                modifier = Modifier
                    .requiredSize(ThumbSize + ThumbSize / 2) // 光环最大尺寸, 允许溢出父组件大小
                    .scale(haloScale) // 缩放动画
                    .alpha(haloAlpha) // 透明度动画
                    .background(haloColor, CircleShape)
            )

            // 3.2 滑块本体 (Thumb) - 绘制在顶层
            Box(
                modifier = Modifier
                    .size(ThumbSize)
                    .shadow(elevation = ThumbElevation, shape = CircleShape)
                    .background(thumbColor)
                    .border(width = ThumbBorderWidth, color = thumbBorderColor, shape = CircleShape)
            )
        }
    }
}

private val ThumbSize = 20.dp
private val ThumbBorderWidth = 2.dp
private val ThumbElevation = 2.dp
private val TrackHeight = 6.dp
private val TrackCornerRadius = RoundedCornerShape(100)