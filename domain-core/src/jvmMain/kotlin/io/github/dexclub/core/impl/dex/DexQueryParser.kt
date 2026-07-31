package io.github.dexclub.core.impl.dex

import io.github.dexclub.core.api.dex.DexQueryError
import io.github.dexclub.core.api.dex.DexQueryErrorReason
import io.github.dexclub.core.api.dex.FindClassQuery
import io.github.dexclub.core.api.dex.FindFieldQuery
import io.github.dexclub.core.api.dex.FindMethodQuery
import io.github.dexclub.core.impl.shared.workspaceJson
import io.github.dexclub.dexkit.query.FindClass
import io.github.dexclub.dexkit.query.FindField
import io.github.dexclub.dexkit.query.FindMethod
import kotlinx.serialization.SerializationException

internal class DexQueryParser {
    fun parseFindClass(queryText: String): FindClass {
        val normalized = queryText.trim()
        if (normalized.isEmpty()) {
            throw DexQueryError(
                reason = DexQueryErrorReason.InvalidQuery,
                message = "Query JSON must not be empty",
            )
        }
        return try {
            workspaceJson.decodeFromString<FindClassQuery>(normalized).let { query ->
                FindClass(
                    searchPackages = query.searchPackages,
                    excludePackages = query.excludePackages,
                    ignorePackagesCase = query.ignorePackagesCase,
                    matcher = query.matcher,
                    findFirst = query.findFirst,
                )
            }
        } catch (cause: SerializationException) {
            throw DexQueryError(
                reason = DexQueryErrorReason.InvalidQuery,
                message = "Invalid find-class query JSON",
                cause = cause,
            )
        } catch (cause: IllegalArgumentException) {
            throw DexQueryError(
                reason = DexQueryErrorReason.InvalidQuery,
                message = "Invalid find-class query value",
                cause = cause,
            )
        }
    }

    fun parseFindMethod(queryText: String): FindMethod {
        val normalized = queryText.trim()
        if (normalized.isEmpty()) {
            throw DexQueryError(
                reason = DexQueryErrorReason.InvalidQuery,
                message = "Query JSON must not be empty",
            )
        }
        return try {
            workspaceJson.decodeFromString<FindMethodQuery>(normalized).let { query ->
                FindMethod(
                    searchPackages = query.searchPackages,
                    excludePackages = query.excludePackages,
                    ignorePackagesCase = query.ignorePackagesCase,
                    matcher = query.matcher,
                    findFirst = query.findFirst,
                )
            }
        } catch (cause: SerializationException) {
            throw DexQueryError(
                reason = DexQueryErrorReason.InvalidQuery,
                message = "Invalid find-method query JSON",
                cause = cause,
            )
        } catch (cause: IllegalArgumentException) {
            throw DexQueryError(
                reason = DexQueryErrorReason.InvalidQuery,
                message = "Invalid find-method query value",
                cause = cause,
            )
        }
    }

    fun parseFindField(queryText: String): FindField {
        val normalized = queryText.trim()
        if (normalized.isEmpty()) {
            throw DexQueryError(
                reason = DexQueryErrorReason.InvalidQuery,
                message = "Query JSON must not be empty",
            )
        }
        return try {
            workspaceJson.decodeFromString<FindFieldQuery>(normalized).let { query ->
                FindField(
                    searchPackages = query.searchPackages,
                    excludePackages = query.excludePackages,
                    ignorePackagesCase = query.ignorePackagesCase,
                    matcher = query.matcher,
                    findFirst = query.findFirst,
                )
            }
        } catch (cause: SerializationException) {
            throw DexQueryError(
                reason = DexQueryErrorReason.InvalidQuery,
                message = "Invalid find-field query JSON",
                cause = cause,
            )
        } catch (cause: IllegalArgumentException) {
            throw DexQueryError(
                reason = DexQueryErrorReason.InvalidQuery,
                message = "Invalid find-field query value",
                cause = cause,
            )
        }
    }
}
