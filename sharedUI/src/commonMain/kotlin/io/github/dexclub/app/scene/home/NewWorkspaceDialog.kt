package io.github.dexclub.app.scene.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.compose.Loading
import io.github.dexclub.app.res.StringRes
import io.github.dexclub.compat.displayPath
import io.github.shadcn.ui.compose.Button
import io.github.shadcn.ui.compose.Card
import io.github.shadcn.ui.compose.Dialog
import io.github.shadcn.ui.compose.LocalSonner
import io.github.shadcn.ui.compose.OutlineButton
import io.github.shadcn.ui.compose.SelectField
import io.github.shadcn.ui.compose.SelectFieldColor
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.TextField
import io.github.shadcn.ui.compose.TextFieldColor
import io.github.shadcn.ui.compose.defaultSelectFieldColors
import io.github.shadcn.ui.compose.defaultTextFieldColors
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.launch

@Composable
fun NewWorkspaceDialog(
    model: HomeSceneViewModel,
    uiState: HomeUiState,
) {
    if (!uiState.newWorkspaceDialog) return

    val scope = rememberCoroutineScope()
    val workspaceNameFocusRequester = remember { FocusRequester() }
    var isWorkspaceNameError by remember { mutableStateOf(false) }
    var workspaceName by remember { mutableStateOf(TextFieldValue(text = StringRes.current.workspaceName)) }

    var isTargetFileError by remember { mutableStateOf(false) }
    var selectedTargetFile by remember { mutableStateOf<PlatformFile?>(null) }

    Dialog(
        onDismissRequest = model::onCloseNewWorkspaceDialog,
    ) {
        val sonnerState = LocalSonner.current

        BoxWithConstraints(
            contentAlignment = androidx.compose.ui.Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            val dialogWidth = (maxWidth * 0.96f).coerceAtMost(420.dp)

            Card(
                contentPadding = PaddingValues.Zero,
                modifier = Modifier.width(dialogWidth),
            ) {
                Column {
                    Text(
                        text = StringRes.current.createWorkspace,
                        style = ShadcnTheme.textStyles.titleLarge,
                        modifier = Modifier.padding(24.dp, 24.dp, 24.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = StringRes.current.projectName,
                        style = ShadcnTheme.textStyles.labelLarge,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 8.dp),
                    )
                    TextField(
                        value = workspaceName,
                        placeholder = StringRes.current.inputProjectNamePlaceholder,
                        onValueChange = {
                            isWorkspaceNameError = false
                            workspaceName = it.copy(it.text.trim())
                        },
                        colors = if (isWorkspaceNameError) {
                            defaultTextFieldColors(TextFieldColor.Error)
                        } else {
                            defaultTextFieldColors()
                        },
                        maxLines = 1,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth()
                            .focusRequester(workspaceNameFocusRequester),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = StringRes.current.targetFile,
                        style = ShadcnTheme.textStyles.labelLarge,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 8.dp),
                    )

                    SelectField(
                        colors = if (isTargetFileError) {
                            defaultSelectFieldColors(SelectFieldColor.Error)
                        } else {
                            defaultSelectFieldColors()
                        },
                        onClick = {
                            isTargetFileError = false
                            PickerResultLauncher {
                                scope.launch {
                                    val file = FileKit.openFilePicker(
                                        type = FileKitType.File("apk", "dex"),
                                        dialogSettings = FileKitDialogSettings.createDefault(),
                                    )
                                    if (file != null) {
                                        selectedTargetFile = file
                                    }
                                }
                            }.launch()
                        },
                    ) {
                        Text(
                            text = selectedTargetFile?.displayPath ?: StringRes.current.selectTargetFilePlaceholder,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = ShadcnTheme.textStyles.bodyMedium.copy(
                                color = if (selectedTargetFile != null) {
                                    ShadcnTheme.colors.primary
                                } else {
                                    ShadcnTheme.textStyles.bodyMedium.color.copy(alpha = 0.6f)
                                },
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ShadcnTheme.colors.border)
                            .background(ShadcnTheme.colors.muted.copy(0.5f))
                            .padding(16.dp, 12.dp),
                    ) {
                        OutlineButton(
                            onClick = model::onCloseNewWorkspaceDialog,
                        ) {
                            Text(
                                text = StringRes.current.cancel,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (workspaceName.text.isEmpty()) {
                                    isWorkspaceNameError = true
                                    workspaceNameFocusRequester.requestFocus()
                                    sonnerState.sonner(StringRes.current.inputProjectNamePlaceholder)
                                    return@Button
                                }

                                if (selectedTargetFile == null) {
                                    isTargetFileError = true
                                    sonnerState.sonner(StringRes.current.selectTargetFilePlaceholder)
                                    return@Button
                                }
                                model.onNew(workspaceName.text, selectedTargetFile)
                            },
                        ) {
                            Text(
                                text = StringRes.current.confirm,
                            )
                        }
                    }
                }
            }
        }

        if (uiState.loading) {
            Loading(uiState.loadingMessage)
        }
    }
}
