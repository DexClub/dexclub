package io.github.dexclub.core.app.projection

import io.github.dexclub.core.app.contract.DecodedXmlResult
import io.github.dexclub.core.app.contract.ResourceEntryValueHit
import io.github.dexclub.core.api.resource.ResourceBag
import io.github.dexclub.core.api.resource.ResourceBagItem
import io.github.dexclub.core.api.resource.ResourceConfiguration
import io.github.dexclub.core.api.resource.ResourceTypedValue
import io.github.dexclub.core.api.resource.ResourceValueVariant
import io.github.dexclub.core.app.contract.ResourceValue

data class DecodedXmlProjection(
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val text: String,
)

data class ResourceValueProjection(
    val resourceId: String? = null,
    val packageName: String? = null,
    val type: String,
    val name: String,
    val variants: List<ResourceValueVariantProjection> = emptyList(),
)

data class ResourceValueVariantProjection(
    val configuration: ResourceConfigurationProjection,
    val value: ResourceTypedValueProjection? = null,
    val bag: ResourceBagProjection? = null,
)

data class ResourceConfigurationProjection(val qualifiers: String, val isDefault: Boolean)

data class ResourceTypedValueProjection(
    val valueType: String,
    val rawData: Int,
    val rawDataHex: String,
    val decodedValue: String? = null,
    val referencedResourceId: String? = null,
)

data class ResourceBagProjection(
    val kind: String,
    val parentResourceId: String? = null,
    val parentResourceName: String? = null,
    val items: List<ResourceBagItemProjection> = emptyList(),
)

data class ResourceBagItemProjection(
    val rawKey: String,
    val keyResourceId: String? = null,
    val keyName: String? = null,
    val index: Int? = null,
    val quantity: String? = null,
    val attributeType: String? = null,
    val attributeFormats: List<String>? = null,
    val value: ResourceTypedValueProjection,
)

data class ResourceEntryValueHitProjection(
    val resourceId: String? = null,
    val packageName: String? = null,
    val type: String? = null,
    val name: String? = null,
    val value: String? = null,
    val qualifier: String? = null,
    val valueKind: String? = null,
    val matchTarget: String? = null,
    val bagIndex: Int? = null,
    val bagKey: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
)

fun DecodedXmlResult.toProjection(): DecodedXmlProjection =
    DecodedXmlProjection(
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
        text = text,
    )

fun ResourceValue.toProjection(): ResourceValueProjection =
    ResourceValueProjection(
        resourceId = resourceId,
        packageName = packageName,
        type = type,
        name = name,
        variants = variants.map(ResourceValueVariant::toProjection),
    )

fun ResourceValueVariant.toProjection(): ResourceValueVariantProjection =
    ResourceValueVariantProjection(configuration.toProjection(), value?.toProjection(), bag?.toProjection())

fun ResourceConfiguration.toProjection(): ResourceConfigurationProjection =
    ResourceConfigurationProjection(qualifiers, isDefault)

fun ResourceTypedValue.toProjection(): ResourceTypedValueProjection =
    ResourceTypedValueProjection(valueType, rawData, rawDataHex, decodedValue, referencedResourceId)

fun ResourceBag.toProjection(): ResourceBagProjection =
    ResourceBagProjection(kind.name, parentResourceId, parentResourceName, items.map(ResourceBagItem::toProjection))

fun ResourceBagItem.toProjection(): ResourceBagItemProjection =
    ResourceBagItemProjection(
        rawKey, keyResourceId, keyName, index, quantity, attributeType, attributeFormats, value.toProjection(),
    )

fun ResourceEntryValueHit.toProjection(): ResourceEntryValueHitProjection =
    ResourceEntryValueHitProjection(
        resourceId = resourceId,
        packageName = packageName,
        type = type,
        name = name,
        value = value,
        qualifier = qualifier,
        valueKind = valueKind,
        matchTarget = matchTarget,
        bagIndex = bagIndex,
        bagKey = bagKey,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )
