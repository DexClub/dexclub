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
import java.util.zip.ZipFile
import kotlin.concurrent.read
import kotlin.concurrent.write

actual class DexKitBridge {
    private val lock = ReentrantReadWriteLock()

    @Volatile
    private var delegate: NativeDexKitBridge? = null

    actual constructor(dexPaths: List<String>) {
        require(dexPaths.isNotEmpty()) { "dexPaths must not be empty" }
        val files = dexPaths.map { File(it) }
        files.forEach { dexFile ->
            require(dexFile.exists()) { "dex file does not exist: ${dexFile.absolutePath}" }
            require(dexFile.isFile) { "dex path must point to a file: ${dexFile.absolutePath}" }
        }
        delegate = if (files.size == 1 && files.first().extension.equals("apk", ignoreCase = true)) {
            NativeDexKitBridge.create(readDexBytesFromApk(files.first()))
        } else {
            NativeDexKitBridge.create(files.map { it.readBytes() }.toTypedArray())
        }
    }

    actual constructor(apkPath: String) {
        require(apkPath.isNotEmpty()) { "apkPath must not be empty" }
        val apkFile = File(apkPath)
        require(apkFile.exists()) { "apk file does not exist: ${apkFile.absolutePath}" }
        require(apkFile.isFile) { "apk path must point to a file: ${apkFile.absolutePath}" }
        delegate = NativeDexKitBridge.create(apkFile.absolutePath)
    }

    actual constructor(dexBytesArray: Array<ByteArray>) {
        require(dexBytesArray.isNotEmpty()) { "dexBytesArray must not be empty" }
        delegate = NativeDexKitBridge.create(dexBytesArray)
    }

    actual val isValid: Boolean
        get() = lock.read { delegate?.isValid == true }

    actual fun getDexNum(): Int = withDelegate { it.getDexNum() }

    actual fun setThreadNum(num: Int) = withDelegate { it.setThreadNum(num) }

    actual fun initFullCache() = withDelegate { it.initFullCache() }

    actual fun exportDexFile(outPath: String) {
        require(outPath.isNotEmpty()) { "outPath must not be empty" }
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
        require(descriptor.isNotEmpty()) { "descriptor must not be empty" }
        return withDelegate { it.getClassData(descriptor)?.toKmpClassData() }
    }

    actual fun getMethodData(descriptor: String): MethodData? {
        require(descriptor.isNotEmpty()) { "descriptor must not be empty" }
        return withDelegate { it.getMethodData(descriptor)?.toKmpMethodData() }
    }

    actual fun getFieldData(descriptor: String): FieldData? {
        require(descriptor.isNotEmpty()) { "descriptor must not be empty" }
        return withDelegate { it.getFieldData(descriptor)?.toKmpFieldData() }
    }

    actual fun getFieldReaders(descriptor: String): MethodDataList {
        require(descriptor.isNotEmpty()) { "descriptor must not be empty" }
        return withDelegate { d ->
            d.getFieldData(descriptor)
                ?.readers?.map { it.toKmpMethodData() }.orEmpty()
                .toMethodDataList(this)
        }
    }

    actual fun getFieldWriters(descriptor: String): MethodDataList {
        require(descriptor.isNotEmpty()) { "descriptor must not be empty" }
        return withDelegate { d ->
            d.getFieldData(descriptor)
                ?.writers?.map { it.toKmpMethodData() }.orEmpty()
                .toMethodDataList(this)
        }
    }

    actual fun getMethodCallers(descriptor: String): MethodDataList {
        require(descriptor.isNotEmpty()) { "descriptor must not be empty" }
        return withDelegate { d ->
            d.getMethodData(descriptor)
                ?.callers?.map { it.toKmpMethodData() }.orEmpty()
                .toMethodDataList(this)
        }
    }

    actual fun getMethodInvokes(descriptor: String): MethodDataList {
        require(descriptor.isNotEmpty()) { "descriptor must not be empty" }
        return withDelegate { d ->
            d.getMethodData(descriptor)
                ?.invokes?.map { it.toKmpMethodData() }.orEmpty()
                .toMethodDataList(this)
        }
    }

    actual fun getMethodUsingFields(descriptor: String): List<UsingFieldData> {
        require(descriptor.isNotEmpty()) { "descriptor must not be empty" }
        return withDelegate { d ->
            d.getMethodData(descriptor)
                ?.usingFields?.map { it.toKmpUsingFieldData() }.orEmpty()
        }
    }

    actual fun getMethodUsingStrings(descriptor: String): List<String> {
        require(descriptor.isNotEmpty()) { "descriptor must not be empty" }
        return withDelegate { d ->
            d.getMethodData(descriptor)
                ?.usingStrings.orEmpty()
        }
    }

    actual fun getMethodAnnotations(descriptor: String): List<String> {
        require(descriptor.isNotEmpty()) { "descriptor must not be empty" }
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
        checkNotNull(delegate) { "DexKitBridge is not initialized; provide valid dex or apk input" }

    private fun readDexBytesFromApk(apkFile: File): Array<ByteArray> {
        val dexEntries = ZipFile(apkFile).use { zip ->
            zip.entries().asSequence()
                .filterNot { it.isDirectory }
                .filter { it.name.endsWith(".dex", ignoreCase = true) }
                .map { entry ->
                    zip.getInputStream(entry).use { input -> input.readBytes() }
                }
                .toList()
        }
        require(dexEntries.isNotEmpty()) { "no dex entries were found in apk: ${apkFile.absolutePath}" }
        return dexEntries.toTypedArray()
    }
}
