package io.github.dexclub.codeview.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@Composable
internal actual fun rememberPlatformImeBottomInsetPx(): Float {
    return WindowInsets.ime.getBottom(LocalDensity.current).toFloat()
}
