package io.github.dexclub.core.impl.resource

import com.reandroid.arsc.value.plurals.PluralsBag
import com.reandroid.arsc.value.ValueType
import com.reandroid.arsc.value.Entry
import io.github.dexclub.core.api.resource.ResourcePluralItem

internal data class DecodedResourceValue(
    val value: String? = null,
    val pluralItems: List<ResourcePluralItem>? = null,
)

internal fun Entry.toDisplayValue(): String? {
    val resValue = getResValue() ?: return null
    return when (resValue.getValueType()) {
        null -> null
        ValueType.NULL -> null
        else -> resValue.decodeValue()
    }
}

internal fun Entry.toDecodedResourceValue(typeName: String? = null): DecodedResourceValue {
    val pluralItems = toPluralItems(typeName)
    if (pluralItems != null) {
        return DecodedResourceValue(pluralItems = pluralItems)
    }
    return DecodedResourceValue(value = toDisplayValue())
}

internal fun Entry.toPluralItems(typeName: String? = null): List<ResourcePluralItem>? {
    if (typeName != "plurals" && !PluralsBag.isPlurals(this)) {
        return null
    }
    val bag = PluralsBag.create(this) ?: return emptyList()
    return bag.entries
        .mapNotNull { (quantity, item) ->
            val quantityName = quantity?.getName() ?: return@mapNotNull null
            val value = runCatching { item.getQualityString(null) }.getOrNull() ?: return@mapNotNull null
            ResourcePluralItem(
                quantity = quantityName,
                value = value,
            )
        }
        .ifEmpty { emptyList() }
}
