package io.github.dexclub.codeview.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dexclub.codeview.compose.internal.editor.CodeEditorInputAnchorState
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import kotlinx.coroutines.CoroutineScope

internal interface PlatformEditorBridge {
    val useFloatingInputAnchor: Boolean
    val useTouchSelectionGestures: Boolean

    fun Modifier.bindEditorInput(
        fieldValue: TextFieldValue,
        layoutSnapshot: CodeLayoutSnapshot,
        clipboard: Clipboard,
        scope: CoroutineScope,
        preferredColumn: Int?,
        onPreferredColumnChange: (Int?) -> Unit,
        onInterruptInputAnchor: () -> Unit,
        onFieldValueChange: (TextFieldValue) -> Unit,
    ): Modifier

    fun requestInputFocus(focusRequester: FocusRequester)

    @Composable
    fun InputHost(
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
    )
}

@Composable
internal expect fun rememberPlatformEditorBridge(): PlatformEditorBridge
