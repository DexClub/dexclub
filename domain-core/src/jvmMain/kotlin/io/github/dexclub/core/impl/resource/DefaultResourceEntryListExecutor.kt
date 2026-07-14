package io.github.dexclub.core.impl.resource

import com.reandroid.apk.ApkModule
import com.reandroid.apk.ResFile
import io.github.dexclub.core.api.resource.ResourceEntry
import io.github.dexclub.core.api.resource.ResourceResolution
import io.github.dexclub.core.api.resource.normalizedResolution
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.impl.workspace.model.MaterialInventory
import io.github.dexclub.core.impl.workspace.model.ResourceEntryIndexRecord
import io.github.dexclub.core.impl.workspace.store.WorkspaceStore
import java.nio.file.Path

internal class DefaultResourceEntryListExecutor(
    private val store: WorkspaceStore,
    private val toolVersion: String,
) : ResourceEntryListExecutor {
    override fun listResourceEntries(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
    ): List<ResourceEntry> {
        store.loadResourceEntryIndex(workspace.workdir, workspace.activeTargetId)
            ?.takeIf {
                it.toolVersion == toolVersion &&
                    it.contentFingerprint == workspace.snapshot.contentFingerprint
            }
            ?.let { return it.entries.map(ResourceEntry::normalizedResolution) }

        val workdirPath = Path.of(workspace.workdir)
        val entries = buildList {
            inventory.apkFiles.forEach { apkSourcePath ->
                addAll(listApkEntries(workdirPath.resolve(apkSourcePath).normalize(), apkSourcePath))
            }
        }
        val normalizedEntries = normalizeResourceEntries(entries)
        store.saveResourceEntryIndex(
            workdir = workspace.workdir,
            targetId = workspace.activeTargetId,
            resourceEntryIndex = ResourceEntryIndexRecord(
                generatedAt = resourceNowUtc(),
                targetId = workspace.activeTargetId,
                toolVersion = toolVersion,
                contentFingerprint = workspace.snapshot.contentFingerprint,
                entries = normalizedEntries,
            ),
        )
        return normalizedEntries
    }

    private fun listApkEntries(apkPath: Path, sourcePath: String): List<ResourceEntry> =
        ApkModule.loadApkFile(apkPath.toFile()).use { apk ->
            val mappedEntries = apk.listResFiles()
                .flatMap { resFile -> mapApkResFile(resFile, sourcePath) }
            val mappedKeys = mappedEntries.mapNotNullTo(mutableSetOf()) { it.logicalGroupingKey() }
            val unresolvedEntries = apk.tableBlock.resources
                .asSequence()
                .map { resource ->
                    ResourceEntry(
                        resourceId = resource.hexId,
                        type = resource.type,
                        name = resource.name,
                        filePath = null,
                        sourcePath = sourcePath,
                        sourceEntry = null,
                        resolution = unresolvedResourceResolution(resource.any() != null),
                    )
                }
                .filter { entry -> entry.logicalGroupingKey() !in mappedKeys }
                .toList()
            (mappedEntries + unresolvedEntries).map(ResourceEntry::normalizedResolution)
        }

    private fun mapApkResFile(resFile: ResFile, sourcePath: String): List<ResourceEntry> {
        val filePath = resFile.filePath ?: return emptyList()
        return buildList {
            resFile.forEach { entry ->
                val resourceEntry = entry.resourceEntry
                add(
                    ResourceEntry(
                        resourceId = resourceEntry.hexId,
                        type = entry.typeName,
                        name = entry.name,
                        filePath = filePath,
                        sourcePath = sourcePath,
                        sourceEntry = filePath,
                        resolution = ResourceResolution.TableBacked,
                    ),
                )
            }
        }
    }

}

internal fun normalizeResourceEntries(entries: List<ResourceEntry>): List<ResourceEntry> {
    val normalized = entries.map(ResourceEntry::normalizedResolution)
    val grouped = linkedMapOf<LogicalResourceKey, MutableList<ResourceEntry>>()
    val passthrough = mutableListOf<ResourceEntry>()
    normalized.forEach { entry ->
        val key = entry.logicalGroupingKey()
        if (key == null) {
            passthrough += entry
        } else {
            grouped.getOrPut(key) { mutableListOf() } += entry
        }
    }
    val collapsed = grouped.values.map { candidates ->
        candidates.minWithOrNull(resourceEntryPreferenceOrder)
            ?: error("unexpected empty candidate set")
    }
    return (collapsed + passthrough)
        .distinct()
        .sortedWith(resourceEntrySortOrder)
}

internal sealed interface LogicalResourceKey {
    data class ById(
        val sourcePath: String,
        val type: String,
        val resourceId: String,
    ) : LogicalResourceKey

    data class ByName(
        val sourcePath: String,
        val type: String,
        val name: String,
    ) : LogicalResourceKey
}

private fun ResourceEntry.logicalGroupingKey(): LogicalResourceKey? =
    when {
        !sourcePath.isNullOrBlank() && !type.isNullOrBlank() && !resourceId.isNullOrBlank() ->
            LogicalResourceKey.ById(
                sourcePath = sourcePath,
                type = type,
                resourceId = resourceId,
            )

        !sourcePath.isNullOrBlank() && !type.isNullOrBlank() && !name.isNullOrBlank() ->
            LogicalResourceKey.ByName(
                sourcePath = sourcePath,
                type = type,
                name = name,
            )

        else -> null
    }

private val resourceEntrySortOrder =
    compareBy<ResourceEntry>(
        { it.type.orEmpty() },
        { it.name.orEmpty() },
        { it.filePath.orEmpty() },
        { it.sourcePath.orEmpty() },
        { it.sourceEntry.orEmpty() },
        { it.resolution.name },
        { it.resourceId.orEmpty() },
    )

private val resourceEntryPreferenceOrder =
    compareBy<ResourceEntry>(
        { resolutionRank(it.resolution) },
        { -resourceCompleteness(it) },
        { it.filePath.orEmpty().length },
        { it.filePath.orEmpty() },
        { it.sourceEntry.orEmpty() },
        { it.name.orEmpty() },
        { it.resourceId.orEmpty() },
    )

private fun resolutionRank(resolution: ResourceResolution): Int =
    when (resolution) {
        ResourceResolution.TableBacked -> 0
        ResourceResolution.PathInferred -> 1
        ResourceResolution.TableValue -> 2
        ResourceResolution.Unresolved -> 3
        ResourceResolution.TableHole -> 4
    }

private fun resourceCompleteness(entry: ResourceEntry): Int =
    listOf(entry.name, entry.filePath, entry.sourcePath, entry.sourceEntry, entry.resourceId)
        .count { !it.isNullOrBlank() }

private fun unresolvedResourceResolution(hasEntryPayload: Boolean): ResourceResolution =
    if (hasEntryPayload) ResourceResolution.TableValue else ResourceResolution.TableHole
