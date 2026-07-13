package io.github.dexclub.core.app.projection

import io.github.dexclub.core.app.contract.DecodedXmlResult
import io.github.dexclub.core.app.contract.ResourceEntryValueHit
import io.github.dexclub.core.app.contract.ResourceValue

data class DecodedXmlProjection(
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
    val text: String,
)

data class ResourceValueProjection(
    val resourceId: String? = null,
    val type: String,
    val name: String,
    val value: String? = null,
)

data class ResourceEntryValueHitProjection(
    val resourceId: String? = null,
    val type: String? = null,
    val name: String? = null,
    val value: String? = null,
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
        type = type,
        name = name,
        value = value,
    )

fun ResourceEntryValueHit.toProjection(): ResourceEntryValueHitProjection =
    ResourceEntryValueHitProjection(
        resourceId = resourceId,
        type = type,
        name = name,
        value = value,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )
