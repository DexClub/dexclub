package io.github.dexclub.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.dexclub.app.navigation.Scenes
import io.github.dexclub.app.scene.home.HomeScreen
import io.github.dexclub.app.scene.home.HomeLaunchAction
import io.github.dexclub.app.scene.workspace.WorkspaceScene
import io.github.shadcn.ui.compose.SonnerBox

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
            BackHandler(enabled = backStack.size > 1) {
                backStack.removeLastOrNull()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize(),
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
}
