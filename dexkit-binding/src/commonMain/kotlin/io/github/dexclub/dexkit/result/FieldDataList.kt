package io.github.dexclub.dexkit.result

import io.github.dexclub.dexkit.DexKitBridge
import io.github.dexclub.dexkit.query.FindField

/**
 * A chained [FieldData] result set that implements [List]<[FieldData]> and keeps a bridge reference,
 * allowing follow-up findField queries within the current result scope.
 */
class FieldDataList internal constructor(
    internal val bridge: DexKitBridge,
    data: List<FieldData>,
) : List<FieldData> by data {

    fun findField(query: FindField): FieldDataList =
        bridge.findField(query.copy(searchInFields = this))

    fun findField(init: FindField.() -> Unit): FieldDataList =
        findField(FindField().apply(init))
}

fun List<FieldData>.toFieldDataList(bridge: DexKitBridge): FieldDataList =
    FieldDataList(bridge, this)
