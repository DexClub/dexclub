package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerScrollController
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

internal sealed interface TouchSelectionAutoScrollSession {
    var viewportPosition: Offset

    fun resolveSelection(
        layoutSnapshot: CodeLayoutSnapshot,
        lineLayoutCache: CodeLineTextLayoutCache,
        lineHeightPx: Float,
        scrollController: CodeViewerScrollController,
    ): TextRange
}

internal class LongPressTouchSelectionAutoScrollSession(
    private val initialSelection: TextRange,
    initialViewportPosition: Offset,
) : TouchSelectionAutoScrollSession {
    override var viewportPosition by mutableStateOf(initialViewportPosition)

    override fun resolveSelection(
        layoutSnapshot: CodeLayoutSnapshot,
        lineLayoutCache: CodeLineTextLayoutCache,
        lineHeightPx: Float,
        scrollController: CodeViewerScrollController,
    ): TextRange {
        return resolveLongPressDragSelectionState(
            viewportPosition = viewportPosition,
            scrollController = scrollController,
            layoutSnapshot = layoutSnapshot,
            lineLayoutCache = lineLayoutCache,
            lineHeightPx = lineHeightPx,
            initialSelection = initialSelection,
        ).selection
    }
}

internal class HandleTouchSelectionAutoScrollSession(
    private val target: TouchHandleAutoScrollTarget,
    initialViewportPosition: Offset,
) : TouchSelectionAutoScrollSession {
    override var viewportPosition by mutableStateOf(initialViewportPosition)

    override fun resolveSelection(
        layoutSnapshot: CodeLayoutSnapshot,
        lineLayoutCache: CodeLineTextLayoutCache,
        lineHeightPx: Float,
        scrollController: CodeViewerScrollController,
    ): TextRange {
        val draggedTextOffset = resolveTextOffsetFromViewportPosition(
            layoutSnapshot = layoutSnapshot,
            lineLayoutCache = lineLayoutCache,
            lineHeightPx = lineHeightPx,
            contentStartPaddingPx = scrollController.contentStartPaddingPx,
            contentLeftInsetPx = scrollController.contentLeftInsetPx,
            horizontalScrollPx = scrollController.horizontalScrollPx,
            verticalScrollPx = scrollController.verticalScrollPx,
            viewportPosition = viewportPosition,
        )
        return target.resolveSelection(draggedTextOffset)
    }
}

internal fun resolveTouchAutoScrollDelta(
    viewportPosition: Offset,
    scrollController: CodeViewerScrollController,
    frameDurationNanos: Long = DEFAULT_TOUCH_AUTO_SCROLL_FRAME_DURATION_NANOS,
): Offset {
    return Offset(
        x = resolveTouchAutoScrollDeltaComponent(
            overflowPx = resolveTouchAutoScrollOverflowPx(
                positionPx = viewportPosition.x,
                viewportStartPx = scrollController.contentLeftInsetPx,
                viewportSizePx = scrollController.viewportWidthPx,
            ),
            frameDurationNanos = frameDurationNanos,
        ),
        y = resolveTouchAutoScrollDeltaComponent(
            overflowPx = resolveTouchAutoScrollOverflowPx(
                positionPx = viewportPosition.y,
                viewportSizePx = scrollController.viewportHeightPx,
            ),
            frameDurationNanos = frameDurationNanos,
        ),
    )
}

private fun resolveTouchAutoScrollOverflowPx(
    positionPx: Float,
    viewportStartPx: Float = 0f,
    viewportSizePx: Float,
): Float {
    val viewportEndPx = viewportStartPx + viewportSizePx
    return when {
        positionPx < viewportStartPx -> positionPx - viewportStartPx
        positionPx > viewportEndPx -> positionPx - viewportEndPx
        else -> 0f
    }
}

private fun resolveTouchAutoScrollDeltaComponent(
    overflowPx: Float,
    frameDurationNanos: Long,
): Float {
    if (overflowPx == 0f) return 0f

    val safeFrameDurationNanos = frameDurationNanos.coerceIn(
        MIN_TOUCH_AUTO_SCROLL_FRAME_DURATION_NANOS,
        MAX_TOUCH_AUTO_SCROLL_FRAME_DURATION_NANOS,
    )
    val normalizedOverflow = (abs(overflowPx) / TOUCH_AUTO_SCROLL_ACCELERATION_DISTANCE_PX)
        .coerceIn(0f, 1f)
    val velocityPxPerSecond = TOUCH_AUTO_SCROLL_MIN_SPEED_PX_PER_SECOND +
        (TOUCH_AUTO_SCROLL_MAX_SPEED_PX_PER_SECOND - TOUCH_AUTO_SCROLL_MIN_SPEED_PX_PER_SECOND) *
        normalizedOverflow.pow(TOUCH_AUTO_SCROLL_ACCELERATION_EXPONENT)
    val deltaPx = velocityPxPerSecond * safeFrameDurationNanos / 1_000_000_000f
    return deltaPx * overflowPx.sign
}

internal const val DEFAULT_TOUCH_AUTO_SCROLL_FRAME_DURATION_NANOS: Long = 16_000_000L

private const val MIN_TOUCH_AUTO_SCROLL_FRAME_DURATION_NANOS: Long = 1_000_000L
private const val MAX_TOUCH_AUTO_SCROLL_FRAME_DURATION_NANOS: Long = 32_000_000L
private const val TOUCH_AUTO_SCROLL_ACCELERATION_DISTANCE_PX: Float = 72f
private const val TOUCH_AUTO_SCROLL_ACCELERATION_EXPONENT: Float = 1.25f
private const val TOUCH_AUTO_SCROLL_MIN_SPEED_PX_PER_SECOND: Float = 96f
private const val TOUCH_AUTO_SCROLL_MAX_SPEED_PX_PER_SECOND: Float = 1800f
