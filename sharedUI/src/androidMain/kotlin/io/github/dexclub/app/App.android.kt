package io.github.dexclub.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.dexclub.app.navigation.Scenes
import io.github.dexclub.app.scene.home.HomeScreen
import io.github.dexclub.app.scene.workspace.WorkspaceScene
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.SonnerBox

@Composable
actual fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit,
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
                BackHandler(enabled = backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        },
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
}
