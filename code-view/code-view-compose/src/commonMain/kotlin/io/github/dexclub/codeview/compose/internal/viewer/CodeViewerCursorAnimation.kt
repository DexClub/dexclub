package io.github.dexclub.codeview.compose.internal.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

@Composable
internal fun rememberCodeViewerCursorAlpha(
    cursorVisible: Boolean,
    resetKey: Any?,
): Float {
    val cursorAlpha = remember { Animatable(1f) }

    LaunchedEffect(cursorVisible, resetKey) {
        if (!cursorVisible) {
            cursorAlpha.snapTo(1f)
            return@LaunchedEffect
        }

        cursorAlpha.snapTo(1f)
        while (true) {
            delay(320L)
            cursorAlpha.animateTo(
                targetValue = 0.18f,
                animationSpec = tween(durationMillis = 520, easing = EaseInOutSine),
            )
            cursorAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = EaseInOutSine),
            )
        }
    }

    return cursorAlpha.value
}
