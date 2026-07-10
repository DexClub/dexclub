package io.github.dexclub.dexkit

import io.github.dexclub.dexkit.query.BatchFindClassUsingStrings
import io.github.dexclub.dexkit.query.BatchFindMethodUsingStrings
import io.github.dexclub.dexkit.query.FindClass
import io.github.dexclub.dexkit.query.FindField
import io.github.dexclub.dexkit.query.FindMethod
import io.github.dexclub.dexkit.result.ClassData
import io.github.dexclub.dexkit.result.ClassDataList
import io.github.dexclub.dexkit.result.FieldData
import io.github.dexclub.dexkit.result.FieldDataList
import io.github.dexclub.dexkit.result.MethodData
import io.github.dexclub.dexkit.result.MethodDataList
import io.github.dexclub.dexkit.result.UsingFieldData
import io.github.dexclub.dexkit.result.toClassDataList
import io.github.dexclub.dexkit.result.toFieldDataList
import io.github.dexclub.dexkit.result.toMethodDataList
import org.luckypray.dexkit.DexKitBridge as NativeDexKitBridge
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

actual class DexKitBridge {
    private val lock = ReentrantReadWriteLock()

    @Volatile
    private var delegate: NativeDexKitBridge? = null

    actual constructor(dexPaths: List<String>) {
        require(dexPaths.isNotEmpty()) { "dexPaths 不能为空" }
        val dexBytesArray = dexPaths.map { path ->
            val dexFile = File(path)
            require(dexFile.exists()) { "dex 文件不存在: ${dexFile.absolutePath}" }
            require(dexFile.isFile) { "dex 路径必须是文件: ${dexFile.absolutePath}" }
            dexFile.readBytes()
        }.toTypedArray()
        delegate = NativeDexKitBridge.create(dexBytesArray)
    }

    actual constructor(apkPath: String) {
        require(apkPath.isNotEmpty()) { "apkPath 不能为空" }
        val apkFile = File(apkPath)
        require(apkFile.exists()) { "apk 文件不存在: ${apkFile.absolutePath}" }
        require(apkFile.isFile) { "apk 路径必须是文件: ${apkFile.absolutePath}" }
        delegate = NativeDexKitBridge.create(apkFile.absolutePath)
    }

    actual constructor(dexBytesArray: Array<ByteArray>) {
        require(dexBytesArray.isNotEmpty()) { "dexBytesArray 不能为空" }
        delegate = NativeDexKitBridge.create(dexBytesArray)
    }

    /**
     * 通过 [ClassLoader] 创建 Bridge（仅 Android 平台）。
     *
     * @param loader 目标 ClassLoader
     * @param useMemoryDexFile 是否使用内存中的 dex 文件
     */
    constructor(loader: ClassLoader, useMemoryDexFile: Boolean = false) {
        delegate = NativeDexKitBridge.create(loader, useMemoryDexFile)
    }

    actual val isValid: Boolean
        get() = lock.read { delegate?.isValid == true }

    actual fun getDexNum(): Int = withDelegate { it.getDexNum() }

    actual fun setThreadNum(num: Int) = withDelegate { it.setThreadNum(num) }

    actual fun initFullCache() = withDelegate { it.initFullCache() }

    actual fun exportDexFile(outPath: String) {
        require(outPath.isNotEmpty()) { "outPath 不能为空" }
        withDelegate { it.exportDexFile(outPath) }
    }

    actual fun findClass(query: FindClass): ClassDataList = withDelegate { d ->
        d.findClass(query.toNative(d)).map { it.toKmpClassData() }.toClassDataList(this)
    }

    actual fun findMethod(query: FindMethod): MethodDataList = withDelegate { d ->
        d.findMethod(query.toNative(d)).map { it.toKmpMethodData() }.toMethodDataList(this)
    }

    actual fun findField(query: FindField): FieldDataList = withDelegate { d ->
        d.findField(query.toNative(d)).map { it.toKmpFieldData() }.toFieldDataList(this)
    }

    actual fun batchFindClassUsingStrings(query: BatchFindClassUsingStrings): Map<String, ClassDataList> = withDelegate { d ->
        d.batchFindClassUsingStrings(query.toNative(d))
            .mapValues { (_, list) -> list.map { it.toKmpClassData() }.toClassDataList(this) }
    }

    actual fun batchFindMethodUsingStrings(query: BatchFindMethodUsingStrings): Map<String, MethodDataList> = withDelegate { d ->
        d.batchFindMethodUsingStrings(query.toNative(d))
            .mapValues { (_, list) -> list.map { it.toKmpMethodData() }.toMethodDataList(this) }
    }

    actual fun getClassData(descriptor: String): ClassData? {
        require(descriptor.isNotEmpty()) { "descriptor 不能为空" }
        return withDelegate { it.getClassData(descriptor)?.toKmpClassData() }
    }

    actual fun getMethodData(descriptor: String): MethodData? {
        require(descriptor.isNotEmpty()) { "descriptor 不能为空" }
        return withDelegate { it.getMethodData(descriptor)?.toKmpMethodData() }
    }

    actual fun getFieldData(descriptor: String): FieldData? {
        require(descriptor.isNotEmpty()) { "descriptor 不能为空" }
        return withDelegate { it.getFieldData(descriptor)?.toKmpFieldData() }
    }

    actual fun getFieldReaders(descriptor: String): MethodDataList {
        require(descriptor.isNotEmpty()) { "descriptor 不能为空" }
        return withDelegate { d ->
            d.getFieldData(descriptor)
                ?.readers?.map { it.toKmpMethodData() }.orEmpty()
                .toMethodDataList(this)
        }
    }

    actual fun getFieldWriters(descriptor: String): MethodDataList {
        require(descriptor.isNotEmpty()) { "descriptor 不能为空" }
        return withDelegate { d ->
            d.getFieldData(descriptor)
                ?.writers?.map { it.toKmpMethodData() }.orEmpty()
                .toMethodDataList(this)
        }
    }

    actual fun getMethodCallers(descriptor: String): MethodDataList {
        require(descriptor.isNotEmpty()) { "descriptor 不能为空" }
        return withDelegate { d ->
            d.getMethodData(descriptor)
                ?.callers?.map { it.toKmpMethodData() }.orEmpty()
                .toMethodDataList(this)
        }
    }

    actual fun getMethodInvokes(descriptor: String): MethodDataList {
        require(descriptor.isNotEmpty()) { "descriptor 不能为空" }
        return withDelegate { d ->
            d.getMethodData(descriptor)
                ?.invokes?.map { it.toKmpMethodData() }.orEmpty()
                .toMethodDataList(this)
        }
    }

    actual fun getMethodUsingFields(descriptor: String): List<UsingFieldData> {
        require(descriptor.isNotEmpty()) { "descriptor 不能为空" }
        return withDelegate { d ->
            d.getMethodData(descriptor)
                ?.usingFields?.map { it.toKmpUsingFieldData() }.orEmpty()
        }
    }

    actual fun getMethodUsingStrings(descriptor: String): List<String> {
        require(descriptor.isNotEmpty()) { "descriptor 不能为空" }
        return withDelegate { d ->
            d.getMethodData(descriptor)
                ?.usingStrings.orEmpty()
        }
    }

    actual fun getMethodAnnotations(descriptor: String): List<String> {
        require(descriptor.isNotEmpty()) { "descriptor 不能为空" }
        return withDelegate { d ->
            d.getMethodData(descriptor)
                ?.annotations?.map { it.toString() }.orEmpty()
        }
    }

    actual fun close() {
        lock.write {
            delegate?.close()
            delegate = null
        }
    }

    private inline fun <T> withDelegate(block: (NativeDexKitBridge) -> T): T =
        lock.read { block(ensureDelegate()) }

    private fun ensureDelegate(): NativeDexKitBridge =
        checkNotNull(delegate) { "DexKitBridge 未初始化，请传入有效的 dex/apk 数据" }
}
