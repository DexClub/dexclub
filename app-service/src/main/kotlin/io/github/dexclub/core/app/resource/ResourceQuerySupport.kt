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
): FindResourcesRequest =
    FindResourcesRequest(
        queryText = buildJsonObject {
            put("type", type)
            put("value", value)
            put("contains", contains)
            put("ignoreCase", ignoreCase)
        }.toString(),
        window = PageWindow(),
    )
