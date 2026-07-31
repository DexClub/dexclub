package io.github.dexclub.core.impl.resource

import io.github.dexclub.core.api.resource.FindResourcesRequest
import io.github.dexclub.core.api.resource.ResolveResourceRequest
import io.github.dexclub.core.api.resource.ResourceDecodeError
import io.github.dexclub.core.api.resource.ResourceDecodeErrorReason
import io.github.dexclub.core.api.resource.ResourceEntryValueHit
import io.github.dexclub.core.api.resource.ResourceBag
import io.github.dexclub.core.api.resource.ResourceBagItem
import io.github.dexclub.core.api.resource.ResourceBagKind
import io.github.dexclub.core.api.resource.ResourceConfiguration
import io.github.dexclub.core.api.resource.ResourceTypedValue
import io.github.dexclub.core.api.resource.ResourceValue
import io.github.dexclub.core.api.resource.ResourceValueCandidate
import io.github.dexclub.core.api.resource.ResourceValueVariant
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.impl.workspace.model.MaterialInventory
import io.github.dexclub.core.impl.workspace.model.ResourceTableCacheRecord
import io.github.dexclub.core.impl.workspace.model.ResourceBagItemRecord
import io.github.dexclub.core.impl.workspace.model.ResourceBagRecord
import io.github.dexclub.core.impl.workspace.model.ResourceTypedValueRecord
import io.github.dexclub.core.impl.workspace.model.ResourceValueVariantRecord
import io.github.dexclub.core.impl.workspace.model.resourceTableCacheSchemaVersion
import io.github.dexclub.core.impl.workspace.model.resourceTableFormat
import io.github.dexclub.core.impl.workspace.store.WorkspaceStore

internal class DefaultResourceValueExecutor(
    private val store: WorkspaceStore,
    private val tableLoader: ResourceTableLoader,
    private val queryParser: ResourceSearchQueryParser,
    private val toolVersion: String,
) : ResourceValueExecutor {
    override fun getResourceValue(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        request: ResolveResourceRequest,
    ): ResourceValue {
        loadValidCache(workspace, inventory)?.let { cache ->
            return resolveFromCache(cache, request)
        }

        val loaded = tableLoader.load(workspace, inventory)
        val resource = when {
            request.resourceId != null -> {
                val resourceId = parseResourceId(request.resourceId)
                loaded.tableBlock.getResource(resourceId)
            }

            request.type != null && request.name != null -> {
                loaded.tableBlock.resources
                    .asSequence()
                    .filter {
                        it.type == request.type && it.name == request.name &&
                            (request.packageName == null || it.packageName == request.packageName)
                    }
                    .toList()
                    .let { matches ->
                        when (matches.size) {
                            0 -> null
                            1 -> matches.single()
                            else -> throw ResourceDecodeError(
                                reason = ResourceDecodeErrorReason.ResourceValueAmbiguous,
                                sourcePath = loaded.sourcePath,
                                candidates = matches.map {
                                    ResourceValueCandidate(
                                        resourceId = it.hexId,
                                        packageName = it.packageName,
                                        type = it.type,
                                        name = it.name,
                                        sourcePath = loaded.sourcePath,
                                        sourceEntry = loaded.sourceEntry,
                                    )
                                },
                                message = "Resource is ambiguous: ${request.type}/${request.name}",
                            )
                        }
                    }
            }

            else -> throw ResourceDecodeError(
                reason = ResourceDecodeErrorReason.ResourceValueInvalidSelector,
                message = "Resolve resource request is missing a valid selector",
            )
        } ?: throw ResourceDecodeError(
            reason = ResourceDecodeErrorReason.ResourceValueNotFound,
            sourcePath = loaded.sourcePath,
            message = buildNotFoundMessage(request),
        )

        if (!resource.iterator().hasNext()) {
            throw ResourceDecodeError(
                reason = ResourceDecodeErrorReason.ResourceValueNotFound,
                sourcePath = loaded.sourcePath,
                message = buildNotFoundMessage(request),
            )
        }
        return resource.toResourceValue().selectVariants(request, loaded.sourcePath)
    }

    override fun findResourceValues(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        request: FindResourcesRequest,
    ): List<ResourceEntryValueHit> {
        loadValidCache(workspace, inventory)?.let { cache ->
            return findFromCache(cache, request)
        }

        val loaded = tableLoader.load(workspace, inventory)
        val query = queryParser.parse(request.queryText)
        return loaded.tableBlock.resources
            .asSequence()
            .filter { resource -> resource.type == query.resourceType }
            .filter { resource -> query.packageName == null || resource.packageName == query.packageName }
            .flatMap { resource ->
                findMatches(resource.toResourceValue(), query, loaded.sourcePath, loaded.sourceEntry).asSequence()
            }
            .toList()
    }

    private fun loadValidCache(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
    ): ResourceTableCacheRecord? {
        val source = loaderOrNull(workspace, inventory) ?: return null
        val sourceFingerprint = resourceSourceFingerprint(workspace.workdir, source.sourcePath)
        return store.loadResourceTableCache(workspace.workdir, workspace.activeTargetId)
            ?.takeIf {
                it.schemaVersion == resourceTableCacheSchemaVersion &&
                    it.format == resourceTableFormat &&
                    it.toolVersion == toolVersion &&
                it.sourcePath == source.sourcePath &&
                    it.sourceEntry == source.sourceEntry &&
                    it.sourceFingerprint == sourceFingerprint
            }
    }

    private fun loaderOrNull(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
    ): ResourceTableSource? =
        try {
            tableLoader.resolveSource(inventory)
        } catch (_: ResourceDecodeError) {
            null
        }

    private fun resolveFromCache(
        cache: ResourceTableCacheRecord,
        request: ResolveResourceRequest,
    ): ResourceValue {
        val valueRecord = when {
            request.resourceId != null -> {
                val normalizedResourceId = normalizeResourceId(request.resourceId)
                cache.payload.values.firstOrNull { it.resourceId.equals(normalizedResourceId, ignoreCase = true) }
            }

            request.type != null && request.name != null -> {
                cache.payload.values
                    .filter {
                        it.type == request.type && it.name == request.name &&
                            (request.packageName == null || it.packageName == request.packageName)
                    }
                    .let { matches ->
                        when (matches.size) {
                            0 -> null
                            1 -> matches.single()
                            else -> throw ResourceDecodeError(
                                reason = ResourceDecodeErrorReason.ResourceValueAmbiguous,
                                sourcePath = cache.sourcePath,
                                candidates = matches.map {
                                    ResourceValueCandidate(
                                        resourceId = it.resourceId,
                                        packageName = it.packageName,
                                        type = it.type,
                                        name = it.name,
                                        sourcePath = cache.sourcePath,
                                        sourceEntry = cache.sourceEntry,
                                    )
                                },
                                message = "Resource is ambiguous: ${request.type}/${request.name}",
                            )
                        }
                    }
            }

            else -> throw ResourceDecodeError(
                reason = ResourceDecodeErrorReason.ResourceValueInvalidSelector,
                message = "Resolve resource request is missing a valid selector",
            )
        } ?: throw ResourceDecodeError(
            reason = ResourceDecodeErrorReason.ResourceValueNotFound,
            sourcePath = cache.sourcePath,
            message = buildNotFoundMessage(request),
        )

        return ResourceValue(
            resourceId = valueRecord.resourceId,
            packageName = valueRecord.packageName,
            type = valueRecord.type ?: request.type.orEmpty(),
            name = valueRecord.name ?: request.name.orEmpty(),
            variants = valueRecord.variants.map { it.toApi() },
        ).selectVariants(request, cache.sourcePath)
    }

    private fun findFromCache(
        cache: ResourceTableCacheRecord,
        request: FindResourcesRequest,
    ): List<ResourceEntryValueHit> {
        val query = queryParser.parse(request.queryText)
        return cache.payload.values
            .asSequence()
            .filter { it.type == query.resourceType }
            .filter { query.packageName == null || it.packageName == query.packageName }
            .flatMap { valueRecord ->
                findMatches(valueRecord.toApiValue(), query, cache.sourcePath, cache.sourceEntry).asSequence()
            }
            .toList()
    }

    private fun parseResourceId(text: String): Int =
        runCatching {
            text.trim()
                .removePrefix("0x")
                .removePrefix("0X")
                .toUInt(16)
                .toInt()
        }.getOrElse {
            throw ResourceDecodeError(
                reason = ResourceDecodeErrorReason.ResourceValueInvalidSelector,
                message = "Invalid resource id: $text",
            )
        }

    private fun normalizeResourceId(text: String): String =
        buildString {
            append("0x")
            append(parseResourceId(text).toUInt().toString(16).padStart(8, '0'))
        }

    private fun buildNotFoundMessage(request: ResolveResourceRequest): String =
        when {
            request.resourceId != null -> "Resource not found: ${request.resourceId}"
            request.type != null && request.name != null -> "Resource not found: ${request.type}/${request.name}"
            else -> "Resource not found"
        }

    private fun matches(value: String, query: ResourceSearchQuery): Boolean =
        if (query.contains) {
            value.contains(query.value, ignoreCase = query.ignoreCase)
        } else {
            value.equals(query.value, ignoreCase = query.ignoreCase)
        }

    private fun io.github.dexclub.core.impl.workspace.model.ResourceTableValueRecord.toApiValue(): ResourceValue =
        ResourceValue(
            resourceId = resourceId,
            packageName = packageName,
            type = type.orEmpty(),
            name = name.orEmpty(),
            variants = variants.map { it.toApi() },
        )

    private fun findMatches(
        resource: ResourceValue,
        query: ResourceSearchQuery,
        sourcePath: String,
        sourceEntry: String?,
    ): List<ResourceEntryValueHit> =
        resource.variants
            .asSequence()
            .filter { query.qualifier == null || it.configuration.qualifiers == query.qualifier }
            .flatMap { variant ->
                val scalar = variant.value?.let { typed ->
                    matchTypedValue(typed, query)?.let { (target, matchedValue) ->
                        hit(resource, variant.configuration.qualifiers, typed.valueType, target, matchedValue, null, null, sourcePath, sourceEntry)
                    }
                }
                val bagItems = variant.bag?.items.orEmpty().asSequence().mapNotNull { item ->
                    if (query.valueKind != null &&
                        !item.value.valueType.equals(query.valueKind, ignoreCase = true)
                    ) return@mapNotNull null
                    matchBagItem(item, query)?.let { (target, matchedValue) ->
                        hit(
                            resource, variant.configuration.qualifiers, item.value.valueType, target, matchedValue,
                            item.index, item.keyName ?: item.keyResourceId ?: item.rawKey, sourcePath, sourceEntry,
                        )
                    }
                }
                listOfNotNull(scalar).asSequence() + bagItems
            }
            .toList()

    private fun matchTypedValue(typed: ResourceTypedValue, query: ResourceSearchQuery): Pair<String, String>? {
        if (query.valueKind != null && !typed.valueType.equals(query.valueKind, ignoreCase = true)) return null
        val candidates = when (query.matchTarget) {
            ResourceValueMatchTarget.DecodedValue -> listOf("decoded_value" to typed.decodedValue)
            ResourceValueMatchTarget.RawData -> listOf("raw_data" to typed.rawDataHex, "raw_data" to typed.rawData.toString())
            ResourceValueMatchTarget.Reference -> listOf("reference" to typed.referencedResourceId)
            ResourceValueMatchTarget.BagKey -> emptyList()
            ResourceValueMatchTarget.Any -> listOf(
                "decoded_value" to typed.decodedValue,
                "raw_data" to typed.rawDataHex,
                "raw_data" to typed.rawData.toString(),
                "reference" to typed.referencedResourceId,
            )
        }
        return candidates.firstOrNull { (_, value) -> value != null && matches(value, query) }
            ?.let { (target, value) -> target to requireNotNull(value) }
    }

    private fun matchBagItem(item: ResourceBagItem, query: ResourceSearchQuery): Pair<String, String>? {
        if (query.matchTarget != ResourceValueMatchTarget.BagKey) {
            matchTypedValue(item.value, query)?.let { return it }
        }
        if (query.matchTarget == ResourceValueMatchTarget.BagKey || query.matchTarget == ResourceValueMatchTarget.Any) {
            val bagKey = listOfNotNull(item.keyName, item.keyResourceId, item.rawKey, item.quantity)
                .firstOrNull { matches(it, query) }
            if (bagKey != null) return "bag_key" to bagKey
        }
        return null
    }

    private fun hit(
        resource: ResourceValue,
        qualifier: String,
        valueKind: String,
        matchTarget: String,
        value: String,
        bagIndex: Int?,
        bagKey: String?,
        sourcePath: String,
        sourceEntry: String?,
    ): ResourceEntryValueHit =
        ResourceEntryValueHit(
            resourceId = resource.resourceId,
            packageName = resource.packageName,
            type = resource.type,
            name = resource.name,
            value = value,
            qualifier = qualifier,
            valueKind = valueKind,
            matchTarget = matchTarget,
            bagIndex = bagIndex,
            bagKey = bagKey,
            sourcePath = sourcePath,
            sourceEntry = sourceEntry,
        )

    private fun ResourceValueVariantRecord.toApi(): ResourceValueVariant =
        ResourceValueVariant(
            configuration = ResourceConfiguration(qualifiers, isDefault),
            value = value?.toApi(),
            bag = bag?.toApi(),
        )

    private fun ResourceTypedValueRecord.toApi(): ResourceTypedValue =
        ResourceTypedValue(valueType, rawData, rawDataHex, decodedValue, referencedResourceId)

    private fun ResourceBagRecord.toApi(): ResourceBag =
        ResourceBag(
            kind = runCatching { ResourceBagKind.valueOf(kind) }.getOrDefault(ResourceBagKind.Unknown),
            parentResourceId = parentResourceId,
            parentResourceName = parentResourceName,
            items = items.map { it.toApi() },
        )

    private fun ResourceBagItemRecord.toApi(): ResourceBagItem =
        ResourceBagItem(
            rawKey, keyResourceId, keyName, index, quantity, attributeType, attributeFormats, value.toApi(),
        )

    private fun ResourceValue.selectVariants(request: ResolveResourceRequest, sourcePath: String): ResourceValue {
        val selected = when {
            request.qualifier != null -> variants.filter { it.configuration.qualifiers == request.qualifier }
            request.includeAllVariants -> variants
            else -> listOfNotNull(variants.firstOrNull { it.configuration.isDefault } ?: variants.firstOrNull())
        }
        if (selected.isEmpty()) {
            throw ResourceDecodeError(
                reason = ResourceDecodeErrorReason.ResourceValueNotFound,
                sourcePath = sourcePath,
                message = "Resource variant not found: ${request.qualifier}",
            )
        }
        return copy(variants = selected)
    }
}
