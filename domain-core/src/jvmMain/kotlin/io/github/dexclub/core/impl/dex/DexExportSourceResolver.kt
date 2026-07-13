package io.github.dexclub.core.impl.dex

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import io.github.dexclub.core.api.dex.DexExportError
import io.github.dexclub.core.api.dex.DexExportErrorReason
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.impl.workspace.model.ClassSourceMapRecord
import io.github.dexclub.core.impl.workspace.model.ClassSourceRefRecord
import io.github.dexclub.core.impl.workspace.model.MaterialInventory
import io.github.dexclub.core.impl.workspace.store.WorkspaceStore
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.zip.ZipFile

internal class DexExportSourceResolver(
    private val store: WorkspaceStore,
    private val toolVersion: String,
) {
    fun resolveUniqueClassSource(
        workdirPath: Path,
        inventory: MaterialInventory,
        className: String,
        source: SourceLocator,
        workspace: WorkspaceContext? = null,
    ): LocatedClass {
        val descriptor = toTypeSignature(className)
        val candidates = resolveCandidates(
            workdirPath = workdirPath,
            inventory = inventory,
            source = preferredSourceLocator(
                workspace = workspace,
                inventory = inventory,
                classSignature = descriptor,
                explicitSource = source,
            ),
        )
        val matches = candidates.mapNotNull { candidate ->
            candidate.loadClassDef(descriptor)?.let { classDef ->
                LocatedClass(
                    sourcePath = candidate.sourcePath,
                    sourceEntry = candidate.sourceEntry,
                    classDef = classDef,
                )
            }
        }

        return when (matches.size) {
            1 -> matches.single()
            0 -> throw DexExportError(
                reason = DexExportErrorReason.ClassNotFound,
                message = buildString {
                    append("class not found")
                    append(": ")
                    append(className)
                    val sourceDescription = source.describe()
                    if (sourceDescription != null) {
                        append(" (")
                        append(sourceDescription)
                        append(')')
                    }
                },
            )

            else -> throw DexExportError(
                reason = DexExportErrorReason.AmbiguousClass,
                message = buildString {
                    append("class resolves to multiple dex sources; specify --source-path")
                    if (matches.any { it.sourceEntry != null }) {
                        append(" and --source-entry")
                    }
                    append(": ")
                    append(className)
                },
            )
        }
    }

    private fun resolveCandidates(
        workdirPath: Path,
        inventory: MaterialInventory,
        source: SourceLocator,
    ): List<DexSourceCandidate> {
        if (source.sourceEntry != null && source.sourcePath == null) {
            throw DexExportError(
                reason = DexExportErrorReason.InvalidSourceLocator,
                message = "sourceEntry requires sourcePath",
            )
        }

        val sourcePath = source.sourcePath
        if (sourcePath != null) {
            if (sourcePath in inventory.dexFiles) {
                if (source.sourceEntry != null) {
                    throw DexExportError(
                        reason = DexExportErrorReason.InvalidSourceLocator,
                        message = "sourceEntry is only valid for container sources: $sourcePath",
                    )
                }
                return listOf(dexFileCandidate(workdirPath, sourcePath))
            }
            if (sourcePath in inventory.apkFiles) {
                return apkEntryCandidates(workdirPath, sourcePath, source.sourceEntry)
            }
            throw DexExportError(
                reason = DexExportErrorReason.SourceNotFound,
                message = "sourcePath is not part of the current workspace: $sourcePath",
            )
        }

        return buildList {
            inventory.apkFiles.forEach { apkPath ->
                addAll(apkEntryCandidates(workdirPath, apkPath, null))
            }
            inventory.dexFiles.forEach { dexPath ->
                add(dexFileCandidate(workdirPath, dexPath))
            }
        }
    }

    private fun preferredSourceLocator(
        workspace: WorkspaceContext?,
        inventory: MaterialInventory,
        classSignature: String,
        explicitSource: SourceLocator,
    ): SourceLocator {
        if (explicitSource.sourcePath != null || explicitSource.sourceEntry != null || workspace == null) {
            return explicitSource
        }

        val currentFingerprint = workspace.snapshot.contentFingerprint
        val classSourceMap = store.loadClassSourceMap(workspace.workdir, workspace.activeTargetId)
            ?.takeIf {
                it.toolVersion == toolVersion &&
                    it.contentFingerprint == currentFingerprint
            }
            ?: buildClassSourceMap(
                workspace = workspace,
                inventory = inventory,
                contentFingerprint = currentFingerprint,
            ).also {
                store.saveClassSourceMap(workspace.workdir, workspace.activeTargetId, it)
            }

        val source = classSourceMap.sourceOf(classSignature) ?: return explicitSource
        return SourceLocator(
            sourcePath = source.sourcePath,
            sourceEntry = source.sourceEntry,
        )
    }

    private fun buildClassSourceMap(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        contentFingerprint: String,
    ): ClassSourceMapRecord {
        val workdirPath = Paths.get(workspace.workdir)
        val occurrences = linkedMapOf<String, MutableSet<ClassSourceRefKey>>()

        inventory.dexFiles.forEach { dexPath ->
            val dexFile = DexBackedDexFile(
                Opcodes.getDefault(),
                Files.readAllBytes(workdirPath.resolve(dexPath).normalize()),
            )
            dexFile.classes.forEach { classDef ->
                occurrences.getOrPut(classDef.type) { linkedSetOf() }.add(
                    ClassSourceRefKey(sourcePath = dexPath, sourceEntry = null),
                )
            }
        }

        inventory.apkFiles.forEach { apkPath ->
            val apk = workdirPath.resolve(apkPath).normalize()
            ZipFile(apk.toFile()).use { zip ->
                zip.entries().asSequence()
                    .filterNot { it.isDirectory }
                    .filter { it.name.endsWith(".dex", ignoreCase = true) }
                    .forEach { entry ->
                        val dexBytes = zip.getInputStream(entry).use { it.readBytes() }
                        val dexFile = DexBackedDexFile(Opcodes.getDefault(), dexBytes)
                        dexFile.classes.forEach { classDef ->
                            occurrences.getOrPut(classDef.type) { linkedSetOf() }.add(
                                ClassSourceRefKey(sourcePath = apkPath, sourceEntry = entry.name),
                            )
                        }
                    }
            }
        }

        val sourceIds = linkedMapOf<ClassSourceRefKey, Int>()
        val sources = mutableListOf<ClassSourceRefRecord>()

        fun sourceIdOf(source: ClassSourceRefKey): Int =
            sourceIds.getOrPut(source) {
                val id = sources.size
                sources += ClassSourceRefRecord(
                    id = id,
                    sourcePath = source.sourcePath,
                    sourceEntry = source.sourceEntry,
                )
                id
            }

        val mappings = occurrences.mapNotNull { (classSignature, sourceRefs) ->
            sourceRefs.singleOrNull()?.let { classSignature to sourceIdOf(it) }
        }.toMap(linkedMapOf())

        return ClassSourceMapRecord(
            generatedAt = Instant.now().toString(),
            targetId = workspace.activeTargetId,
            toolVersion = toolVersion,
            contentFingerprint = contentFingerprint,
            sources = sources,
            mappings = mappings,
        )
    }

    private fun dexFileCandidate(workdirPath: Path, relativeDexPath: String): DexSourceCandidate =
        DexSourceCandidate(
            sourcePath = relativeDexPath,
            sourceEntry = null,
            dexBytes = Files.readAllBytes(workdirPath.resolve(relativeDexPath).normalize()),
        )

    private fun apkEntryCandidates(
        workdirPath: Path,
        relativeApkPath: String,
        sourceEntry: String?,
    ): List<DexSourceCandidate> {
        val apkPath = workdirPath.resolve(relativeApkPath).normalize()
        ZipFile(apkPath.toFile()).use { zip ->
            val dexEntries = zip.entries().asSequence()
                .filterNot { it.isDirectory }
                .filter { it.name.endsWith(".dex", ignoreCase = true) }
                .sortedWith(compareBy({ dexEntrySortKey(it.name).first }, { dexEntrySortKey(it.name).second }, { it.name }))
                .toList()

            if (sourceEntry != null) {
                val entry = dexEntries.firstOrNull { it.name == sourceEntry }
                    ?: throw DexExportError(
                        reason = DexExportErrorReason.SourceNotFound,
                        message = "sourceEntry not found in sourcePath: $relativeApkPath!$sourceEntry",
                    )
                return listOf(
                    DexSourceCandidate(
                        sourcePath = relativeApkPath,
                        sourceEntry = entry.name,
                        dexBytes = zip.getInputStream(entry).use { it.readBytes() },
                    ),
                )
            }

            if (dexEntries.isEmpty()) {
                throw DexExportError(
                    reason = DexExportErrorReason.SourceNotFound,
                    message = "sourcePath does not contain any dex entries: $relativeApkPath",
                )
            }

            return dexEntries.map { entry ->
                DexSourceCandidate(
                    sourcePath = relativeApkPath,
                    sourceEntry = entry.name,
                    dexBytes = zip.getInputStream(entry).use { it.readBytes() },
                )
            }
        }
    }

    private data class DexSourceCandidate(
        val sourcePath: String,
        val sourceEntry: String?,
        val dexBytes: ByteArray,
    ) {
        fun loadClassDef(descriptor: String): ClassDef? {
            val dexFile = DexBackedDexFile(Opcodes.getDefault(), dexBytes)
            return dexFile.classes.firstOrNull { it.type == descriptor }
        }
    }

    data class LocatedClass(
        val sourcePath: String,
        val sourceEntry: String?,
        val classDef: ClassDef,
    )

    private data class ClassSourceRefKey(
        val sourcePath: String,
        val sourceEntry: String?,
    )
}
