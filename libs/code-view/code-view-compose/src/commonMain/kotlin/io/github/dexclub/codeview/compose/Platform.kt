package io.github.dexclub.codeview.compose

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.Clipboard

internal expect suspend fun Clipboard.copyText(text: CharSequence, label: CharSequence = "copy")

internal expect suspend fun Clipboard.pasteText(): String?

internal expect fun isModifierKeyHeld(event: KeyEvent): Boolean
