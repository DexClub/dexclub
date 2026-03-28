package io.github.dexclub.core.session

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.DexFile
import io.github.dexclub.core.input.DexInputInspector
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.source
import kotlinx.io.buffered
import kotlinx.io.readByteArray

internal object DexSessionLoader {
    private const val TAG = "DexSessionLoader"

    fun loadMultiDex(dexFiles: List<PlatformFile>): DexSession {
        val backedDexs = mutableMapOf<String, DexFile>()
        for (dex in dexFiles) {
            if (DexInputInspector.isDex(dex)) {
                val byteArray = dex.source().buffered().readByteArray()
                val backedDexFile = DexBackedDexFile(Opcodes.getDefault(), byteArray)
                backedDexs[dex.absolutePath()] = backedDexFile
            } else {
                System.err.println("[$TAG] Dex 文件校验失败: ${dex.absolutePath()}")
            }
        }
        return DexSession(backedDexs)
    }
}
