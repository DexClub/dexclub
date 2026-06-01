import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.LocalShadcnTextStyles
import org.jetbrains.jewel.intui.window.styling.defaults
import org.jetbrains.jewel.intui.window.styling.lightWithLightHeader
import org.jetbrains.jewel.window.DecoratedWindowScope
import org.jetbrains.jewel.window.TitleBar
import org.jetbrains.jewel.window.styling.TitleBarColors
import org.jetbrains.jewel.window.styling.TitleBarMetrics
import org.jetbrains.jewel.window.styling.TitleBarStyle

@Composable
fun DecoratedWindowScope.DexClubTitleBar() {
    TitleBar(
        style = TitleBarStyle.lightWithLightHeader(
            colors = TitleBarColors.lightWithLightHeader(
                backgroundColor = Color.White,
                borderColor = Color.White,
            ),
            metrics = TitleBarMetrics.defaults(
                height = 34.dp,
            )
        ),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 24.dp),
        ) {
            Text(
                text = title,
                style = LocalShadcnTextStyles.current.titleSmall,
            )
        }
    }
}