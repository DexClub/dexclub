package io.github.dexclub.codeview.compose

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import io.github.dexclub.codeview.compose.internal.editor.CodeEditorInputAnchorState
import io.github.dexclub.codeview.compose.internal.editor.codeEditorCommandKeyInput
import io.github.dexclub.codeview.compose.internal.editor.handleInputAnchorValueChange
import io.github.dexclub.codeview.compose.internal.editor.moveCaretHorizontally
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import kotlinx.coroutines.CoroutineScope

private const val INPUT_HOST_IDLE_PREFIX: String = "\u2060"
private const val INPUT_HOST_IDLE_SUFFIX: String = "\u2060"
private const val INPUT_HOST_IDLE_TEXT: String = INPUT_HOST_IDLE_PREFIX + INPUT_HOST_IDLE_SUFFIX
private const val INPUT_HOST_IDLE_CURSOR: Int = INPUT_HOST_IDLE_PREFIX.length

@Composable
internal actual fun rememberPlatformEditorBridge(): PlatformEditorBridge {
    return remember { AndroidPlatformEditorBridge }
}

private object AndroidPlatformEditorBridge : PlatformEditorBridge {
    override val useFloatingInputAnchor: Boolean = false
    override val useTouchSelectionGestures: Boolean = true
    private var latestRequestKeyboardFocus: (() -> Unit)? = null

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
        val requestKeyboardFocus = latestRequestKeyboardFocus
        if (requestKeyboardFocus != null) {
            requestKeyboardFocus()
            return
        }
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
        val keyboardController = LocalSoftwareKeyboardController.current
        val view = LocalView.current
        val inputHostValue = resolveAndroidInputHostValue(inputAnchorState)

        DisposableEffect(focusRequester, keyboardController, view) {
            val requestKeyboardFocus = {
                view.post {
                    try {
                        focusRequester.requestFocus()
                    } catch (_: Exception) {
                    }
                    keyboardController?.show()
                }
                Unit
            }
            latestRequestKeyboardFocus = requestKeyboardFocus

            onDispose {
                if (latestRequestKeyboardFocus === requestKeyboardFocus) {
                    latestRequestKeyboardFocus = null
                }
            }
        }

        BasicTextField(
            value = inputHostValue,
            onValueChange = { newValue ->
                handleAndroidInputHostValueChange(
                    inputAnchorState = inputAnchorState,
                    newValue = newValue,
                    currentHostValue = inputHostValue,
                    fieldValue = fieldValue,
                    onPreferredColumnChange = onPreferredColumnChange,
                    onFieldValueChange = onFieldValueChange,
                )
            },
            modifier = modifier
                .focusRequester(focusRequester)
                .codeEditorCommandKeyInput(
                    fieldValue = fieldValue,
                    layoutSnapshot = layoutSnapshot,
                    clipboard = clipboard,
                    scope = scope,
                    preferredColumn = preferredColumn,
                    onPreferredColumnChange = onPreferredColumnChange,
                    onInterruptInputAnchor = onInterruptInputAnchor,
                    onFieldValueChange = onFieldValueChange,
                )
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

private fun resolveAndroidInputHostValue(
    inputAnchorState: CodeEditorInputAnchorState,
): TextFieldValue {
    val imeFieldValue = inputAnchorState.imeFieldValue
    return if (imeFieldValue.text.isEmpty() && imeFieldValue.composition == null) {
        TextFieldValue(
            text = INPUT_HOST_IDLE_TEXT,
            selection = TextRange(INPUT_HOST_IDLE_CURSOR),
        )
    } else {
        imeFieldValue
    }
}

private fun handleAndroidInputHostValueChange(
    inputAnchorState: CodeEditorInputAnchorState,
    newValue: TextFieldValue,
    currentHostValue: TextFieldValue,
    fieldValue: TextFieldValue,
    onPreferredColumnChange: (Int?) -> Unit,
    onFieldValueChange: (TextFieldValue) -> Unit,
) {
    val isIdleHost =
        currentHostValue.text == INPUT_HOST_IDLE_TEXT &&
                currentHostValue.composition == null &&
                inputAnchorState.imeFieldValue.text.isEmpty() &&
                inputAnchorState.imeFieldValue.composition == null

    if (isIdleHost && newValue.text == INPUT_HOST_IDLE_TEXT && newValue.composition == null) {
        val delta = newValue.selection.end.compareTo(INPUT_HOST_IDLE_CURSOR)
        if (delta != 0) {
            onPreferredColumnChange(null)
            onFieldValueChange(
                moveCaretHorizontally(
                    fieldValue = fieldValue,
                    delta = delta,
                    extendSelection = false,
                )
            )
        }
        return
    }

    val actualValue = if (isIdleHost) {
        newValue.toAndroidImeFieldValue()
    } else {
        newValue
    }

    handleInputAnchorValueChange(
        inputAnchorState = inputAnchorState,
        newValue = actualValue,
        fieldValue = fieldValue,
        onPreferredColumnChange = onPreferredColumnChange,
        onFieldValueChange = onFieldValueChange,
    )
}

private fun TextFieldValue.toAndroidImeFieldValue(): TextFieldValue {
    if (
        text.length < INPUT_HOST_IDLE_PREFIX.length + INPUT_HOST_IDLE_SUFFIX.length ||
        !text.startsWith(INPUT_HOST_IDLE_PREFIX) ||
        !text.endsWith(INPUT_HOST_IDLE_SUFFIX)
    ) {
        return this
    }
    val actualText = text.substring(
        INPUT_HOST_IDLE_PREFIX.length,
        text.length - INPUT_HOST_IDLE_SUFFIX.length,
    )
    return TextFieldValue(
        text = actualText,
        selection = selection.shiftAndClamp(-INPUT_HOST_IDLE_PREFIX.length, actualText.length),
        composition = composition?.shiftAndClamp(-INPUT_HOST_IDLE_PREFIX.length, actualText.length),
    )
}

private fun TextRange.shiftAndClamp(delta: Int, textLength: Int): TextRange {
    return TextRange(
        start = (start + delta).coerceIn(0, textLength),
        end = (end + delta).coerceIn(0, textLength),
    )
}
