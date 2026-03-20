package io.github.dexclub.core.search

import io.github.dexclub.core.workspace.ClassVisualKind
import io.github.dexclub.core.workspace.WorkspaceIndexService
import io.github.dexclub.dexkit.result.ClassData
import io.github.dexclub.dexkit.result.MethodData
import io.github.dexclub.utils.SignatureUtils

internal suspend fun WorkspaceIndexService.loadSearchClassInfos(
    names: List<String>,
): Map<String, SearchClassInfo> {
    val normalizedNames = names
        .map(::normalizeSearchTargetClassName)
        .filter { it.isNotEmpty() }
        .distinct()

    if (normalizedNames.isEmpty()) {
        return emptyMap()
    }

    return normalizedNames
        .chunked(CLASS_QUERY_BATCH_SIZE)
        .flatMap { chunk -> findByNames(chunk) }
        .associate { record ->
            record.className to SearchClassInfo(
                className = record.className,
                classVisualKind = record.classVisualKind,
            )
        }
}

internal fun normalizeSearchTargetClassName(value: String): String {
    val raw = value.trim()
    if (raw.isEmpty()) return ""

    val descriptor = CLASS_SIGNATURE_REGEX.find(raw)?.value
    if (!descriptor.isNullOrEmpty()) {
        return SignatureUtils.typeName(descriptor)
    }

    val beforeMember = raw.substringBefore("->").trim()
    if (beforeMember.startsWith('L') && beforeMember.endsWith(';')) {
        return SignatureUtils.typeName(beforeMember)
    }

    return beforeMember
        .replace('/', '.')
        .removePrefix("L")
        .removeSuffix(";")
        .trim()
}

internal fun normalizeDexKitClassName(
    classData: ClassData,
): String {
    return normalizeSearchTargetClassName(
        value = classData.name.ifBlank { classData.descriptor },
    )
}

internal fun normalizeDexKitClassName(
    methodData: MethodData,
): String {
    return normalizeSearchTargetClassName(
        value = methodData.className.ifBlank { methodData.descriptor.substringBefore("->") },
    )
}

internal fun classSearchRank(
    keyword: String,
    className: String,
): Int {
    val normalizedKeyword = normalizeSearchTargetClassName(keyword).lowercase()
    if (normalizedKeyword.isEmpty()) {
        return 6
    }

    val normalizedClassName = className.lowercase()
    val simpleClassName = className
        .substringAfterLast('.')
        .substringAfterLast('$')
        .lowercase()

    return when {
        normalizedClassName == normalizedKeyword -> 0
        simpleClassName == normalizedKeyword -> 1
        normalizedClassName.startsWith(normalizedKeyword) -> 2
        simpleClassName.startsWith(normalizedKeyword) -> 3
        normalizedClassName.contains(normalizedKeyword) -> 4
        simpleClassName.contains(normalizedKeyword) -> 5
        else -> 6
    }
}

internal fun buildMethodDisplaySignature(
    methodData: MethodData,
): String {
    val params = methodData.paramTypeNames.joinToString(", ") { param ->
        param.substringAfterLast('.')
    }
    val returnType = methodData.returnTypeName.substringAfterLast('.')
    return "${methodData.name}($params): $returnType"
}

internal data class SearchClassInfo(
    val className: String,
    val classVisualKind: ClassVisualKind,
)

private const val CLASS_QUERY_BATCH_SIZE = 900
private val CLASS_SIGNATURE_REGEX = Regex("L[\\w/$]+;")
