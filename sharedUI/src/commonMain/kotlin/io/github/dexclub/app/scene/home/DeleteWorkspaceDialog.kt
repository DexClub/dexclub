package io.github.dexclub.app.scene.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.res.StringRes
import io.github.shadcn.ui.compose.Button
import io.github.shadcn.ui.compose.Card
import io.github.shadcn.ui.compose.Dialog
import io.github.shadcn.ui.compose.OutlineButton
import io.github.shadcn.ui.compose.ShadcnTheme

@Composable
fun DeleteConfirmDialog(
    model: HomeSceneViewModel,
    uiState: HomeUiState,
) {
    if (!uiState.deleteConfirmDialog) return

    Dialog(
        onDismissRequest = model::onCloseDeleteConfirmDialog,
    ) {
        Card(
            contentPadding = androidx.compose.foundation.layout.PaddingValues.Zero,
            modifier = Modifier.width(320.dp),
        ) {
            Column {
                Text(
                    text = buildAnnotatedString {
                        val split = StringRes.current.deleteWorkspaceMessage.split(" ")
                        append(split[0])
                        append(" ")
                        withStyle(SpanStyle(background = ShadcnTheme.colors.sidebarBorder)) {
                            append(split[1].format(uiState.selectedWorkspaceItem!!.name))
                        }
                        append(" ")
                        append(split[2])
                    },
                    style = ShadcnTheme.textStyles.bodyMedium,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                )

                Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ShadcnTheme.colors.border)
                        .background(ShadcnTheme.colors.muted.copy(0.5f))
                        .padding(16.dp, 12.dp),
                ) {
                    OutlineButton(
                        onClick = model::onCloseDeleteConfirmDialog,
                    ) {
                        Text(
                            text = StringRes.current.cancel,
                        )
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Button(
                        onClick = model::onDeleteConfirm,
                    ) {
                        Text(
                            text = StringRes.current.delete,
                        )
                    }
                }
            }
        }
    }
}
