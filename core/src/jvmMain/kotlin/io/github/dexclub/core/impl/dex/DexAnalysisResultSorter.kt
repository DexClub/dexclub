package io.github.dexclub.core.impl.dex

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.FieldHit
import io.github.dexclub.core.api.dex.FieldUsageType
import io.github.dexclub.core.api.dex.MethodDetail
import io.github.dexclub.core.api.dex.MethodFieldUsage
import io.github.dexclub.core.api.dex.MethodHit

internal object DexAnalysisResultSorter {
    fun sortClassHits(hits: List<ClassHit>): List<ClassHit> =
        hits.sortedWith(
            compareBy<ClassHit>({ it.className }, { it.sourcePath.orEmpty() }, { it.sourceEntry.orEmpty() }),
        )

    fun sortMethodHits(hits: List<MethodHit>): List<MethodHit> =
        hits.sortedWith(
            compareBy<MethodHit>(
                { it.className },
                { it.methodName },
                { it.descriptor },
                { it.sourcePath.orEmpty() },
                { it.sourceEntry.orEmpty() },
            ),
        )

    fun sortFieldHits(hits: List<FieldHit>): List<FieldHit> =
        hits.sortedWith(
            compareBy<FieldHit>(
                { it.className },
                { it.fieldName },
                { it.descriptor },
                { it.sourcePath.orEmpty() },
                { it.sourceEntry.orEmpty() },
            ),
        )

    fun sortMethodDetail(detail: MethodDetail): MethodDetail =
        MethodDetail(
            method = detail.method,
            usingFields = detail.usingFields?.sortedWith(
                compareBy<MethodFieldUsage>(
                    { it.usingType != FieldUsageType.Read },
                    { it.field.className },
                    { it.field.fieldName },
                    { it.field.descriptor },
                    { it.field.sourcePath.orEmpty() },
                    { it.field.sourceEntry.orEmpty() },
                ),
            ),
            callers = detail.callers?.let(::sortMethodHits),
            invokes = detail.invokes?.let(::sortMethodHits),
            strings = detail.strings?.distinct()?.sorted(),
            annotations = detail.annotations?.distinct()?.sorted(),
        )
}
