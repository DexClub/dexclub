package io.github.dexclub.dexkit.result

import io.github.dexclub.dexkit.DexKitBridge
import io.github.dexclub.dexkit.query.FindClass
import io.github.dexclub.dexkit.query.FindField
import io.github.dexclub.dexkit.query.FindMethod

/**
 * A chained [ClassData] result set that implements [List]<[ClassData]> and keeps a bridge reference,
 * allowing follow-up find* queries within the current result scope.
 */
class ClassDataList internal constructor(
    internal val bridge: DexKitBridge,
    data: List<ClassData>,
) : List<ClassData> by data {

    fun findClass(query: FindClass): ClassDataList =
        bridge.findClass(query.copy(searchInClasses = this))

    fun findClass(init: FindClass.() -> Unit): ClassDataList =
        findClass(FindClass().apply(init))

    fun findMethod(query: FindMethod): MethodDataList =
        bridge.findMethod(query.copy(searchInClasses = this))

    fun findMethod(init: FindMethod.() -> Unit): MethodDataList =
        findMethod(FindMethod().apply(init))

    fun findField(query: FindField): FieldDataList =
        bridge.findField(query.copy(searchInClasses = this))

    fun findField(init: FindField.() -> Unit): FieldDataList =
        findField(FindField().apply(init))
}

fun List<ClassData>.toClassDataList(bridge: DexKitBridge): ClassDataList =
    ClassDataList(bridge, this)
