package io.github.dexclub.core.impl.resource

import com.reandroid.arsc.model.ResourceEntry as ArscResourceEntry
import com.reandroid.arsc.value.Entry
import com.reandroid.arsc.value.AttributeDataFormat
import com.reandroid.arsc.value.ValueItem
import com.reandroid.arsc.value.ValueType
import com.reandroid.arsc.value.array.ArrayBag
import com.reandroid.arsc.value.attribute.AttributeBag
import com.reandroid.arsc.value.plurals.PluralsBag
import com.reandroid.arsc.value.style.StyleBag
import io.github.dexclub.core.api.resource.ResourceBag
import io.github.dexclub.core.api.resource.ResourceBagItem
import io.github.dexclub.core.api.resource.ResourceBagKind
import io.github.dexclub.core.api.resource.ResourceConfiguration
import io.github.dexclub.core.api.resource.ResourceTypedValue
import io.github.dexclub.core.api.resource.ResourceValue
import io.github.dexclub.core.api.resource.ResourceValueVariant

internal fun ArscResourceEntry.toResourceValue(): ResourceValue {
    val variants = iterator().asSequence()
        .map(Entry::toResourceValueVariant)
        .sortedWith(compareBy<ResourceValueVariant>({ !it.configuration.isDefault }, { it.configuration.qualifiers }))
        .toList()
    return ResourceValue(
        resourceId = hexId,
        packageName = packageName,
        type = type,
        name = name,
        variants = variants,
    )
}

internal fun Entry.toDisplayValue(): String? = getResValue()?.toTypedValue()?.decodedValue

private fun Entry.toResourceValueVariant(): ResourceValueVariant =
    ResourceValueVariant(
        configuration = ResourceConfiguration(
            qualifiers = resConfig.qualifiers,
            isDefault = resConfig.isDefault,
        ),
        value = getResValue()?.toTypedValue(),
        bag = if (isComplex) toResourceBag() else null,
    )

private fun Entry.toResourceBag(): ResourceBag {
    if (typeName == "attr" || AttributeBag.isAttribute(resTableMapEntry)) {
        val bag = AttributeBag.create(this)
        return ResourceBag(
            kind = ResourceBagKind.Attribute,
            items = bag?.bagItems.orEmpty().map { item ->
                val raw = item.bagItem
                ResourceBagItem(
                    rawKey = raw.nameId.toHexId(),
                    keyResourceId = raw.nameId.toOptionalResourceId(),
                    keyName = item.nameOrHex,
                    attributeType = item.type?.name,
                    attributeFormats = if (item.isFormats) {
                        AttributeDataFormat.entries.filter { it.matches(raw.data) }.map { it.name }
                    } else {
                        item.dataFormats?.map { it.name }
                    },
                    value = raw.toTypedValue(),
                )
            },
        )
    }
    if (typeName == "plurals" || PluralsBag.isPlurals(this)) {
        val bag = PluralsBag.create(this)
        return ResourceBag(
            kind = ResourceBagKind.Plurals,
            items = bag?.entries.orEmpty().map { (quantity, item) ->
                val raw = item.bagItem
                ResourceBagItem(
                    rawKey = raw.nameId.toHexId(),
                    keyResourceId = raw.nameId.toOptionalResourceId(),
                    quantity = quantity?.getName(),
                    value = raw.toTypedValue(),
                )
            },
        )
    }
    if (typeName == "style" || StyleBag.isStyle(this)) {
        val bag = StyleBag.create(this)
        return ResourceBag(
            kind = ResourceBagKind.Style,
            parentResourceId = bag?.parentId?.toOptionalResourceId(),
            parentResourceName = bag?.parentResourceName,
            items = bag?.entries.orEmpty().map { (key, item) ->
                ResourceBagItem(
                    rawKey = key.toHexId(),
                    keyResourceId = key.toOptionalResourceId(),
                    keyName = item.name,
                    value = item.bagItem.toTypedValue(),
                )
            },
        )
    }
    if (typeName == "array" || typeName.endsWith("-array") || ArrayBag.isArray(this)) {
        val bag = ArrayBag.create(this)
        return ResourceBag(
            kind = ResourceBagKind.Array,
            items = bag.orEmpty().mapIndexed { index, item ->
                val raw = item.bagItem
                ResourceBagItem(
                    rawKey = raw.nameId.toHexId(),
                    keyResourceId = raw.nameId.toOptionalResourceId(),
                    index = index,
                    value = raw.toTypedValue(),
                )
            },
        )
    }
    return ResourceBag(
        kind = ResourceBagKind.Unknown,
        parentResourceId = resTableMapEntry?.parentId?.toOptionalResourceId(),
        items = resTableMapEntry?.listResValueMap().orEmpty().map { item ->
            ResourceBagItem(
                rawKey = item.nameId.toHexId(),
                keyResourceId = item.nameId.toOptionalResourceId(),
                value = item.toTypedValue(),
            )
        },
    )
}

private fun ValueItem.toTypedValue(): ResourceTypedValue {
    val type = valueType
    return ResourceTypedValue(
        valueType = type?.name ?: "UNKNOWN",
        rawData = data,
        rawDataHex = data.toHexId(),
        decodedValue = if (type == null || type == ValueType.NULL) null else runCatching(::decodeValue).getOrNull(),
        referencedResourceId = if (type?.isReference == true) data.toOptionalResourceId() else null,
    )
}

private fun Int.toHexId(): String = "0x" + toUInt().toString(16).padStart(8, '0')

private fun Int.toOptionalResourceId(): String? = takeIf { it != 0 }?.toHexId()
