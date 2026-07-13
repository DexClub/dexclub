package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.dex.FindClassesUsingStringsRequest
import io.github.dexclub.core.api.dex.FindMethodsRequest
import io.github.dexclub.core.api.dex.FindMethodsUsingStringsRequest
import io.github.dexclub.core.api.shared.PageWindow
import io.github.dexclub.dexkit.query.BatchFindClassUsingStrings
import io.github.dexclub.dexkit.query.BatchFindMethodUsingStrings
import io.github.dexclub.dexkit.query.ClassMatcher
import io.github.dexclub.dexkit.query.FindMethod
import io.github.dexclub.dexkit.query.MethodMatcher
import io.github.dexclub.dexkit.query.StringMatchType
import io.github.dexclub.dexkit.query.StringMatcher
import kotlinx.serialization.json.Json

internal fun buildFindMethodsRequest(
    classNameContains: String? = null,
    methodNameContains: String? = null,
): FindMethodsRequest {
    val normalizedClassName = classNameContains?.trim()?.ifEmpty { null }
    val normalizedMethodName = methodNameContains?.trim()?.ifEmpty { null }
    require(normalizedClassName != null || normalizedMethodName != null) {
        "At least one of class_name_contains or method_name_contains is required; descriptor_contains is only applied as a secondary filter"
    }

    val matcher = MethodMatcher().apply {
        if (normalizedMethodName != null) {
            name = containsMatcher(normalizedMethodName)
        }
        if (normalizedClassName != null) {
            declaredClass = ClassMatcher(
                className = containsMatcher(normalizedClassName),
            )
        }
    }

    return FindMethodsRequest(
        queryText = Json.encodeToString(
            FindMethod.serializer(),
            FindMethod(matcher = matcher),
        ),
        window = PageWindow(),
    )
}

internal fun buildFindMethodsUsingStringsRequest(
    strings: List<String>,
    requireAll: Boolean,
): FindMethodsUsingStringsRequest =
    FindMethodsUsingStringsRequest(
        queryText = Json.encodeToString(
            BatchFindMethodUsingStrings.serializer(),
            buildStringMatcherQuery(BatchFindMethodUsingStrings(), strings, requireAll),
        ),
        window = PageWindow(),
    )

internal fun buildFindClassesUsingStringsRequest(
    strings: List<String>,
    requireAll: Boolean,
): FindClassesUsingStringsRequest =
    FindClassesUsingStringsRequest(
        queryText = Json.encodeToString(
            BatchFindClassUsingStrings.serializer(),
            buildStringMatcherQuery(BatchFindClassUsingStrings(), strings, requireAll),
        ),
        window = PageWindow(),
    )

private fun buildStringMatcherQuery(
    query: BatchFindMethodUsingStrings,
    strings: List<String>,
    requireAll: Boolean,
): BatchFindMethodUsingStrings = query.apply {
    populateStringMatcherGroups(groups, strings, requireAll)
}

private fun buildStringMatcherQuery(
    query: BatchFindClassUsingStrings,
    strings: List<String>,
    requireAll: Boolean,
): BatchFindClassUsingStrings = query.apply {
    populateStringMatcherGroups(groups, strings, requireAll)
}

private fun populateStringMatcherGroups(
    groups: MutableMap<String, List<StringMatcher>>,
    strings: List<String>,
    requireAll: Boolean,
) {
    val normalized = strings.filter { it.isNotBlank() }
    if (requireAll) {
        if (normalized.isNotEmpty()) {
            groups["all"] = normalized.map(::containsMatcher)
        }
    } else {
        normalized.forEachIndexed { index, value ->
            groups["any-$index"] = listOf(containsMatcher(value))
        }
    }

    if (groups.isEmpty()) {
        throw IllegalArgumentException("At least one non-blank string filter is required")
    }
}

private fun containsMatcher(value: String): StringMatcher =
    StringMatcher(value = value, matchType = StringMatchType.Contains)
