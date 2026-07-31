package io.github.dexclub.core.impl.resource

import com.reandroid.apk.ApkModule
import com.reandroid.arsc.chunk.TableBlock
import io.github.dexclub.core.api.resource.ResourceEntry
import io.github.dexclub.core.api.resource.ResourceBag
import io.github.dexclub.core.api.resource.ResourceBagItem
import io.github.dexclub.core.api.resource.ResourceTypedValue
import io.github.dexclub.core.api.resource.ResourceValueVariant
import io.github.dexclub.core.api.resource.ResourceResolution
import io.github.dexclub.core.api.resource.ResourceTableResult
import io.github.dexclub.core.api.resource.normalizedResolution
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.impl.workspace.model.ResourceTableCacheRecord
import io.github.dexclub.core.impl.workspace.model.ResourceBagItemRecord
import io.github.dexclub.core.impl.workspace.model.ResourceBagRecord
import io.github.dexclub.core.impl.workspace.model.ResourceTablePayloadRecord
import io.github.dexclub.core.impl.workspace.model.ResourceTableValueRecord
import io.github.dexclub.core.impl.workspace.model.ResourceTypedValueRecord
import io.github.dexclub.core.impl.workspace.model.ResourceValueVariantRecord
import io.github.dexclub.core.impl.workspace.model.resourceTableCacheSchemaVersion
import io.github.dexclub.core.impl.workspace.model.resourceTableFormat
import io.github.dexclub.core.impl.workspace.model.MaterialInventory
import io.github.dexclub.core.impl.workspace.store.WorkspaceStore

internal class DefaultResourceTableExecutor(
    private val store: WorkspaceStore,
    private val loader: ResourceTableLoader,
    private val toolVersion: String,
) : ResourceTableExecutor {
    override fun dumpResourceTable(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
    ): ResourceTableResult {
        val source = loader.resolveSource(inventory)
        val sourceFingerprint = resourceSourceFingerprint(workspace.workdir, source.sourcePath)
        store.loadResourceTableCache(workspace.workdir, workspace.activeTargetId)
            ?.takeIf {
                it.schemaVersion == resourceTableCacheSchemaVersion &&
                    it.format == resourceTableFormat &&
                    it.toolVersion == toolVersion &&
                it.sourcePath == source.sourcePath &&
                    it.sourceEntry == source.sourceEntry &&
                    it.sourceFingerprint == sourceFingerprint
            }
            ?.let(::toResult)
            ?.let { return it }

        val loaded = loader.load(workspace, source)
        val result = buildResult(
            tableBlock = loaded.tableBlock,
            sourcePath = loaded.sourcePath,
            sourceEntry = loaded.sourceEntry,
        )
        store.saveResourceTableCache(
            workdir = workspace.workdir,
            targetId = workspace.activeTargetId,
            resourceTableCache = ResourceTableCacheRecord(
                generatedAt = resourceNowUtc(),
                targetId = workspace.activeTargetId,
                toolVersion = toolVersion,
                sourcePath = result.sourcePath ?: source.sourcePath,
                sourceEntry = result.sourceEntry,
                sourceFingerprint = sourceFingerprint,
                payload = ResourceTablePayloadRecord(
                    packages = loaded.tableBlock.resources
                        .asSequence()
                        .map { it.packageName }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                        .toList(),
                    typeCount = result.typeCount,
                    entries = result.entries,
                    values = loaded.tableBlock.resources
                        .asSequence()
                        .mapNotNull { resource ->
                            val decoded = resource.toResourceValueOrNull() ?: return@mapNotNull null
                            if (decoded.variants.isEmpty()) return@mapNotNull null
                            ResourceTableValueRecord(
                                resourceId = decoded.resourceId,
                                packageName = decoded.packageName,
                                type = decoded.type,
                                name = decoded.name,
                                variants = decoded.variants.map { it.toRecord() },
                            )
                        }
                        .sortedWith(
                            compareBy<ResourceTableValueRecord>(
                                { it.resourceId.orEmpty() },
                                { it.packageName.orEmpty() },
                                { it.type.orEmpty() },
                                { it.name.orEmpty() },
                            ),
                        )
                        .toList(),
                ),
            ),
        )
        return result
    }

    private fun buildResult(
        tableBlock: TableBlock,
        sourcePath: String,
        sourceEntry: String?,
    ): ResourceTableResult {
        val entries = tableBlock.resources
            .asSequence()
            .map { resource ->
                ResourceEntry(
                    resourceId = resource.hexId,
                    packageName = resource.packageName,
                    type = resource.type,
                    name = resource.name,
                    filePath = null,
                    sourcePath = sourcePath,
                    sourceEntry = sourceEntry,
                    resolution = if (resource.iterator().hasNext()) ResourceResolution.TableValue else ResourceResolution.TableHole,
                )
            }
            .sortedWith(
                compareBy<ResourceEntry>(
                    { it.resourceId.orEmpty() },
                    { it.packageName.orEmpty() },
                    { it.type.orEmpty() },
                    { it.name.orEmpty() },
                ),
            )
            .toList()
        val typeCount = tableBlock.resources
            .asSequence()
            .map { resource -> resource.packageName to resource.type }
            .distinct()
            .count()
        return ResourceTableResult(
            sourcePath = sourcePath,
            sourceEntry = sourceEntry,
            packageCount = tableBlock.size(),
            typeCount = typeCount,
            entryCount = entries.size,
            entries = entries,
        )
    }

    private fun toResult(cache: ResourceTableCacheRecord): ResourceTableResult =
        ResourceTableResult(
            sourcePath = cache.sourcePath,
            sourceEntry = cache.sourceEntry,
            packageCount = cache.payload.packages.size,
            typeCount = cache.payload.typeCount,
            entryCount = cache.payload.entries.size,
            entries = cache.payload.entries.map(ResourceEntry::normalizedResolution),
        )

    private fun ResourceValueVariant.toRecord(): ResourceValueVariantRecord =
        ResourceValueVariantRecord(
            qualifiers = configuration.qualifiers,
            isDefault = configuration.isDefault,
            value = value?.toRecord(),
            bag = bag?.toRecord(),
        )

    private fun ResourceTypedValue.toRecord(): ResourceTypedValueRecord =
        ResourceTypedValueRecord(valueType, rawData, rawDataHex, decodedValue, referencedResourceId)

    private fun ResourceBag.toRecord(): ResourceBagRecord =
        ResourceBagRecord(
            kind = kind.name,
            parentResourceId = parentResourceId,
            parentResourceName = parentResourceName,
            items = items.map { it.toRecord() },
        )

    private fun ResourceBagItem.toRecord(): ResourceBagItemRecord =
        ResourceBagItemRecord(
            rawKey, keyResourceId, keyName, index, quantity, attributeType, attributeFormats, value.toRecord(),
        )
}
