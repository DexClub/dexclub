package io.github.dexclub.codeview.compose

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.onFocusChanged
import io.github.dexclub.codeview.compose.internal.editor.CodeEditorInputAnchorState
import io.github.dexclub.codeview.compose.internal.editor.codeEditorCommandKeyInput
import io.github.dexclub.codeview.compose.internal.editor.handleInputAnchorValueChange
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import kotlinx.coroutines.CoroutineScope

// 调试开关：用于临时观察 Desktop 输入锚点是否真正收到输入事件。
// 保留该常量，后续如需继续排查 IME 会话问题可直接打开；不要直接删除。
private const val SHOW_INPUT_ANCHOR_TEXT_DEBUG: Boolean = false

@Composable
internal actual fun rememberPlatformEditorBridge(): PlatformEditorBridge {
    return remember { JvmPlatformEditorBridge }
}

private object JvmPlatformEditorBridge : PlatformEditorBridge {
    override val useFloatingInputAnchor: Boolean = true

    override fun Modifier.bindEditorInput(
        fieldValue: TextFieldValue,
        layoutSnapshot: CodeLayoutSnapshot,
        clipboard: Clipboard,
        scope: CoroutineScope,
        preferredColumn: Int?,
        onPreferredColumnChange: (Int?) -> Unit,
        onInterruptInputAnchor: () -> Unit,
        onFieldValueChange: (TextFieldValue) -> Unit,
    ): Modifier {
        return codeEditorCommandKeyInput(
            fieldValue = fieldValue,
            layoutSnapshot = layoutSnapshot,
            clipboard = clipboard,
            scope = scope,
            preferredColumn = preferredColumn,
            onPreferredColumnChange = onPreferredColumnChange,
            onInterruptInputAnchor = onInterruptInputAnchor,
            onFieldValueChange = onFieldValueChange,
        )
    }

    override fun requestInputFocus(focusRequester: FocusRequester) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    @Composable
    override fun InputHost(
        modifier: Modifier,
        inputAnchorState: CodeEditorInputAnchorState,
        fieldValue: TextFieldValue,
        layoutSnapshot: CodeLayoutSnapshot,
        clipboard: Clipboard,
        scope: CoroutineScope,
        preferredColumn: Int?,
        onPreferredColumnChange: (Int?) -> Unit,
        onInterruptInputAnchor: () -> Unit,
        textStyle: TextStyle,
        focusRequester: FocusRequester,
        onFieldValueChange: (TextFieldValue) -> Unit,
    ) {
        BasicTextField(
            value = inputAnchorState.imeFieldValue,
            onValueChange = { newValue ->
                if (SHOW_INPUT_ANCHOR_TEXT_DEBUG) {
                    println(
                        "[code-view][desktop-ime] text='${newValue.text}' composition=${newValue.composition} selection=${newValue.selection}"
                    )
                }
                handleInputAnchorValueChange(
                    inputAnchorState = inputAnchorState,
                    newValue = newValue,
                    fieldValue = fieldValue,
                    onPreferredColumnChange = onPreferredColumnChange,
                    onFieldValueChange = onFieldValueChange,
                )
            },
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (!it.isFocused) {
                        onInterruptInputAnchor()
                    }
                }
                .codeEditorCommandKeyInput(
                    fieldValue = fieldValue,
                    layoutSnapshot = layoutSnapshot,
                    clipboard = clipboard,
                    scope = scope,
                    preferredColumn = preferredColumn,
                    onPreferredColumnChange = onPreferredColumnChange,
                    onInterruptInputAnchor = onInterruptInputAnchor,
                    onFieldValueChange = onFieldValueChange,
                ),
            readOnly = false,
            singleLine = true,
            textStyle = CodeViewDefaults.CodeTextStyle
                .merge(textStyle)
                .copy(
                    color = if (SHOW_INPUT_ANCHOR_TEXT_DEBUG) {
                        Color.Red.copy(alpha = 0.55f)
                    } else {
                        Color.Transparent
                    },
                ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.None,
            ),
            cursorBrush = SolidColor(Color.Transparent),
        )
    }
}
