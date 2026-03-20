import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.dexclub.Env
import io.github.dexclub.app.App
import io.github.dexclub.app.LocalComposeWindow
import java.awt.Dimension

fun main() {
    Env.onInit()

    application {
        Window(
            title = "DexClub",
            state = rememberWindowState(
                width = 900.dp,
                height = 720.dp,
                position = WindowPosition.Aligned(Alignment.Center),
            ),
            onCloseRequest = ::exitApplication,
        ) {
            window.minimumSize = Dimension(900, 720)
            CompositionLocalProvider(LocalComposeWindow provides window) {
                App(onThemeChanged = { })
            }
        }
    }
}

