package io.github.dexclub.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.dexclub.app.navigation.Scenes
import io.github.dexclub.app.scene.home.HomeScreen
import io.github.dexclub.app.scene.workspace.WorkspaceScene
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.SonnerBox

val LocalComposeWindow = compositionLocalOf<ComposeWindow> {
    error("No Window provided")
}

@Composable
actual fun App(
    onThemeChanged: @Composable ((isDark: Boolean) -> Unit),
) {
    val systemIsDark = isSystemInDarkTheme()
    val isDarkState = remember(systemIsDark) { mutableStateOf(systemIsDark) }
    CompositionLocalProvider(
        LocalThemeIsDark provides isDarkState
    ) {
        val isDark by isDarkState
        onThemeChanged(!isDark)
        ShadcnTheme(
            isDarkTheme = isDark
        ) {
            SonnerBox {
                val backStack = remember { mutableStateListOf<Scenes>(Scenes.Home) }
                NavDisplay(
                    backStack = backStack,
                    onBack = { /* 不处理物理返回 */ },
                    entryProvider = entryProvider {
                        entry<Scenes.Home> { stack ->
                            HomeScreen(
                                onEnterWorkspace = { routeArgs ->
                                    backStack.add(Scenes.Workspace(routeArgs))
                                },
                            )
                        }

                        entry<Scenes.Workspace> { stack ->
                            WorkspaceScene(
                                onBackPressed = { backStack.removeLastOrNull() },
                                routeArgs = stack.args,
                            )
                        }
                    }
                )
            }
        }
    }
}
