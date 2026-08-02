package io.github.dexclub.codeview.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dexclub.codeview.compose.internal.editor.CodeEditorInputAnchorState
import io.github.dexclub.codeview.compose.internal.editor.codeEditorCommandKeyInput
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import java.awt.Container
import java.awt.KeyboardFocusManager
import java.awt.Window
import javax.swing.RootPaneContainer
import kotlinx.coroutines.CoroutineScope

@Composable
internal actual fun rememberPlatformEditorBridge(): PlatformEditorBridge {
    return remember { JvmPlatformEditorBridge }
}

private object JvmPlatformEditorBridge : PlatformEditorBridge {
    override val useFloatingInputAnchor: Boolean = true
    override val useTouchSelectionGestures: Boolean = false
    override fun isSoftwareKeyboardVisible(): Boolean = false

    // Desktop IME is driven by dedicated AWT components rather than a hidden Compose text field.
    // In split view we can have multiple editors alive at once, so focus must target the host
    // bound to the requesting editor instead of a single global "latest" host.
    private val inputHostsByFocusRequester = mutableMapOf<FocusRequester, DesktopInputHostComponent>()

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
    ): Modifier {
        return codeEditorCommandKeyInput(
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
    }

    override fun requestInputFocus(focusRequester: FocusRequester) {
        inputHostsByFocusRequester[focusRequester]?.requestFocusInWindow()
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
        val inputHost = remember { DesktopInputHostComponent() }
        var boundsInWindow = remember { Rect.Zero }

        DisposableEffect(inputHost, focusRequester) {
            inputHostsByFocusRequester[focusRequester] = inputHost

            onDispose {
                inputHostsByFocusRequester.remove(focusRequester, inputHost)
                detachInputHostFromWindow(inputHost)
            }
        }

        SideEffect {
            syncInputHostAttachment(inputHost = inputHost)
            inputHost.updateSession(
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
            inputHost.updateWindowBounds(boundsInWindow)
        }

        Box(
            modifier = modifier.onGloballyPositioned { coordinates ->
                boundsInWindow = coordinates.boundsInWindow()
                inputHost.updateWindowBounds(boundsInWindow)
            },
        )
    }
}

private fun syncInputHostAttachment(inputHost: DesktopInputHostComponent) {
    attachInputHostToWindow(
        window = currentActiveAwtWindow(),
        inputHost = inputHost,
    )
}

private fun attachInputHostToWindow(window: Window?, inputHost: DesktopInputHostComponent) {
    val container = window.awtInputHostContainer() ?: return
    val currentParent = inputHost.parent
    if (currentParent != null && currentParent !== container) {
        currentParent.remove(inputHost)
        currentParent.revalidate()
        currentParent.repaint()
    }
    if (inputHost.parent !== container) {
        container.add(inputHost)
        container.revalidate()
        container.repaint()
    }
}

private fun detachInputHostFromWindow(inputHost: DesktopInputHostComponent) {
    val parent = inputHost.parent ?: return
    parent.remove(inputHost)
    parent.revalidate()
    parent.repaint()
}

private fun Window?.awtInputHostContainer(): Container? {
    return when (this) {
        is RootPaneContainer -> layeredPane
        else -> this
    }
}

private fun currentActiveAwtWindow(): Window? {
    return KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
}
