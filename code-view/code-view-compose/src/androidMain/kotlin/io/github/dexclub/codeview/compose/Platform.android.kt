package io.github.dexclub.codeview.compose

import android.content.ClipData
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

internal actual suspend fun Clipboard.copyText(text: CharSequence, label: CharSequence) {
    val entry = ClipEntry(ClipData.newPlainText(label, text))
    setClipEntry(entry)
}

internal actual suspend fun Clipboard.pasteText(): String? {
    val entry = getClipEntry() ?: return null
    val clip = entry.clipData
    return clip.getItemAt(0)?.text?.toString()
}

internal actual fun isModifierKeyHeld(event: KeyEvent): Boolean = event.isCtrlPressed
