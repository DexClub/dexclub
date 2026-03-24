package io.github.dexclub.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

internal val LocalThemeIsDark = compositionLocalOf {
    mutableStateOf(true)
}

@Composable
fun rememberAppThemeIsDarkState(): MutableState<Boolean> {
    val systemIsDark = isSystemInDarkTheme()
    return remember(systemIsDark) {
        mutableStateOf(systemIsDark)
    }
}

@Composable
expect fun App(
    themeIsDarkState: MutableState<Boolean>,
)
