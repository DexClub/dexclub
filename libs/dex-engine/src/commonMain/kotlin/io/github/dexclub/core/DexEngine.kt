package io.github.dexclub.core

import io.github.dexclub.core.export.DexExportService
import io.github.dexclub.core.input.DexInputInspector
import io.github.dexclub.core.runtime.DexKitRuntime
import io.github.dexclub.core.source.DexIndexedClass
import io.github.dexclub.core.session.DexSessionLoader
import io.github.dexclub.dexkit.DexKitBridge
import io.github.dexclub.dexkit.findClass
import io.github.dexclub.dexkit.findMethod
import io.github.dexclub.dexkit.query.StringMatchType
import io.github.dexclub.dexkit.result.ClassData
import io.github.dexclub.dexkit.result.MethodData
import io.github.vinceglb.filekit.PlatformFile

class DexEngine(
    dexPaths: List<String>,
) : AutoCloseable {
    private val normalizedDexPaths = dexPaths
        .map(String::trim)
        .filter(String::isNotEmpty)
    private val dexFiles by lazy(LazyThreadSafetyMode.NONE) {
        normalizedDexPaths.map(::PlatformFile)
    }
    private val dexSession by lazy(LazyThreadSafetyMode.NONE) {
        DexSessionLoader.loadMultiDex(dexFiles)
    }
    private val dexExportService by lazy(LazyThreadSafetyMode.NONE) {
        DexExportService(dexSession)
    }
    private val dexKitRuntime by lazy(LazyThreadSafetyMode.NONE) {
        DexKitRuntime(normalizedDexPaths)
    }

    fun dexCount(): Int {
        return dexSession.dexCount
    }

    fun classCount(): Int {
        return dexSession.classCount
    }

    fun indexedClasses(): Sequence<DexIndexedClass> {
        return dexSession.classes()
    }

    fun getOrCreateBridge(): DexKitBridge? {
        return dexKitRuntime.getOrCreateBridge()
    }

    fun readDexNum(): Int? {
        return dexKitRuntime.readDexNum()
    }

    fun searchClassesByName(keyword: String): List<ClassData> {
        val bridge = getOrCreateBridge()
            ?: return emptyList()
        return bridge.findClass {
            matcher {
                className(
                    value = keyword,
                    matchType = StringMatchType.Contains,
                    ignoreCase = true,
                )
            }
        }
    }

    fun searchMethodsByString(keyword: String): List<MethodData> {
        val bridge = getOrCreateBridge()
            ?: return emptyList()
        return bridge.findMethod {
            matcher {
                addUsingString(
                    value = keyword,
                    matchType = StringMatchType.Contains,
                    ignoreCase = true,
                )
            }
        }
    }

    suspend fun exportSingleDex(
        className: String,
        dexPath: String,
        outputPath: String,
    ): String {
        return dexExportService.exportSingleDex(
            className = className,
            dexPath = dexPath,
            outputPath = outputPath,
        )
    }

    suspend fun exportSingleSmali(
        smaliUnicodeDecode: Boolean,
        className: String,
        dexPath: String,
        outputPath: String,
    ): String {
        return dexExportService.exportSingleSmali(
            smaliUnicodeDecode = smaliUnicodeDecode,
            className = className,
            dexPath = dexPath,
            outputPath = outputPath,
        )
    }

    suspend fun exportSingleJavaSource(
        escapeUnicode: Boolean,
        className: String,
        dexPath: String,
        outputPath: String,
    ): String {
        return dexExportService.exportSingleJavaSource(
            escapeUnicode = escapeUnicode,
            className = className,
            dexPath = dexPath,
            outputPath = outputPath,
        )
    }

    override fun close() {
        dexKitRuntime.close()
    }

    companion object {
        fun isDex(path: String): Boolean {
            return DexInputInspector.isDex(PlatformFile(path))
        }
    }
}
