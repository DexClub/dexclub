package io.github.dexclub.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.dexclub.app.navigation.Scenes
import io.github.dexclub.app.scene.home.HomeScreen
import io.github.dexclub.app.scene.home.HomeLaunchAction
import io.github.dexclub.app.scene.workspace.WorkspaceScene
import io.github.shadcn.ui.compose.SonnerBox

val LocalComposeWindow = compositionLocalOf<ComposeWindow> {
    error("No Window provided")
}

@Composable
actual fun App(
    themeIsDarkState: MutableState<Boolean>,
) {
    CompositionLocalProvider(
        LocalThemeIsDark provides themeIsDarkState,
    ) {
        SonnerBox {
            val backStack = remember { mutableStateListOf<Scenes>(Scenes.Home) }
            val homeLaunchActionState = remember { mutableStateOf<HomeLaunchAction?>(null) }
            NavDisplay(
                backStack = backStack,
                onBack = { /* 不处理物理返回 */ },
                entryProvider = entryProvider {
                    entry<Scenes.Home> { stack ->
                        HomeScreen(
                            onEnterWorkspace = { routeArgs ->
                                backStack.add(Scenes.Workspace(routeArgs))
                            },
                            launchAction = homeLaunchActionState.value,
                            onLaunchActionConsumed = {
                                homeLaunchActionState.value = null
                            },
                        )
                    }

                    entry<Scenes.Workspace> { stack ->
                        WorkspaceScene(
                            onBackPressed = { backStack.removeLastOrNull() },
                            onRequestNavigateHome = {
                                backStack.clear()
                                backStack.add(Scenes.Home)
                            },
                            onRequestCreateWorkspace = {
                                homeLaunchActionState.value = HomeLaunchAction.CreateWorkspace
                                backStack.clear()
                                backStack.add(Scenes.Home)
                            },
                            onRequestOpenWorkspace = {
                                homeLaunchActionState.value = HomeLaunchAction.OpenWorkspace
                                backStack.clear()
                                backStack.add(Scenes.Home)
                            },
                            routeArgs = stack.args,
                        )
                    }
                }
            )
        }
    }
}
