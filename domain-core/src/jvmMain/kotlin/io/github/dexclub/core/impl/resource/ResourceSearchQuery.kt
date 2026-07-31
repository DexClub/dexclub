package io.github.dexclub.core.impl.resource

internal data class ResourceSearchQuery(
    val resourceType: String,
    val value: String,
    val contains: Boolean = false,
    val ignoreCase: Boolean = false,
    val packageName: String? = null,
    val qualifier: String? = null,
    val valueKind: String? = null,
    val matchTarget: ResourceValueMatchTarget = ResourceValueMatchTarget.DecodedValue,
)

internal enum class ResourceValueMatchTarget {
    DecodedValue,
    RawData,
    Reference,
    BagKey,
    Any,
}
