import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.dexclub.Env
import io.github.dexclub.app.App
import io.github.dexclub.app.LocalComposeWindow
import io.github.dexclub.app.rememberAppThemeIsDarkState
import io.github.dexclub.desktop.generated.resources.Res
import io.github.dexclub.desktop.generated.resources.dexclub_icon
import io.github.shadcn.ui.compose.ShadcnTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.jewel.intui.window.styling.light
import org.jetbrains.jewel.window.DecoratedWindow
import org.jetbrains.jewel.window.styling.DecoratedWindowStyle
import java.awt.Dimension

fun main() {
    Env.onInit()

    application {
        val themeIsDarkState = rememberAppThemeIsDarkState()
        ShadcnTheme(
            isDarkTheme = themeIsDarkState.value,
        ) {
            val appIcon = painterResource(Res.drawable.dexclub_icon)
            DecoratedWindow(
                onCloseRequest = ::exitApplication,
                title = "DexClub",
                icon = appIcon,
                state = rememberWindowState(
                    width = 900.dp,
                    height = 720.dp,
                    position = WindowPosition.Aligned(Alignment.Center),
                ),
                style = DecoratedWindowStyle.light()
            ) {
                CompositionLocalProvider(LocalComposeWindow provides window) {
                    window.minimumSize = Dimension(900, 720)

                    DexClubTitleBar()

                    App(
                        themeIsDarkState = themeIsDarkState,
                    )
                }
            }
        }
    }
}

