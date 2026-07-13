package io.github.dexclub.core.impl.dex

import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.impl.workspace.store.WorkspaceStore
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

internal class DexSearchApkCache(
    private val store: WorkspaceStore,
) {
    fun prepareApkDexFiles(
        workspace: WorkspaceContext,
        workdirPath: Path,
        relativeApkPath: String,
    ): List<Pair<String, Path>> {
        val apkPath = workdirPath.resolve(relativeApkPath).normalize()
        val cacheDir = Paths.get(store.apkDexDir(workspace.workdir, workspace.activeTargetId))
        val fingerprintFile = cacheDir.resolve(".content-fingerprint")
        Files.createDirectories(cacheDir)

        val cachedFingerprint = if (Files.isRegularFile(fingerprintFile)) {
            Files.readString(fingerprintFile)
        } else {
            null
        }

        if (cachedFingerprint != workspace.snapshot.contentFingerprint) {
            clearDirectory(cacheDir)
            extractApkDexEntries(apkPath, cacheDir)
            Files.writeString(fingerprintFile, workspace.snapshot.contentFingerprint)
        }

        val dexFiles = Files.list(cacheDir).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().endsWith(".dex", ignoreCase = true) }
                .sorted(compareBy({ dexEntrySortKey(it.fileName.toString()).first }, { dexEntrySortKey(it.fileName.toString()).second }, { it.fileName.toString() }))
                .toList()
        }

        check(dexFiles.isNotEmpty()) { "APK does not contain any dex entries: $apkPath" }
        return dexFiles.map { it.fileName.toString() to it }
    }

    private fun extractApkDexEntries(apkPath: Path, cacheDir: Path) {
        ZipFile(apkPath.toFile()).use { zip ->
            val dexEntries = zip.entries().asSequence()
                .filterNot { it.isDirectory }
                .filter { it.name.endsWith(".dex", ignoreCase = true) }
                .sortedWith(compareBy({ dexEntrySortKey(it.name).first }, { dexEntrySortKey(it.name).second }, { it.name }))
                .toList()
            check(dexEntries.isNotEmpty()) { "APK does not contain any dex entries: $apkPath" }
            for (entry in dexEntries) {
                val output = cacheDir.resolve(entry.name)
                zip.getInputStream(entry).use { input ->
                    Files.copy(input, output)
                }
            }
        }
    }

    private fun clearDirectory(path: Path) {
        if (!Files.isDirectory(path)) return
        Files.list(path).use { paths ->
            paths.forEach { child ->
                if (Files.isDirectory(child)) {
                    clearDirectory(child)
                }
                Files.deleteIfExists(child)
            }
        }
    }
}
