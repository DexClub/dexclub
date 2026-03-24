package io.github.dexclub.android

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import io.github.dexclub.app.App
import io.github.dexclub.app.rememberAppThemeIsDarkState
import io.github.shadcn.ui.compose.ShadcnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeIsDarkState = rememberAppThemeIsDarkState()
            ThemeChanged(!themeIsDarkState.value)
            ShadcnTheme(
                isDarkTheme = themeIsDarkState.value,
            ) {
                App(
                    themeIsDarkState = themeIsDarkState,
                )
            }
        }
    }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isDark
            isAppearanceLightNavigationBars = isDark
        }
    }
}
