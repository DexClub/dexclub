package io.github.dexclub.core.impl.dex

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.FieldHit
import io.github.dexclub.core.api.dex.FieldUsageType
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.dexkit.DexKitBridge
import io.github.dexclub.dexkit.result.ClassData
import io.github.dexclub.dexkit.result.FieldData
import io.github.dexclub.dexkit.result.MethodData
import java.nio.file.Path

internal data class TargetDexSource(
    val dexPath: Path,
    val memberSource: MemberSource,
)

internal data class MemberSource(
    val sourcePath: String,
    val sourceEntry: String?,
)

internal data class TargetDexScope(
    val workdir: String,
    val activeTargetId: String,
)

internal data class TargetDexCacheKey(
    val contentFingerprint: String,
)

internal data class TargetDexContext(
    val bridge: DexKitBridge,
    val sources: List<TargetDexSource>,
    val sourcesByDexId: Map<Int, MemberSource>,
) {
    fun resolveSource(dexId: Int): MemberSource? = sourcesByDexId[dexId]

    fun requireSource(dexId: Int): MemberSource =
        checkNotNull(resolveSource(dexId)) { "missing source mapping for dexId=$dexId" }
}

internal data class LocatedMethod(
    val method: MethodData,
    val source: MemberSource,
) {
    fun toMethodHit(): MethodHit = method.toMethodHit(source)
}

internal fun MethodData.toMethodHit(source: MemberSource?): MethodHit =
    MethodHit(
        className = className,
        methodName = name,
        descriptor = descriptor,
        sourcePath = source?.sourcePath,
        sourceEntry = source?.sourceEntry,
    )

internal fun FieldData.toFieldHit(source: MemberSource?): FieldHit =
    FieldHit(
        className = className,
        fieldName = name,
        descriptor = descriptor,
        sourcePath = source?.sourcePath,
        sourceEntry = source?.sourceEntry,
    )

internal fun TargetDexContext.toClassHit(classData: ClassData): ClassHit {
    val source = resolveSource(classData.dexId)
    return ClassHit(
        className = classData.descriptor,
        sourcePath = source?.sourcePath,
        sourceEntry = source?.sourceEntry,
    )
}

internal fun MethodData.toResolvedMethodHit(
    context: TargetDexContext,
    classSourceCache: MutableMap<String, MemberSource?>,
    resolveClassSource: (TargetDexContext, String, MutableMap<String, MemberSource?>) -> MemberSource?,
): MethodHit {
    val source = context.resolveSource(dexId) ?: resolveClassSource(context, className, classSourceCache)
    return toMethodHit(source)
}

internal fun FieldData.toResolvedFieldHit(
    context: TargetDexContext,
    classSourceCache: MutableMap<String, MemberSource?>,
    resolveClassSource: (TargetDexContext, String, MutableMap<String, MemberSource?>) -> MemberSource?,
): FieldHit {
    val source = context.resolveSource(dexId) ?: resolveClassSource(context, className, classSourceCache)
    return toFieldHit(source)
}

internal fun resolveClassSource(
    context: TargetDexContext,
    className: String,
    classSourceCache: MutableMap<String, MemberSource?>,
): MemberSource? =
    classSourceCache.getOrPut(className) {
        context.bridge.getClassData(toTypeSignature(className))
            ?.dexId
            ?.let(context::resolveSource)
    }

internal fun io.github.dexclub.dexkit.result.FieldUsingType.toFieldUsageType(): FieldUsageType =
    when (this) {
        io.github.dexclub.dexkit.result.FieldUsingType.Read -> FieldUsageType.Read
        io.github.dexclub.dexkit.result.FieldUsingType.Write -> FieldUsageType.Write
    }
