package io.github.dexclub.dexkit.result

import io.github.dexclub.dexkit.DexKitBridge
import io.github.dexclub.dexkit.query.FindMethod

/**
 * A chained [MethodData] result set that implements [List]<[MethodData]> and keeps a bridge reference,
 * allowing follow-up findMethod queries within the current result scope.
 */
class MethodDataList internal constructor(
    internal val bridge: DexKitBridge,
    data: List<MethodData>,
) : List<MethodData> by data {

    fun findMethod(query: FindMethod): MethodDataList =
        bridge.findMethod(query.copy(searchInMethods = this))

    fun findMethod(init: FindMethod.() -> Unit): MethodDataList =
        findMethod(FindMethod().apply(init))
}

fun List<MethodData>.toMethodDataList(bridge: DexKitBridge): MethodDataList =
    MethodDataList(bridge, this)
