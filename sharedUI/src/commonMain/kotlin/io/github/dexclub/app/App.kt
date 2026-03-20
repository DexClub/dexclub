package io.github.dexclub.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

internal val LocalThemeIsDark = compositionLocalOf {
    mutableStateOf(true)
}

@Composable
expect fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit,
)