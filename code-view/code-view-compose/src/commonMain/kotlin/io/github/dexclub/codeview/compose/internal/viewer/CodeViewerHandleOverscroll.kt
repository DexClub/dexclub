package io.github.dexclub.codeview.compose.internal.viewer

import kotlin.math.abs
import kotlin.math.sign

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun rememberCodeViewerHandleOverscrollEffect(): CodeViewerHandleOverscrollEffect {
    val scope = rememberCoroutineScope()
    val visualEffect = rememberOverscrollEffect()
    return remember(scope, visualEffect) {
        CodeViewerHandleOverscrollEffect(
            scope = scope,
            visualEffect = visualEffect,
        )
    }
}

internal class CodeViewerHandleOverscrollEffect(
    private val scope: CoroutineScope,
    private val visualEffect: OverscrollEffect?,
) : OverscrollEffect {
    // The platform effect owns the actual stretch drawing. We only keep a lightweight
    // overscroll estimate so viewport-based helpers can still position overlays that
    // are rendered outside of the stretched content subtree.
    var horizontalContentOffsetPx by mutableFloatStateOf(0f)
        private set
    var verticalContentOffsetPx by mutableFloatStateOf(0f)
        private set

    var viewportWidthPx by mutableFloatStateOf(1f)
    var viewportHeightPx by mutableFloatStateOf(1f)

    private val horizontalAnimatable = Animatable(0f)
    private val verticalAnimatable = Animatable(0f)
    private var animationJob: Job? = null

    override val isInProgress: Boolean
        get() {
            return visualEffect?.isInProgress == true ||
                abs(horizontalContentOffsetPx) > HANDLE_OVERSCROLL_MIN_DELTA_PX ||
                abs(verticalContentOffsetPx) > HANDLE_OVERSCROLL_MIN_DELTA_PX ||
                animationJob?.isActive == true
        }

    override val node: DelegatableNode = visualEffect?.node ?: object : androidx.compose.ui.Modifier.Node() {}

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        if (source == NestedScrollSource.UserInput) {
            animationJob?.cancel()
        }

        observeReverseScroll(delta)

        if (visualEffect == null) {
            val consumedByScroll = performScroll(delta)
            observeStretch(
                leftOver = delta - consumedByScroll,
                source = source,
            )
            return consumedByScroll
        }

        return visualEffect.applyToScroll(delta, source) { available ->
            val consumedByScroll = performScroll(available)
            observeStretch(
                leftOver = available - consumedByScroll,
                source = source,
            )
            consumedByScroll
        }
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        var leftOverVelocity = Velocity.Zero

        if (visualEffect == null) {
            val consumedByFling = performFling(velocity)
            leftOverVelocity = velocity - consumedByFling
        } else {
            visualEffect.applyToFling(velocity) { available ->
                val consumedByFling = performFling(available)
                leftOverVelocity = available - consumedByFling
                consumedByFling
            }
        }

        animationJob?.cancel()
        if (
            abs(horizontalContentOffsetPx) <= HANDLE_OVERSCROLL_MIN_DELTA_PX &&
            abs(verticalContentOffsetPx) <= HANDLE_OVERSCROLL_MIN_DELTA_PX
        ) {
            horizontalContentOffsetPx = 0f
            verticalContentOffsetPx = 0f
            return
        }

        animationJob = scope.launch {
            coroutineScope {
                launch {
                    horizontalAnimatable.snapTo(horizontalContentOffsetPx)
                    horizontalAnimatable.animateTo(
                        targetValue = 0f,
                        initialVelocity = leftOverVelocity.x * HANDLE_OVERSCROLL_FLING_FACTOR,
                        animationSpec = spring(
                            stiffness = HANDLE_OVERSCROLL_SPRING_STIFFNESS,
                            dampingRatio = HANDLE_OVERSCROLL_SPRING_DAMPING_RATIO,
                        ),
                    ) {
                        horizontalContentOffsetPx = value
                    }
                }
                launch {
                    verticalAnimatable.snapTo(verticalContentOffsetPx)
                    verticalAnimatable.animateTo(
                        targetValue = 0f,
                        initialVelocity = leftOverVelocity.y * HANDLE_OVERSCROLL_FLING_FACTOR,
                        animationSpec = spring(
                            stiffness = HANDLE_OVERSCROLL_SPRING_STIFFNESS,
                            dampingRatio = HANDLE_OVERSCROLL_SPRING_DAMPING_RATIO,
                        ),
                    ) {
                        verticalContentOffsetPx = value
                    }
                }
            }
        }
    }

    private fun observeReverseScroll(delta: Offset) {
        horizontalContentOffsetPx = relaxTrackedOffset(
            trackedOffsetPx = horizontalContentOffsetPx,
            deltaPx = delta.x,
        )
        verticalContentOffsetPx = relaxTrackedOffset(
            trackedOffsetPx = verticalContentOffsetPx,
            deltaPx = delta.y,
        )
    }

    private fun observeStretch(
        leftOver: Offset,
        source: NestedScrollSource,
    ) {
        if (source != NestedScrollSource.UserInput) return

        // These tracked offsets are intentionally approximate. They are not trying to
        // recreate Android's RenderNode stretch exactly, only to provide a stable
        // signal for viewport math outside of the overscrolled layer.
        horizontalContentOffsetPx = stretchTrackedOffset(
            trackedOffsetPx = horizontalContentOffsetPx,
            leftOverPx = leftOver.x,
            viewportSizePx = viewportWidthPx,
            dragFactor = CONTENT_OVERSCROLL_DRAG_FACTOR_HORIZONTAL,
            maxOffsetPx = MAX_CONTENT_OVERSCROLL_OFFSET_HORIZONTAL_PX,
        )
        verticalContentOffsetPx = stretchTrackedOffset(
            trackedOffsetPx = verticalContentOffsetPx,
            leftOverPx = leftOver.y,
            viewportSizePx = viewportHeightPx,
            dragFactor = CONTENT_OVERSCROLL_DRAG_FACTOR_VERTICAL,
            maxOffsetPx = MAX_CONTENT_OVERSCROLL_OFFSET_VERTICAL_PX,
        )
    }

    private fun relaxTrackedOffset(
        trackedOffsetPx: Float,
        deltaPx: Float,
    ): Float {
        if (trackedOffsetPx == 0f || deltaPx == 0f) return trackedOffsetPx
        if (sign(deltaPx) == sign(trackedOffsetPx)) return trackedOffsetPx

        return if (abs(deltaPx) >= abs(trackedOffsetPx)) {
            0f
        } else {
            trackedOffsetPx + deltaPx
        }
    }

    private fun stretchTrackedOffset(
        trackedOffsetPx: Float,
        leftOverPx: Float,
        viewportSizePx: Float,
        dragFactor: Float,
        maxOffsetPx: Float,
    ): Float {
        if (abs(leftOverPx) <= HANDLE_OVERSCROLL_MIN_DELTA_PX) return trackedOffsetPx

        val safeViewportSizePx = viewportSizePx.coerceAtLeast(1f)
        val progress = (abs(trackedOffsetPx) / safeViewportSizePx).coerceIn(0f, 1f)
        val stretchFactor = dragFactor * (1f - progress)
        return (trackedOffsetPx + leftOverPx * stretchFactor)
            .coerceIn(-maxOffsetPx, maxOffsetPx)
    }
}

private const val HANDLE_OVERSCROLL_MIN_DELTA_PX: Float = 0.5f
internal const val MAX_CONTENT_OVERSCROLL_OFFSET_HORIZONTAL_PX: Float = 56f
internal const val MAX_CONTENT_OVERSCROLL_OFFSET_VERTICAL_PX: Float = 72f

private const val CONTENT_OVERSCROLL_DRAG_FACTOR_HORIZONTAL: Float = 0.42f
private const val CONTENT_OVERSCROLL_DRAG_FACTOR_VERTICAL: Float = 0.58f
private const val HANDLE_OVERSCROLL_FLING_FACTOR: Float = 0.05f
private const val HANDLE_OVERSCROLL_SPRING_STIFFNESS: Float = 260f
private const val HANDLE_OVERSCROLL_SPRING_DAMPING_RATIO: Float = 1.05f
