package io.github.dexclub.app.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.Card
import io.github.shadcn.ui.compose.LinearProgress
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.ext.noRippleClickable

@Composable
fun Loading(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(ShadcnTheme.colors.foreground.copy(alpha = 0.6f))
            .noRippleClickable {
                /* 禁止点击事件穿透 */
            },
    ) {
        Card(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.widthIn(max = 350.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LinearProgress(
                    progress = null,
                    modifier = Modifier,
                )
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        style = ShadcnTheme.textStyles.labelMedium.copy(
                            color = ShadcnTheme.colors.primary.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}