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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import io.github.dexclub.codeview.compose.internal.editor.CodeEditorInputAnchorState
import io.github.dexclub.codeview.compose.internal.editor.handleInputAnchorValueChange
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import kotlinx.coroutines.CoroutineScope

@Composable
internal actual fun rememberPlatformEditorBridge(): PlatformEditorBridge {
    return remember { AndroidPlatformEditorBridge }
}

private object AndroidPlatformEditorBridge : PlatformEditorBridge {
    override val useFloatingInputAnchor: Boolean = false

    override fun Modifier.bindEditorInput(
        fieldValue: TextFieldValue,
        layoutSnapshot: CodeLayoutSnapshot,
        clipboard: Clipboard,
        scope: CoroutineScope,
        preferredColumn: Int?,
        onPreferredColumnChange: (Int?) -> Unit,
        onInterruptInputAnchor: () -> Unit,
        onFieldValueChange: (TextFieldValue) -> Unit,
    ): Modifier = this

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
                .layout { measurable, _ ->
                    val placeable = measurable.measure(Constraints.fixed(0, 0))
                    layout(0, 0) {
                        placeable.place(0, 0)
                    }
                },
            readOnly = false,
            singleLine = false,
            textStyle = CodeViewDefaults.CodeTextStyle
                .merge(textStyle)
                .copy(color = Color.Transparent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.None,
            ),
            cursorBrush = SolidColor(Color.Transparent),
        )
    }
}
