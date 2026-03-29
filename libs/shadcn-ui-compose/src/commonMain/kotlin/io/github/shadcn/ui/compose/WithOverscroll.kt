package io.github.shadcn.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

class StretchOverscrollEffect(private val scope: CoroutineScope) : OverscrollEffect {
    var visibleOffset by mutableStateOf(Offset.Zero)
    var containerSize by mutableStateOf(IntSize.Zero)
    private val animatableOffset = Animatable(Offset.Zero, Offset.VectorConverter)
    private var animationJob: Job? = null

    override val node: DelegatableNode = StretchOverscrollNode(this)

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        if (source == NestedScrollSource.UserInput) {
            animationJob?.cancel()
        }

        var remX = delta.x
        var remY = delta.y
        var consumedByReverseX = 0f
        var consumedByReverseY = 0f

        // 1. 反向抵消逻辑 (处理已有的拉伸)
        if (visibleOffset.x != 0f && sign(delta.x) != sign(visibleOffset.x)) {
            val consumed = if (abs(delta.x) >= abs(visibleOffset.x)) {
                val rem = delta.x + visibleOffset.x
                visibleOffset = visibleOffset.copy(x = 0f)
                delta.x - rem
            } else {
                visibleOffset = visibleOffset.copy(x = visibleOffset.x + delta.x)
                delta.x
            }
            consumedByReverseX = consumed
            remX = delta.x - consumed
        }

        if (visibleOffset.y != 0f && sign(delta.y) != sign(visibleOffset.y)) {
            val consumed = if (abs(delta.y) >= abs(visibleOffset.y)) {
                val rem = delta.y + visibleOffset.y
                visibleOffset = visibleOffset.copy(y = 0f)
                delta.y - rem
            } else {
                visibleOffset = visibleOffset.copy(y = visibleOffset.y + delta.y)
                delta.y
            }
            consumedByReverseY = consumed
            remY = delta.y - consumed
        }

        // 2. 正常滚动内容
        val consumedByScroll = performScroll(Offset(remX, remY))

        // 3. 处理拉伸 (计算剩余量)
        val leftOverX = remX - consumedByScroll.x
        val leftOverY = remY - consumedByScroll.y

        var consumedByStretchX = 0f
        var consumedByStretchY = 0f

        if (source == NestedScrollSource.UserInput && containerSize.width > 0 && containerSize.height > 0) {
            val width = containerSize.width.toFloat()
            val height = containerSize.height.toFloat()

            val newX = if (abs(leftOverX) > 0.5f) {
                val progress = (abs(visibleOffset.x) / width).coerceIn(0f, 1f)
                // 阻力系数：拉伸越大，移动越难
                val stretchFactor = 0.15f * (1f - progress)
                consumedByStretchX = leftOverX // 记录被拉伸吸收的滚动量
                visibleOffset.x + leftOverX * stretchFactor
            } else visibleOffset.x

            val newY = if (abs(leftOverY) > 0.5f) {
                val progress = (abs(visibleOffset.y) / height).coerceIn(0f, 1f)
                val stretchFactor = 0.15f * (1f - progress)
                consumedByStretchY = leftOverY // 记录被拉伸吸收的滚动量
                visibleOffset.y + leftOverY * stretchFactor
            } else visibleOffset.y

            visibleOffset = Offset(newX, newY)
        }

        // 必须返回 抵消量 + 滚动消费量 + 拉伸消费量
        return Offset(
            consumedByReverseX + consumedByScroll.x + consumedByStretchX,
            consumedByReverseY + consumedByScroll.y + consumedByStretchY
        )
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        val remainingVelocity = performFling(velocity)
        animationJob?.cancel()

        // 只有当存在拉伸时，才需要执行回弹动画
        if (visibleOffset == Offset.Zero) return

        animationJob = scope.launch {
            // Fling 撞击感转换：将速度传递给 Spring 动画
            val impulse = Offset(
                remainingVelocity.x * 0.05f,
                remainingVelocity.y * 0.05f
            )

            // 使用一个 Animatable 同时处理 x 和 y
            animatableOffset.snapTo(visibleOffset)
            animatableOffset.animateTo(
                targetValue = Offset.Zero,
                initialVelocity = impulse,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            ) {
                visibleOffset = this.value
            }
            animationJob?.join()
        }
    }

    // override val isInProgress: Boolean
    //     get() = visibleOffset != Offset.Zero || animationJob?.isActive == true

    override val isInProgress: Boolean
        get() = abs(visibleOffset.x) > 0.5f || abs(visibleOffset.y) > 0.5f || animationJob?.isActive == true
}

private class StretchOverscrollNode(
    private val effect: StretchOverscrollEffect
) : Modifier.Node(), DrawModifierNode, LayoutAwareModifierNode {

    override fun onRemeasured(size: IntSize) {
        super.onRemeasured(size)
        effect.containerSize = size
    }

    override fun ContentDrawScope.draw() {
        val offset = effect.visibleOffset
        if (
            (abs(offset.x) <= OVERSCROLL_VISUAL_EPSILON_PX && abs(offset.y) <= OVERSCROLL_VISUAL_EPSILON_PX) ||
            size.width == 0f ||
            size.height == 0f
        ) {
            drawContent()
        } else {
            // 计算 X 轴拉伸 (最大拉伸 3%)
            val scaleX = 1f + (abs(offset.x) / size.width * 0.5f).coerceAtMost(0.03f)
            val pivotX = if (offset.x > 0) 0f else size.width

            // 计算 Y 轴拉伸 (最大拉伸 3%)
            val scaleY = 1f + (abs(offset.y) / size.height * 0.5f).coerceAtMost(0.03f)
            val pivotY = if (offset.y > 0) 0f else size.height

            // 应用双向缩放
            scale(
                scaleX = scaleX,
                scaleY = scaleY,
                pivot = Offset(pivotX, pivotY)
            ) {
                this@draw.drawContent()
            }
        }
    }
}

private class StretchOverscrollFactory(private val scope: CoroutineScope) : OverscrollFactory {
    override fun createOverscrollEffect() = StretchOverscrollEffect(scope)
    override fun hashCode(): Int = scope.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return (other is StretchOverscrollFactory && other.scope == this.scope)
    }
}

@Composable
fun WithOverscroll(
    overscroll: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (!overscroll) {
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            content()
        }
        return
    }

    val scope = rememberCoroutineScope()
    val factory = remember { StretchOverscrollFactory(scope) }
    CompositionLocalProvider(LocalOverscrollFactory provides factory) {
        content()
    }
}

private const val OVERSCROLL_VISUAL_EPSILON_PX: Float = 0.5f
