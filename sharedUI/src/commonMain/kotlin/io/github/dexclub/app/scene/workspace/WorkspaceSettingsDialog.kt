package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.Card
import io.github.shadcn.ui.compose.Checkbox
import io.github.shadcn.ui.compose.Dialog
import io.github.shadcn.ui.compose.OutlineButton
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.Slider

@Composable
internal fun WorkspaceSettingsDialog(
    uiState: WorkspaceSettingsUiState,
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onSmaliUnicodeDecodeChange: (Boolean) -> Unit,
    onJavaUnicodeDecodeChange: (Boolean) -> Unit,
    onCodeScrollPastEndChange: (Int) -> Unit,
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        BoxWithConstraints(
            contentAlignment = androidx.compose.ui.Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            val dialogWidth = (maxWidth * 0.92f).coerceAtMost(360.dp)

            Card(
                contentPadding = PaddingValues(24.dp),
                modifier = Modifier.width(dialogWidth),
            ) {
                Column {
                    Text(
                        text = "设置",
                        style = ShadcnTheme.textStyles.titleLarge,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Checkbox(
                        checked = uiState.smaliUnicodeDecodeEnabled,
                        onCheckedChange = onSmaliUnicodeDecodeChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Column {
                                Text(
                                    text = "Smali Unicode 解码",
                                    style = ShadcnTheme.textStyles.bodySmall,
                                )
                                Text(
                                    text = "导出 Smali 时将 \\uXXXX 转为实际字符",
                                    style = ShadcnTheme.textStyles.labelMedium.copy(
                                        color = ShadcnTheme.colors.primary.copy(alpha = 0.6f),
                                    ),
                                )
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Checkbox(
                        checked = uiState.javaUnicodeDecodeEnabled,
                        onCheckedChange = onJavaUnicodeDecodeChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Column {
                                Text(
                                    text = "Java Unicode 解码",
                                    style = ShadcnTheme.textStyles.bodySmall,
                                )
                                Text(
                                    text = "导出 Java 时将 \\uXXXX 转为实际字符",
                                    style = ShadcnTheme.textStyles.labelMedium.copy(
                                        color = ShadcnTheme.colors.primary.copy(alpha = 0.6f),
                                    ),
                                )
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Scroll Past End",
                            style = ShadcnTheme.textStyles.bodySmall,
                        )
                        Text(
                            text = "底部额外预留 ${uiState.codeScrollPastEnd} 行空白，0 表示关闭",
                            style = ShadcnTheme.textStyles.labelMedium.copy(
                                color = ShadcnTheme.colors.primary.copy(alpha = 0.6f),
                            ),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Slider(
                            value = uiState.codeScrollPastEnd.coerceIn(0, MAX_SCROLL_PAST_END_LINES).toFloat() /
                                MAX_SCROLL_PAST_END_LINES.toFloat(),
                            onValueChange = { fraction ->
                                onCodeScrollPastEndChange(
                                    (fraction * MAX_SCROLL_PAST_END_LINES)
                                        .toInt()
                                        .coerceIn(0, MAX_SCROLL_PAST_END_LINES),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        OutlineButton(
                            onClick = onDismissRequest,
                        ) {
                            Text(
                                text = "关闭",
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val MAX_SCROLL_PAST_END_LINES: Int = 20
