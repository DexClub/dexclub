package io.github.dexclub.core.app.resource

import io.github.dexclub.core.api.resource.FindResourcesRequest
import io.github.dexclub.core.api.shared.PageWindow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun buildFindResourcesRequest(
    type: String,
    value: String,
    contains: Boolean,
    ignoreCase: Boolean,
    packageName: String? = null,
    qualifier: String? = null,
    valueKind: String? = null,
    matchTarget: String? = null,
): FindResourcesRequest =
    FindResourcesRequest(
        queryText = buildJsonObject {
            put("resourceType", type)
            put("value", value)
            put("contains", contains)
            put("ignoreCase", ignoreCase)
            packageName?.let { put("packageName", it) }
            qualifier?.let { put("qualifier", it) }
            valueKind?.let { put("valueKind", it) }
            matchTarget?.let { put("matchTarget", it) }
        }.toString(),
        window = PageWindow(),
    )
