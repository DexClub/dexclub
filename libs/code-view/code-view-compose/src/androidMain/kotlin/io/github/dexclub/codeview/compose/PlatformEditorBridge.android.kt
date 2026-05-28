package io.github.dexclub.codeview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.viewinterop.AndroidView
import io.github.dexclub.codeview.compose.internal.editor.CodeEditorInputAnchorState
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import kotlinx.coroutines.CoroutineScope

@Composable
internal actual fun rememberPlatformEditorBridge(): PlatformEditorBridge {
    return remember { AndroidPlatformEditorBridge }
}

private object AndroidPlatformEditorBridge : PlatformEditorBridge {
    override val useFloatingInputAnchor: Boolean = false
    override val useTouchSelectionGestures: Boolean = true
    private var latestInputHost: AndroidInputHostView? = null

    override fun isSoftwareKeyboardVisible(): Boolean {
        return latestInputHost?.isSoftwareKeyboardVisible() == true
    }

    override fun Modifier.bindEditorInput(
        fieldValue: TextFieldValue,
        layoutSnapshot: CodeLayoutSnapshot,
        clipboard: Clipboard,
        scope: CoroutineScope,
        preferredColumn: Int?,
        onPreferredColumnChange: (Int?) -> Unit,
        onInterruptInputAnchor: () -> Unit,
        onFindRequested: (String) -> Unit,
        onFieldValueChange: (TextFieldValue) -> Unit,
    ): Modifier = this

    override fun requestInputFocus(focusRequester: FocusRequester) {
        val inputHost = latestInputHost
        if (inputHost != null) {
            inputHost.requestEditorFocus()
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
        onFindRequested: (String) -> Unit,
        onFieldValueChange: (TextFieldValue) -> Unit,
    ) {
        val context = LocalContext.current
        val inputHost = remember(context) { AndroidInputHostView(context = context) }

        DisposableEffect(inputHost) {
            latestInputHost = inputHost

            onDispose {
                if (latestInputHost === inputHost) {
                    latestInputHost = null
                }
            }
        }

        AndroidView(
            factory = { inputHost },
            modifier = modifier.layout { measurable, _ ->
                val placeable = measurable.measure(Constraints.fixed(1, 1))
                layout(1, 1) {
                    placeable.place(0, 0)
                }
            },
            update = { view ->
                view.updateSession(
                    inputAnchorState = inputAnchorState,
                    fieldValue = fieldValue,
                    layoutSnapshot = layoutSnapshot,
                    clipboard = clipboard,
                    scope = scope,
                    preferredColumn = preferredColumn,
                    onPreferredColumnChange = onPreferredColumnChange,
                    onInterruptInputAnchor = onInterruptInputAnchor,
                    onFindRequested = onFindRequested,
                    onFieldValueChange = onFieldValueChange,
                )
            },
        )
    }
}
