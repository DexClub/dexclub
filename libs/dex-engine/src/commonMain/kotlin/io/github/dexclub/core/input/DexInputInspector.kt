package io.github.dexclub.core.input

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.source
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.readByteArray

internal object DexInputInspector {
    fun isDex(file: PlatformFile): Boolean {
        try {
            val buffer = Buffer()
            val bytesRead = file.source().buffered().readAtMostTo(buffer, DEX_MAGIC_SIZE)
            if (bytesRead < DEX_MAGIC_SIZE) {
                return false
            }
            val header = buffer.readByteArray()
            return header[0] == 'd'.code.toByte() &&
                header[1] == 'e'.code.toByte() &&
                header[2] == 'x'.code.toByte() &&
                header[3] == '\n'.code.toByte() &&
                header[4].isAsciiDigit() &&
                header[5].isAsciiDigit() &&
                header[6].isAsciiDigit() &&
                header[7] == 0.toByte()
        } catch (_: Exception) {
            return false
        }
    }

    private fun Byte.isAsciiDigit(): Boolean {
        return this in '0'.code.toByte()..'9'.code.toByte()
    }

    private const val DEX_MAGIC_SIZE = 8L
}
