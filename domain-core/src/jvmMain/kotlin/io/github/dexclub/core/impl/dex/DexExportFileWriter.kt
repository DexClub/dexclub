package io.github.dexclub.core.impl.dex

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.impl.workspace.store.WorkspaceStore
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

internal class DexExportFileWriter(
    private val store: WorkspaceStore,
) {
    fun decompileSingleClassDefToJava(
        workspace: WorkspaceContext,
        classDef: ClassDef,
        outputPath: String,
        sanitize: (ClassDef) -> ClassDef,
        decompile: (dexPath: String, outputPath: String, tempDirectory: File) -> Unit,
    ): String {
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        val exportTempRoot = Paths.get(store.exportTempDir(workspace.workdir, workspace.activeTargetId))
        Files.createDirectories(exportTempRoot)
        val sessionDir = Files.createTempDirectory(exportTempRoot, "${outputFile.nameWithoutExtension}-").toFile()
        val tempDex = File(sessionDir, "input.dex")
        val sanitizedClassDef = sanitize(classDef)
        return try {
            writeSingleClassDex(
                classDef = sanitizedClassDef,
                outputPath = tempDex.absolutePath,
            )
            decompile(
                tempDex.absolutePath,
                outputPath,
                sessionDir,
            )
            outputPath
        } finally {
            sessionDir.deleteRecursively()
        }
    }

    fun renderClassSmali(
        classDef: ClassDef,
        outputPath: String,
        renderText: (ClassDef) -> String,
    ): String = writeTextOutput(outputPath, renderText(classDef))

    fun writeSingleClassDex(
        classDef: ClassDef,
        outputPath: String,
    ): String {
        val dataStore = MemoryDataStore()
        val dexPool = DexPool(Opcodes.getDefault())
        dexPool.internClass(classDef)
        dexPool.writeTo(dataStore)
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(dataStore.data)
        return outputFile.absolutePath
    }

    fun writeTextOutput(outputPath: String, text: String): String {
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(text, Charsets.UTF_8)
        return outputFile.absolutePath
    }
}

