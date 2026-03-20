package io.github.dexclub.core.workspace

import io.github.dexclub.core.DexFactory

data class WorkspaceIndexedClass(
    val dexAbsolutePath: String,
    val signature: String,
    val modifiers: Int,
)

interface WorkspaceClassSource {
    val classCount: Int

    fun classes(): Sequence<WorkspaceIndexedClass>
}

class DexFactoryWorkspaceClassSource(
    private val dexFactory: DexFactory,
) : WorkspaceClassSource {
    override val classCount: Int
        get() = dexFactory.dexs.values.sumOf { dex -> dex.classes.size }

    override fun classes(): Sequence<WorkspaceIndexedClass> {
        return dexFactory.dexs.asSequence().flatMap { (path, dex) ->
            dex.classes.asSequence().map { classDef ->
                WorkspaceIndexedClass(
                    dexAbsolutePath = path,
                    signature = classDef.type,
                    modifiers = classDef.accessFlags,
                )
            }
        }
    }
}

fun DexFactory.asWorkspaceClassSource(): WorkspaceClassSource {
    return DexFactoryWorkspaceClassSource(this)
}
