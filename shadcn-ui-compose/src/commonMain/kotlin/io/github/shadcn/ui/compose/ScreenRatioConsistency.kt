package io.github.shadcn.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density

@Composable
fun rememberScaleFactor(designWidthDp: Float = 360f): Float {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    return remember(windowInfo.containerSize, density.density) {
        val containerWidthPx = windowInfo.containerSize.width
        val containerWidthDp = containerWidthPx / density.density
        containerWidthDp / designWidthDp
    }
}

@Composable
fun ScreenRatioConsistency(
    enabled: Boolean = true,
    designWidthDp: Float = 360f,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    val oldDensity = LocalDensity.current
    val scaleFactor = rememberScaleFactor(designWidthDp)
    val newDensity = remember(oldDensity, scaleFactor) {
        Density(
            density = oldDensity.density * scaleFactor,
            fontScale = oldDensity.fontScale * scaleFactor
        )
    }
    CompositionLocalProvider(LocalDensity provides newDensity) {
        content()
    }
}