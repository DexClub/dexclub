package io.github.dexclub.core.api.dex

import io.github.dexclub.dexkit.query.ClassMatcher
import io.github.dexclub.dexkit.query.FieldMatcher
import io.github.dexclub.dexkit.query.MethodMatcher
import kotlinx.serialization.Serializable

@Serializable
data class FindClassQuery(
    val searchPackages: List<String> = emptyList(),
    val excludePackages: List<String> = emptyList(),
    val ignorePackagesCase: Boolean = false,
    val matcher: ClassMatcher? = null,
    val findFirst: Boolean = false,
)

@Serializable
data class FindMethodQuery(
    val searchPackages: List<String> = emptyList(),
    val excludePackages: List<String> = emptyList(),
    val ignorePackagesCase: Boolean = false,
    val matcher: MethodMatcher? = null,
    val findFirst: Boolean = false,
)

@Serializable
data class FindFieldQuery(
    val searchPackages: List<String> = emptyList(),
    val excludePackages: List<String> = emptyList(),
    val ignorePackagesCase: Boolean = false,
    val matcher: FieldMatcher? = null,
    val findFirst: Boolean = false,
)
