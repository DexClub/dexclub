package io.github.dexclub.core.impl.dex

import io.github.dexclub.core.api.dex.DexInspectError
import io.github.dexclub.core.api.dex.DexInspectErrorReason
import io.github.dexclub.dexkit.query.ClassMatcher
import io.github.dexclub.dexkit.query.FindMethod
import io.github.dexclub.dexkit.query.MatchType
import io.github.dexclub.dexkit.query.MethodMatcher
import io.github.dexclub.dexkit.query.MethodsMatcher
import io.github.dexclub.dexkit.query.ParameterMatcher
import io.github.dexclub.dexkit.query.ParametersMatcher
import io.github.dexclub.dexkit.query.StringMatchType
import io.github.dexclub.dexkit.query.StringMatcher
import io.github.dexclub.dexkit.result.MethodData

internal fun buildExactCallersQuery(method: MethodData): FindMethod =
    FindMethod(
        matcher = MethodMatcher(
            invokeMethods = MethodsMatcher(
                methods = mutableListOf(method.toExactMethodMatcher()),
                matchType = MatchType.Contains,
            ),
        ),
    )

internal fun MethodData.toExactMethodMatcher(): MethodMatcher =
    MethodMatcher(
        name = StringMatcher(value = name, matchType = StringMatchType.Equals),
        declaredClass = ClassMatcher(
            className = StringMatcher(value = className, matchType = StringMatchType.Equals),
        ),
        returnType = ClassMatcher(
            className = StringMatcher(value = returnTypeName, matchType = StringMatchType.Equals),
        ),
        params = ParametersMatcher(
            params = paramTypeNames.map { typeName ->
                ParameterMatcher(
                    type = ClassMatcher(
                        className = StringMatcher(value = typeName, matchType = StringMatchType.Equals),
                    ),
                )
            }.toMutableList(),
        ),
    )

internal fun validateMethodDescriptor(descriptor: String) {
    val arrowIndex = descriptor.indexOf("->")
    val descriptorStart = descriptor.indexOf('(', startIndex = arrowIndex + 2)
    val descriptorEnd = descriptor.lastIndexOf(')')
    if (
        descriptor.isBlank() ||
        arrowIndex <= 0 ||
        descriptorStart <= arrowIndex + 2 ||
        descriptorEnd < descriptorStart ||
        descriptorEnd == descriptor.lastIndex
    ) {
        throw DexInspectError(
            reason = DexInspectErrorReason.InvalidMethodDescriptor,
            message = "invalid method descriptor: $descriptor",
        )
    }
}
