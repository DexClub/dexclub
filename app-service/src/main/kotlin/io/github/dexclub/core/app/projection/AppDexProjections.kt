package io.github.dexclub.core.app.projection

import io.github.dexclub.core.app.contract.ClassHit
import io.github.dexclub.core.app.contract.FieldHit
import io.github.dexclub.core.app.contract.FieldUsageType
import io.github.dexclub.core.app.contract.MethodDetail
import io.github.dexclub.core.app.contract.MethodFieldUsage
import io.github.dexclub.core.app.contract.MethodHit

data class ClassHitProjection(
    val className: String,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
)

data class MethodHitProjection(
    val className: String,
    val methodName: String,
    val descriptor: String,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
)

data class FieldHitProjection(
    val className: String,
    val fieldName: String,
    val descriptor: String,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
)

data class MethodFieldUsageProjection(
    val usingType: FieldUsageType,
    val field: FieldHitProjection,
)

data class MethodDetailProjection(
    val method: MethodHitProjection,
    val usingFields: List<MethodFieldUsageProjection>? = null,
    val callers: List<MethodHitProjection>? = null,
    val invokes: List<MethodHitProjection>? = null,
    val strings: List<String>? = null,
    val annotations: List<String>? = null,
)

fun ClassHit.toProjection(): ClassHitProjection =
    ClassHitProjection(
        className = className,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )

fun MethodHit.toProjection(): MethodHitProjection =
    MethodHitProjection(
        className = className,
        methodName = methodName,
        descriptor = descriptor,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )

fun FieldHit.toProjection(): FieldHitProjection =
    FieldHitProjection(
        className = className,
        fieldName = fieldName,
        descriptor = descriptor,
        sourcePath = sourcePath,
        sourceEntry = sourceEntry,
    )

fun MethodFieldUsage.toProjection(): MethodFieldUsageProjection =
    MethodFieldUsageProjection(
        usingType = usingType,
        field = field.toProjection(),
    )

fun MethodDetail.toProjection(): MethodDetailProjection =
    MethodDetailProjection(
        method = method.toProjection(),
        usingFields = usingFields?.map(MethodFieldUsage::toProjection),
        callers = callers?.map(MethodHit::toProjection),
        invokes = invokes?.map(MethodHit::toProjection),
        strings = strings,
        annotations = annotations,
    )
