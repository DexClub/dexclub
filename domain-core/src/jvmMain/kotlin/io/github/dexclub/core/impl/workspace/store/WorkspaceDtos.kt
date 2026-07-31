package io.github.dexclub.core.impl.workspace.store

import io.github.dexclub.core.api.shared.CapabilitySet
import io.github.dexclub.core.api.shared.InputType
import io.github.dexclub.core.api.shared.WorkspaceKind
import io.github.dexclub.core.api.resource.ResourceEntry
import io.github.dexclub.core.api.resource.ResourceResolution
import io.github.dexclub.core.impl.workspace.model.ClassSourceMapRecord
import io.github.dexclub.core.impl.workspace.model.ClassSourceRefRecord
import io.github.dexclub.core.impl.workspace.model.ManifestCacheRecord
import io.github.dexclub.core.impl.workspace.model.DecodedXmlCacheRecord
import io.github.dexclub.core.impl.workspace.model.MaterialInventory
import io.github.dexclub.core.impl.workspace.model.ResourceEntryIndexRecord
import io.github.dexclub.core.impl.workspace.model.ResourceBagItemRecord
import io.github.dexclub.core.impl.workspace.model.ResourceBagRecord
import io.github.dexclub.core.impl.workspace.model.ResourceTableCacheRecord
import io.github.dexclub.core.impl.workspace.model.ResourceTablePayloadRecord
import io.github.dexclub.core.impl.workspace.model.ResourceTableValueRecord
import io.github.dexclub.core.impl.workspace.model.ResourceTypedValueRecord
import io.github.dexclub.core.impl.workspace.model.ResourceValueVariantRecord
import io.github.dexclub.core.impl.workspace.model.SnapshotRecord
import io.github.dexclub.core.impl.workspace.model.TargetRecord
import io.github.dexclub.core.impl.workspace.model.WorkspaceRecord
import kotlinx.serialization.Serializable

@Serializable
internal data class WorkspaceDto(
    val schemaVersion: Int,
    val layoutVersion: Int,
    val workspaceId: String,
    val createdAt: String,
    val updatedAt: String,
    val toolVersion: String,
    val activeTargetId: String,
)

@Serializable
internal data class TargetDto(
    val schemaVersion: Int,
    val targetId: String,
    val createdAt: String,
    val updatedAt: String,
    val inputType: String,
    val inputPath: String,
)

@Serializable
internal data class SnapshotDto(
    val schemaVersion: Int,
    val generatedAt: String,
    val targetId: String,
    val inputPath: String,
    val kind: String,
    val inventory: MaterialInventoryDto,
    val inventoryFingerprint: String,
    val contentFingerprint: String,
    val capabilities: CapabilitySetDto,
)

@Serializable
internal data class MaterialInventoryDto(
    val apkFiles: List<String> = emptyList(),
    val dexFiles: List<String> = emptyList(),
    val manifestFiles: List<String> = emptyList(),
    val arscFiles: List<String> = emptyList(),
    val binaryXmlFiles: List<String> = emptyList(),
)

@Serializable
internal data class CapabilitySetDto(
    val inspect: Boolean = true,
    val findClass: Boolean = false,
    val findMethod: Boolean = false,
    val findField: Boolean = false,
    val exportDex: Boolean = false,
    val exportSmali: Boolean = false,
    val exportJava: Boolean = false,
    val manifestDecode: Boolean = false,
    val resourceTableDecode: Boolean = false,
    val xmlDecode: Boolean = false,
    val resourceEntryList: Boolean = false,
)

@Serializable
internal data class ClassSourceRefDto(
    val id: Int,
    val sourcePath: String,
    val sourceEntry: String? = null,
)

@Serializable
internal data class ClassSourceMapDto(
    val schemaVersion: Int,
    val generatedAt: String,
    val targetId: String,
    val toolVersion: String,
    val contentFingerprint: String,
    val format: String,
    val sources: List<ClassSourceRefDto> = emptyList(),
    val mappings: Map<String, Int>,
)

@Serializable
internal data class ManifestCacheDto(
    val schemaVersion: Int,
    val generatedAt: String,
    val targetId: String,
    val toolVersion: String,
    val sourcePath: String,
    val sourceEntry: String? = null,
    val sourceFingerprint: String,
    val format: String,
    val text: String,
)

@Serializable
internal data class ResourceEntryDto(
    val resourceId: String? = null,
    val packageName: String? = null,
    val type: String? = null,
    val name: String? = null,
    val filePath: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val resolution: String,
)

@Serializable
internal data class ResourceTablePayloadDto(
    val packages: List<String> = emptyList(),
    val typeCount: Int,
    val entries: List<ResourceEntryDto> = emptyList(),
    val values: List<ResourceTableValueDto> = emptyList(),
)

@Serializable
internal data class ResourceTableValueDto(
    val resourceId: String? = null,
    val packageName: String? = null,
    val type: String? = null,
    val name: String? = null,
    val variants: List<ResourceValueVariantDto> = emptyList(),
)

@Serializable
internal data class ResourceValueVariantDto(
    val qualifiers: String,
    val isDefault: Boolean,
    val value: ResourceTypedValueDto? = null,
    val bag: ResourceBagDto? = null,
)

@Serializable
internal data class ResourceTypedValueDto(
    val valueType: String,
    val rawData: Int,
    val rawDataHex: String,
    val decodedValue: String? = null,
    val referencedResourceId: String? = null,
)

@Serializable
internal data class ResourceBagDto(
    val kind: String,
    val parentResourceId: String? = null,
    val parentResourceName: String? = null,
    val items: List<ResourceBagItemDto> = emptyList(),
)

@Serializable
internal data class ResourceBagItemDto(
    val rawKey: String,
    val keyResourceId: String? = null,
    val keyName: String? = null,
    val index: Int? = null,
    val quantity: String? = null,
    val attributeType: String? = null,
    val attributeFormats: List<String>? = null,
    val value: ResourceTypedValueDto,
)

@Serializable
internal data class ResourceTableCacheDto(
    val schemaVersion: Int,
    val generatedAt: String,
    val targetId: String,
    val toolVersion: String,
    val sourcePath: String,
    val sourceEntry: String? = null,
    val sourceFingerprint: String,
    val format: String,
    val payload: ResourceTablePayloadDto,
)

@Serializable
internal data class DecodedXmlCacheDto(
    val schemaVersion: Int,
    val generatedAt: String,
    val targetId: String,
    val toolVersion: String,
    val sourcePath: String,
    val sourceEntry: String? = null,
    val sourceFingerprint: String,
    val format: String,
    val text: String,
)

@Serializable
internal data class ResourceEntryIndexDto(
    val schemaVersion: Int,
    val generatedAt: String,
    val targetId: String,
    val toolVersion: String,
    val contentFingerprint: String,
    val format: String,
    val entries: List<ResourceEntryDto> = emptyList(),
)

internal fun WorkspaceDto.toRecord(): WorkspaceRecord =
    WorkspaceRecord(
        schemaVersion = schemaVersion,
        layoutVersion = layoutVersion,
        workspaceId = workspaceId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        toolVersion = toolVersion,
        activeTargetId = activeTargetId,
    )

internal fun WorkspaceRecord.toDto(): WorkspaceDto =
    WorkspaceDto(
        schemaVersion = schemaVersion,
        layoutVersion = layoutVersion,
        workspaceId = workspaceId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        toolVersion = toolVersion,
        activeTargetId = activeTargetId,
    )

internal fun TargetDto.toRecord(): TargetRecord =
    TargetRecord(
        schemaVersion = schemaVersion,
        targetId = targetId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        inputType = inputType.toInputType(),
        inputPath = inputPath,
    )

internal fun TargetRecord.toDto(): TargetDto =
    TargetDto(
        schemaVersion = schemaVersion,
        targetId = targetId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        inputType = inputType.toStorageValue(),
        inputPath = inputPath,
    )

internal fun SnapshotDto.toRecord(): SnapshotRecord =
    SnapshotRecord(
        schemaVersion = schemaVersion,
        generatedAt = generatedAt,
        targetId = targetId,
        inputPath = inputPath,
        kind = kind.toWorkspaceKind(),
        inventory = inventory.toModel(),
        inventoryFingerprint = inventoryFingerprint,
        contentFingerprint = contentFingerprint,
        capabilities = capabilities.toModel(),
    )

internal fun SnapshotRecord.toDto(): SnapshotDto =
    SnapshotDto(
        schemaVersion = schemaVersion,
        generatedAt = generatedAt,
        targetId = targetId,
        inputPath = inputPath,
        kind = kind.toStorageValue(),
        inventory = inventory.toDto(),
        inventoryFingerprint = inventoryFingerprint,
        contentFingerprint = contentFingerprint,
        capabilities = capabilities.toDto(),
    )

internal fun MaterialInventoryDto.toModel(): MaterialInventory =
    MaterialInventory(
        apkFiles = apkFiles,
        dexFiles = dexFiles,
        manifestFiles = manifestFiles,
        arscFiles = arscFiles,
        binaryXmlFiles = binaryXmlFiles,
    )

internal fun MaterialInventory.toDto(): MaterialInventoryDto =
    MaterialInventoryDto(
        apkFiles = apkFiles,
        dexFiles = dexFiles,
        manifestFiles = manifestFiles,
        arscFiles = arscFiles,
        binaryXmlFiles = binaryXmlFiles,
    )

internal fun CapabilitySetDto.toModel(): CapabilitySet =
    CapabilitySet(
        inspect = inspect,
        findClass = findClass,
        findMethod = findMethod,
        findField = findField,
        exportDex = exportDex,
        exportSmali = exportSmali,
        exportJava = exportJava,
        manifestDecode = manifestDecode,
        resourceTableDecode = resourceTableDecode,
        xmlDecode = xmlDecode,
        resourceEntryList = resourceEntryList,
    )

internal fun CapabilitySet.toDto(): CapabilitySetDto =
    CapabilitySetDto(
        inspect = inspect,
        findClass = findClass,
        findMethod = findMethod,
        findField = findField,
        exportDex = exportDex,
        exportSmali = exportSmali,
        exportJava = exportJava,
        manifestDecode = manifestDecode,
        resourceTableDecode = resourceTableDecode,
        xmlDecode = xmlDecode,
        resourceEntryList = resourceEntryList,
    )

internal fun ClassSourceMapDto.toRecord(): ClassSourceMapRecord =
    ClassSourceMapRecord(
        schemaVersion = schemaVersion,
        generatedAt = generatedAt,
        targetId = targetId,
        toolVersion = toolVersion,
        contentFingerprint = contentFingerprint,
        format = format,
        sources = sources.map(ClassSourceRefDto::toRecord),
        mappings = mappings,
    )

internal fun ClassSourceMapRecord.toDto(): ClassSourceMapDto =
    ClassSourceMapDto(
        schemaVersion = schemaVersion,
        generatedAt = generatedAt,
        targetId = targetId,
        toolVersion = toolVersion,
        contentFingerprint = contentFingerprint,
        format = format,
        sources = sources.map(ClassSourceRefRecord::toDto),
        mappings = mappings,
    )

internal fun ClassSourceRefDto.toRecord(): ClassSourceRefRecord =
    ClassSourceRefRecord(
        id = id,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )

internal fun ClassSourceRefRecord.toDto(): ClassSourceRefDto =
    ClassSourceRefDto(
        id = id,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )

internal fun ManifestCacheDto.toRecord(): ManifestCacheRecord =
    ManifestCacheRecord(
        schemaVersion = schemaVersion,
        generatedAt = generatedAt,
        targetId = targetId,
        toolVersion = toolVersion,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
        sourceFingerprint = sourceFingerprint,
        format = format,
        text = text,
    )

internal fun ManifestCacheRecord.toDto(): ManifestCacheDto =
    ManifestCacheDto(
        schemaVersion = schemaVersion,
        generatedAt = generatedAt,
        targetId = targetId,
        toolVersion = toolVersion,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
        sourceFingerprint = sourceFingerprint,
        format = format,
        text = text,
    )

internal fun ResourceEntryDto.toModel(): ResourceEntry =
    ResourceEntry(
        resourceId = resourceId,
        packageName = packageName,
        type = type,
        name = name,
        filePath = filePath,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
        resolution = resolution.toResourceResolution(),
    )

internal fun ResourceEntry.toDto(): ResourceEntryDto =
    ResourceEntryDto(
        resourceId = resourceId,
        packageName = packageName,
        type = type,
        name = name,
        filePath = filePath,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
        resolution = resolution.toStorageValue(),
    )

internal fun ResourceTablePayloadDto.toModel(): ResourceTablePayloadRecord =
    ResourceTablePayloadRecord(
        packages = packages,
        typeCount = typeCount,
        entries = entries.map(ResourceEntryDto::toModel),
        values = values.map(ResourceTableValueDto::toModel),
    )

internal fun ResourceTablePayloadRecord.toDto(): ResourceTablePayloadDto =
    ResourceTablePayloadDto(
        packages = packages,
        typeCount = typeCount,
        entries = entries.map(ResourceEntry::toDto),
        values = values.map(ResourceTableValueRecord::toDto),
    )

internal fun ResourceTableValueDto.toModel(): ResourceTableValueRecord =
    ResourceTableValueRecord(
        resourceId = resourceId,
        packageName = packageName,
        type = type,
        name = name,
        variants = variants.map(ResourceValueVariantDto::toModel),
    )

internal fun ResourceTableValueRecord.toDto(): ResourceTableValueDto =
    ResourceTableValueDto(
        resourceId = resourceId,
        packageName = packageName,
        type = type,
        name = name,
        variants = variants.map(ResourceValueVariantRecord::toDto),
    )

internal fun ResourceValueVariantDto.toModel(): ResourceValueVariantRecord =
    ResourceValueVariantRecord(qualifiers, isDefault, value?.toModel(), bag?.toModel())

internal fun ResourceValueVariantRecord.toDto(): ResourceValueVariantDto =
    ResourceValueVariantDto(qualifiers, isDefault, value?.toDto(), bag?.toDto())

internal fun ResourceTypedValueDto.toModel(): ResourceTypedValueRecord =
    ResourceTypedValueRecord(valueType, rawData, rawDataHex, decodedValue, referencedResourceId)

internal fun ResourceTypedValueRecord.toDto(): ResourceTypedValueDto =
    ResourceTypedValueDto(valueType, rawData, rawDataHex, decodedValue, referencedResourceId)

internal fun ResourceBagDto.toModel(): ResourceBagRecord =
    ResourceBagRecord(kind, parentResourceId, parentResourceName, items.map(ResourceBagItemDto::toModel))

internal fun ResourceBagRecord.toDto(): ResourceBagDto =
    ResourceBagDto(kind, parentResourceId, parentResourceName, items.map(ResourceBagItemRecord::toDto))

internal fun ResourceBagItemDto.toModel(): ResourceBagItemRecord =
    ResourceBagItemRecord(
        rawKey, keyResourceId, keyName, index, quantity, attributeType, attributeFormats, value.toModel(),
    )

internal fun ResourceBagItemRecord.toDto(): ResourceBagItemDto =
    ResourceBagItemDto(
        rawKey, keyResourceId, keyName, index, quantity, attributeType, attributeFormats, value.toDto(),
    )

internal fun ResourceTableCacheDto.toRecord(): ResourceTableCacheRecord =
    ResourceTableCacheRecord(
        schemaVersion = schemaVersion,
        generatedAt = generatedAt,
        targetId = targetId,
        toolVersion = toolVersion,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
        sourceFingerprint = sourceFingerprint,
        format = format,
        payload = payload.toModel(),
    )

internal fun ResourceTableCacheRecord.toDto(): ResourceTableCacheDto =
    ResourceTableCacheDto(
        schemaVersion = schemaVersion,
        generatedAt = generatedAt,
        targetId = targetId,
        toolVersion = toolVersion,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
        sourceFingerprint = sourceFingerprint,
        format = format,
        payload = payload.toDto(),
    )

internal fun DecodedXmlCacheDto.toRecord(): DecodedXmlCacheRecord =
    DecodedXmlCacheRecord(
        schemaVersion = schemaVersion,
        generatedAt = generatedAt,
        targetId = targetId,
        toolVersion = toolVersion,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
        sourceFingerprint = sourceFingerprint,
        format = format,
        text = text,
    )

internal fun DecodedXmlCacheRecord.toDto(): DecodedXmlCacheDto =
    DecodedXmlCacheDto(
        schemaVersion = schemaVersion,
        generatedAt = generatedAt,
        targetId = targetId,
        toolVersion = toolVersion,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
        sourceFingerprint = sourceFingerprint,
        format = format,
        text = text,
    )

internal fun ResourceEntryIndexDto.toRecord(): ResourceEntryIndexRecord =
    ResourceEntryIndexRecord(
        schemaVersion = schemaVersion,
        generatedAt = generatedAt,
        targetId = targetId,
        toolVersion = toolVersion,
        contentFingerprint = contentFingerprint,
        format = format,
        entries = entries.map(ResourceEntryDto::toModel),
    )

internal fun ResourceEntryIndexRecord.toDto(): ResourceEntryIndexDto =
    ResourceEntryIndexDto(
        schemaVersion = schemaVersion,
        generatedAt = generatedAt,
        targetId = targetId,
        toolVersion = toolVersion,
        contentFingerprint = contentFingerprint,
        format = format,
        entries = entries.map(ResourceEntry::toDto),
    )

private fun InputType.toStorageValue(): String =
    when (this) {
        InputType.File -> "file"
    }

private fun String.toInputType(): InputType =
    when (lowercase()) {
        "file" -> InputType.File
        else -> throw IllegalArgumentException("Unsupported input type: $this")
    }

private fun WorkspaceKind.toStorageValue(): String =
    when (this) {
        WorkspaceKind.Apk -> "apk"
        WorkspaceKind.Dex -> "dex"
        WorkspaceKind.Manifest -> "manifest"
        WorkspaceKind.Arsc -> "arsc"
        WorkspaceKind.Axml -> "axml"
    }

private fun String.toWorkspaceKind(): WorkspaceKind =
    when (lowercase()) {
        "apk" -> WorkspaceKind.Apk
        "dex" -> WorkspaceKind.Dex
        "manifest" -> WorkspaceKind.Manifest
        "arsc" -> WorkspaceKind.Arsc
        "axml" -> WorkspaceKind.Axml
        else -> throw IllegalArgumentException("Unsupported workspace kind: $this")
    }

private fun ResourceResolution.toStorageValue(): String =
    when (this) {
        ResourceResolution.TableBacked -> "table-backed"
        ResourceResolution.TableValue -> "table-value"
        ResourceResolution.Unresolved -> "unresolved"
        ResourceResolution.TableHole -> "table-hole"
    }

private fun String.toResourceResolution(): ResourceResolution =
    when (lowercase()) {
        "table-backed" -> ResourceResolution.TableBacked
        "table-value" -> ResourceResolution.TableValue
        "unresolved" -> ResourceResolution.Unresolved
        "table-hole" -> ResourceResolution.TableHole
        else -> throw IllegalArgumentException("Unsupported resource resolution: $this")
    }
