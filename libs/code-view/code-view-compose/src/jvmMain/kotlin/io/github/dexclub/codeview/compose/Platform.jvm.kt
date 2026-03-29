package io.github.dexclub.codeview.compose

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalComposeUiApi::class)
internal actual suspend fun Clipboard.copyText(text: CharSequence, label: CharSequence) {
    val entry = ClipEntry(StringSelection(text.toString()))
    setClipEntry(entry)
}

@OptIn(ExperimentalComposeUiApi::class)
internal actual suspend fun Clipboard.pasteText(): String? {
    val entry = getClipEntry() ?: return null
    val transferable = entry.nativeClipEntry as? Transferable ?: return null
    return if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        withContext(Dispatchers.IO) { transferable.getTransferData(DataFlavor.stringFlavor) } as? String
    } else {
        null
    }
}

private val isMac: Boolean = System.getProperty("os.name")?.contains("Mac", ignoreCase = true) == true

internal actual fun isModifierKeyHeld(event: KeyEvent): Boolean =
    if (isMac) event.isMetaPressed else event.isCtrlPressed
