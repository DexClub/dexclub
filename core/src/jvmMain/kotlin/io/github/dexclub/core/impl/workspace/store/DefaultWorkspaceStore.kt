package io.github.dexclub.core.impl.workspace.store

import io.github.dexclub.core.api.workspace.GcResult
import io.github.dexclub.core.api.workspace.WorkspaceResolveError
import io.github.dexclub.core.api.workspace.WorkspaceResolveErrorReason
import io.github.dexclub.core.impl.shared.workspaceJson
import io.github.dexclub.core.impl.workspace.model.ClassSourceMapRecord
import io.github.dexclub.core.impl.workspace.model.DecodedXmlCacheRecord
import io.github.dexclub.core.impl.workspace.model.ManifestCacheRecord
import io.github.dexclub.core.impl.workspace.model.ResourceEntryIndexRecord
import io.github.dexclub.core.impl.workspace.model.ResourceTableCacheRecord
import io.github.dexclub.core.impl.workspace.model.SnapshotRecord
import io.github.dexclub.core.impl.workspace.model.TargetRecord
import io.github.dexclub.core.impl.workspace.model.WorkspaceRecord
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Comparator
import kotlinx.serialization.SerializationException

internal class DefaultWorkspaceStore : WorkspaceStore {
    override fun exists(workdir: String): Boolean = Files.isDirectory(dexclubDirPath(workdir))

    override fun dexclubDir(workdir: String): String = dexclubDirPath(workdir).toString()

    override fun apkDexDir(workdir: String, targetId: String): String =
        targetDir(workdir, targetId).resolve("cache/decoded/apk-dex").toString()

    override fun exportTempDir(workdir: String, targetId: String): String =
        targetDir(workdir, targetId).resolve("cache/exports/tmp").toString()

    override fun initialize(
        workdir: String,
        workspace: WorkspaceRecord,
        target: TargetRecord,
        snapshot: SnapshotRecord,
    ) {
        val targetDir = targetDir(workdir, target.targetId)
        writeState {
            initializeCacheDirs(targetDir)
            writeJson(workspaceFile(workdir), workspace.toDto())
            writeJson(targetDir.resolve("target.json"), target.toDto())
            writeJson(targetDir.resolve("snapshot.json"), snapshot.toDto())
        }
    }

    override fun loadWorkspace(workdir: String): WorkspaceRecord =
        readJson<WorkspaceDto>(
            path = workspaceFile(workdir),
            reason = WorkspaceResolveErrorReason.InvalidWorkspaceMetadata,
            workdir = workdir,
            missingMessage = "Workspace metadata is missing: ${workspaceFile(workdir)}",
        ).toRecord()

    override fun saveWorkspace(workdir: String, workspace: WorkspaceRecord) {
        writeState {
            writeJson(workspaceFile(workdir), workspace.toDto())
        }
    }

    override fun loadTarget(workdir: String, targetId: String): TargetRecord =
        readJson<TargetDto>(
            path = targetDir(workdir, targetId).resolve("target.json"),
            reason = WorkspaceResolveErrorReason.InvalidTargetMetadata,
            workdir = workdir,
            missingMessage = "Target metadata is missing: ${targetDir(workdir, targetId).resolve("target.json")}",
        ).toRecord()

    override fun listTargets(workdir: String): List<TargetRecord> {
        val targetsRoot = dexclubDirPath(workdir).resolve("targets")
        if (!Files.isDirectory(targetsRoot)) return emptyList()
        Files.list(targetsRoot).use { targets ->
            val result = mutableListOf<TargetRecord>()
            val iterator = targets.iterator()
            while (iterator.hasNext()) {
                val dir = iterator.next()
                if (!Files.isDirectory(dir)) continue
                val targetPath = dir.resolve("target.json")
                if (!Files.isRegularFile(targetPath)) continue
                val record = runCatching {
                    workspaceJson.decodeFromString<TargetDto>(Files.readString(targetPath)).toRecord()
                }.getOrNull() ?: continue
                result += record
            }
            return result.sortedBy { it.inputPath }
        }
    }

    override fun findTargetByInputPath(workdir: String, inputPath: String): TargetRecord? {
        return listTargets(workdir).firstOrNull { it.inputPath == inputPath }
    }

    override fun saveTarget(workdir: String, target: TargetRecord) {
        writeState {
            Files.createDirectories(targetDir(workdir, target.targetId))
            writeJson(targetDir(workdir, target.targetId).resolve("target.json"), target.toDto())
        }
    }

    override fun loadSnapshot(workdir: String, targetId: String): SnapshotRecord? {
        val snapshotPath = targetDir(workdir, targetId).resolve("snapshot.json")
        if (!Files.isRegularFile(snapshotPath)) return null
        return readJson<SnapshotDto>(
            path = snapshotPath,
            reason = WorkspaceResolveErrorReason.InvalidSnapshot,
            workdir = workdir,
            missingMessage = "Snapshot is missing: $snapshotPath",
        ).toRecord()
    }

    override fun saveSnapshot(workdir: String, targetId: String, snapshot: SnapshotRecord) {
        writeState {
            Files.createDirectories(targetDir(workdir, targetId))
            writeJson(targetDir(workdir, targetId).resolve("snapshot.json"), snapshot.toDto())
        }
    }

    override fun loadClassSourceMap(workdir: String, targetId: String): ClassSourceMapRecord? {
        return readOptionalJson(classSourceMapPath(workdir, targetId)) {
            workspaceJson.decodeFromString<ClassSourceMapDto>(it).toRecord()
        }
    }

    override fun saveClassSourceMap(workdir: String, targetId: String, classSourceMap: ClassSourceMapRecord) {
        writeState {
            writeJson(classSourceMapPath(workdir, targetId), classSourceMap.toDto())
        }
    }

    override fun loadManifestCache(workdir: String, targetId: String): ManifestCacheRecord? {
        return readOptionalJson(manifestCachePath(workdir, targetId)) {
            workspaceJson.decodeFromString<ManifestCacheDto>(it).toRecord()
        }
    }

    override fun saveManifestCache(workdir: String, targetId: String, manifestCache: ManifestCacheRecord) {
        writeState {
            writeJson(manifestCachePath(workdir, targetId), manifestCache.toDto())
        }
    }

    override fun loadResourceTableCache(workdir: String, targetId: String): ResourceTableCacheRecord? {
        return readOptionalJson(resourceTableCachePath(workdir, targetId)) {
            workspaceJson.decodeFromString<ResourceTableCacheDto>(it).toRecord()
        }
    }

    override fun saveResourceTableCache(workdir: String, targetId: String, resourceTableCache: ResourceTableCacheRecord) {
        writeState {
            writeJson(resourceTableCachePath(workdir, targetId), resourceTableCache.toDto())
        }
    }

    override fun loadDecodedXmlCache(workdir: String, targetId: String, xmlId: String): DecodedXmlCacheRecord? {
        return readOptionalJson(decodedXmlCachePath(workdir, targetId, xmlId)) {
            workspaceJson.decodeFromString<DecodedXmlCacheDto>(it).toRecord()
        }
    }

    override fun saveDecodedXmlCache(
        workdir: String,
        targetId: String,
        xmlId: String,
        decodedXmlCache: DecodedXmlCacheRecord,
    ) {
        writeState {
            writeJson(decodedXmlCachePath(workdir, targetId, xmlId), decodedXmlCache.toDto())
        }
    }

    override fun loadResourceEntryIndex(workdir: String, targetId: String): ResourceEntryIndexRecord? {
        return readOptionalJson(resourceEntryIndexPath(workdir, targetId)) {
            workspaceJson.decodeFromString<ResourceEntryIndexDto>(it).toRecord()
        }
    }

    override fun saveResourceEntryIndex(workdir: String, targetId: String, resourceEntryIndex: ResourceEntryIndexRecord) {
        writeState {
            writeJson(resourceEntryIndexPath(workdir, targetId), resourceEntryIndex.toDto())
        }
    }

    override fun clearTargetCache(workdir: String, targetId: String): GcResult {
        val cacheRoot = targetDir(workdir, targetId).resolve("cache")
        val deleted = deleteDirectoryContents(cacheRoot)
        writeState {
            initializeCacheDirs(targetDir(workdir, targetId))
        }
        return GcResult(
            workdir = normalizedWorkdir(workdir).toString(),
            targetId = targetId,
            deletedFiles = deleted.deletedFiles,
            deletedBytes = deleted.deletedBytes,
        )
    }

    private inline fun <reified T> readJson(
        path: Path,
        reason: WorkspaceResolveErrorReason,
        workdir: String,
        missingMessage: String,
    ): T {
        if (!Files.isRegularFile(path)) {
            throw WorkspaceResolveError(
                reason = reason,
                workdir = normalizedWorkdir(workdir).toString(),
                message = missingMessage,
            )
        }
        return try {
            workspaceJson.decodeFromString<T>(Files.readString(path))
        } catch (cause: SerializationException) {
            throw WorkspaceResolveError(
                reason = reason,
                workdir = normalizedWorkdir(workdir).toString(),
                message = "Invalid workspace state file: $path",
                cause = cause,
            )
        } catch (cause: IllegalArgumentException) {
            throw WorkspaceResolveError(
                reason = reason,
                workdir = normalizedWorkdir(workdir).toString(),
                message = "Invalid workspace state value: $path",
                cause = cause,
            )
        } catch (cause: IOException) {
            throw WorkspaceResolveError(
                reason = reason,
                workdir = normalizedWorkdir(workdir).toString(),
                message = "Failed to read workspace state file: $path",
                cause = cause,
            )
        }
    }

    private fun writeJson(path: Path, value: Any) {
        Files.createDirectories(path.parent)
        val content = when (value) {
            is WorkspaceDto -> workspaceJson.encodeToString(WorkspaceDto.serializer(), value)
            is TargetDto -> workspaceJson.encodeToString(TargetDto.serializer(), value)
            is SnapshotDto -> workspaceJson.encodeToString(SnapshotDto.serializer(), value)
            is ClassSourceMapDto -> workspaceJson.encodeToString(ClassSourceMapDto.serializer(), value)
            is ManifestCacheDto -> workspaceJson.encodeToString(ManifestCacheDto.serializer(), value)
            is ResourceTableCacheDto -> workspaceJson.encodeToString(ResourceTableCacheDto.serializer(), value)
            is DecodedXmlCacheDto -> workspaceJson.encodeToString(DecodedXmlCacheDto.serializer(), value)
            is ResourceEntryIndexDto -> workspaceJson.encodeToString(ResourceEntryIndexDto.serializer(), value)
            else -> error("Unsupported workspace dto: ${value::class.qualifiedName}")
        }
        writeAtomically(path, content)
    }

    private fun writeAtomically(path: Path, content: String) {
        val tempPath = Files.createTempFile(path.parent, path.fileName.toString(), ".tmp")
        try {
            Files.writeString(tempPath, content)
            try {
                Files.move(tempPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(tempPath)
        }
    }

    private fun writeState(action: () -> Unit) {
        try {
            action()
        } catch (cause: IOException) {
            throw IllegalStateException("Failed to write workspace state", cause)
        }
    }

    private fun <T> readOptionalJson(path: Path, decode: (String) -> T): T? {
        if (!Files.isRegularFile(path)) return null
        return try {
            decode(Files.readString(path))
        } catch (_: Exception) {
            null
        }
    }

    private fun workspaceFile(workdir: String): Path = dexclubDirPath(workdir).resolve("workspace.json")

    private fun classSourceMapPath(workdir: String, targetId: String): Path =
        indexesDir(workdir, targetId).resolve("class-source-map.json")

    private fun manifestCachePath(workdir: String, targetId: String): Path =
        decodedCacheDir(workdir, targetId).resolve("manifest.json")

    private fun resourceTableCachePath(workdir: String, targetId: String): Path =
        decodedCacheDir(workdir, targetId).resolve("resource-table.json")

    private fun decodedXmlCachePath(workdir: String, targetId: String, xmlId: String): Path =
        decodedXmlDir(workdir, targetId).resolve("$xmlId.json")

    private fun resourceEntryIndexPath(workdir: String, targetId: String): Path =
        indexesDir(workdir, targetId).resolve("resource-entry-index.json")

    private fun initializeCacheDirs(targetDir: Path) {
        Files.createDirectories(targetDir.resolve("cache/decoded"))
        Files.createDirectories(targetDir.resolve("cache/decoded/apk-dex"))
        Files.createDirectories(targetDir.resolve("cache/indexes"))
        Files.createDirectories(targetDir.resolve("cache/exports/tmp"))
    }

    private fun decodedCacheDir(workdir: String, targetId: String): Path =
        targetDir(workdir, targetId).resolve("cache/decoded")

    private fun decodedXmlDir(workdir: String, targetId: String): Path =
        decodedCacheDir(workdir, targetId).resolve("xml")

    private fun indexesDir(workdir: String, targetId: String): Path =
        targetDir(workdir, targetId).resolve("cache/indexes")

    private fun targetDir(workdir: String, targetId: String): Path =
        dexclubDirPath(workdir).resolve("targets").resolve(targetId)

    private fun dexclubDirPath(workdir: String): Path = normalizedWorkdir(workdir).resolve(".dexclub")

    private fun normalizedWorkdir(workdir: String): Path = Paths.get(workdir).toAbsolutePath().normalize()

    private fun deleteDirectoryContents(path: Path): DeletedContent {
        if (!Files.exists(path)) return DeletedContent()
        val files = mutableListOf<Path>()
        Files.walk(path).use { walk ->
            walk.filter { it != path }
                .sorted(Comparator.reverseOrder())
                .forEach(files::add)
        }
        var deletedFiles = 0
        var deletedBytes = 0L
        files.forEach { item ->
            if (Files.isRegularFile(item)) {
                deletedFiles += 1
                deletedBytes += Files.size(item)
            }
            Files.deleteIfExists(item)
        }
        return DeletedContent(
            deletedFiles = deletedFiles,
            deletedBytes = deletedBytes,
        )
    }

    private data class DeletedContent(
        val deletedFiles: Int = 0,
        val deletedBytes: Long = 0L,
    ) {
        operator fun plus(other: DeletedContent): DeletedContent =
            DeletedContent(
                deletedFiles = deletedFiles + other.deletedFiles,
                deletedBytes = deletedBytes + other.deletedBytes,
            )
    }
}
