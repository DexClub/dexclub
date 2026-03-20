package io.github.shadcn.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Immutable
actual class DialogProperties actual constructor(
    dismissOnBackPress: Boolean,
    dismissOnClickOutside: Boolean,
    usePlatformDefaultWidth: Boolean,
    usePlatformInsets: Boolean,
    useSoftwareKeyboardInset: Boolean,
    scrimColor: Color
) {
    actual val dismissOnBackPress: Boolean = dismissOnBackPress
    actual val dismissOnClickOutside: Boolean = dismissOnClickOutside
    actual val usePlatformDefaultWidth: Boolean = usePlatformDefaultWidth
    actual val usePlatformInsets: Boolean = usePlatformInsets
    actual val useSoftwareKeyboardInset: Boolean = useSoftwareKeyboardInset
    actual val scrimColor: Color = scrimColor
}

@Composable
actual fun Dialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    properties: DialogProperties,
    content: @Composable (() -> Unit)
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            usePlatformDefaultWidth = false,
            usePlatformInsets = properties.usePlatformInsets,
            useSoftwareKeyboardInset = properties.useSoftwareKeyboardInset,
            scrimColor = properties.scrimColor,
        ),
    ) {
        val sonnerState = rememberSonnerState()
        CompositionLocalProvider(LocalSonner provides sonnerState) {
            SonnerBox(
                state = sonnerState,
                modifier = Modifier.imePadding(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    content()
                }
            }
        }
    }
}