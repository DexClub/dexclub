package io.github.shadcn.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Immutable
expect class DialogProperties(
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    usePlatformDefaultWidth: Boolean = true,
    usePlatformInsets: Boolean = true,
    useSoftwareKeyboardInset: Boolean = true,
    scrimColor: Color = Color.Black.copy(0.6f),
) {
    val dismissOnBackPress: Boolean
    val dismissOnClickOutside: Boolean
    val usePlatformDefaultWidth: Boolean
    val usePlatformInsets: Boolean
    val useSoftwareKeyboardInset: Boolean
    val scrimColor: Color
}

@Composable
expect fun Dialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
)