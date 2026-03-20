package io.github.dexclub.core

import com.android.tools.smali.baksmali.Adaptors.ClassDefinition
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.util.DexUtil
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import io.github.dexclub.loggerWarn
import io.github.dexclub.utils.SignatureUtils
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.source
import io.github.vinceglb.filekit.write
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.api.impl.NoOpCodeCache
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import java.io.File
import java.io.StringWriter

class DexFactory(
    val dexs: MutableMap<String, DexFile>,
) {

    @Throws(IllegalArgumentException::class)
    suspend fun exportSingleDex(
        className: String,
        dex: PlatformFile,
        output: PlatformFile,
    ): String {
        if (className.trim().isEmpty()) {
            throw IllegalArgumentException("className must not be empty")
        }

        val typeSignature = SignatureUtils.typeSignature(className)
        val dexFile = dexs[dex.absolutePath()]
            ?: throw IllegalArgumentException("`${dex.absolutePath()}` file not found.")

        val findClassDef = dexFile.classes.find { it.type == typeSignature }
            ?: throw IllegalArgumentException("`$className` not found in `${dex.absolutePath()}`")

        val dataStore = MemoryDataStore()
        val dexPool = DexPool(Opcodes.getDefault())
        dexPool.internClass(findClassDef)
        dexPool.writeTo(dataStore)
        output.write(dataStore.data)

        return output.absolutePath()
    }

    suspend fun exportSingleSmali(
        autoUnicodeDecode: Boolean,
        className: String,
        dex: PlatformFile,
        output: PlatformFile,
    ): String {
        if (className.trim().isEmpty()) {
            throw IllegalArgumentException("className must not be empty")
        }

        val typeSignature = SignatureUtils.typeSignature(className)
        val dexFile = dexs[dex.absolutePath()]
            ?: throw IllegalArgumentException("`${dex.absolutePath()}` file not found.")

        val findClassDef = dexFile.classes.find { it.type == typeSignature }
            ?: throw IllegalArgumentException("`$className` not found in `${dex.absolutePath()}`")

        val options = BaksmaliOptions().apply {
            parameterRegisters = true
            localsDirective = true
            debugInfo = true
            accessorComments = true
        }

        val stringWriter = StringWriter()
        val baksmaliWriter = if (autoUnicodeDecode) {
            UnescapedUnicodeBaksmaliWriter(stringWriter)
        } else {
            BaksmaliWriter(stringWriter)
        }

        val classDefinition = ClassDefinition(options, findClassDef)
        classDefinition.writeTo(baksmaliWriter)

        baksmaliWriter.flush()
        output.write(stringWriter.toString().toByteArray(Charsets.UTF_8))

        return output.absolutePath()
    }

    suspend fun jadxDexToJavaSource(
        dex: PlatformFile,
        output: PlatformFile,
    ): String {
        val dexFile = File(dex.absolutePath())
        val javaFile = File(output.absolutePath())
        val outDir = javaFile.parentFile
            ?: throw IllegalArgumentException("output must have a parent directory")

        val args = JadxArgs()
        args.setInputFile(dexFile)
        args.outDir = outDir
        args.codeCache = NoOpCodeCache()
        args.isRenameValid = false
        args.isRenameCaseSensitive = true
        args.isShowInconsistentCode = false
        args.isDebugInfo = false
        args.isMoveInnerClasses = false
        args.isInlineAnonymousClasses = false

        JadxDecompiler(args).use { decompiler ->
            decompiler.load()
            val classes = decompiler.classesWithInners.filterNot { it.isNoCode }
            val javaClass = classes.singleOrNull()
                ?: throw IllegalStateException(
                    "Expected exactly one decompiled class from `${dex.absolutePath()}`, but got ${classes.size}",
                )
            javaFile.parentFile?.mkdirs()
            javaFile.writeText(javaClass.code, Charsets.UTF_8)
        }

        return javaFile.absolutePath
    }

    companion object {
        private const val TAG = "DexFactory"

        fun isDex(file: PlatformFile): Boolean {
            try {
                val buffer = Buffer()
                file.source().buffered().readAtMostTo(buffer, 44)
                DexUtil.verifyDexHeader(buffer.readByteArray(), 0)
            } catch (e: Exception) {
                return false
            }
            return true
        }

        fun loadMultiDex(dexFiles: List<PlatformFile>): DexFactory {
            val backedDexs = mutableMapOf<String, DexFile>()
            for (dex in dexFiles) {
                if (isDex(dex)) {
                    val byteArray = dex.source().buffered().readByteArray()
                    val backedDexFile = DexBackedDexFile(Opcodes.getDefault(), byteArray)
                    backedDexs[dex.absolutePath()] = backedDexFile
                } else {
                    loggerWarn(
                        text = "Dex 文件校验失败: ${dex.absolutePath()}",
                        tag = TAG,
                    )
                }
            }

            return DexFactory(backedDexs)
        }
    }
}
