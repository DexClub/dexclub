package io.github.dexclub.core.impl.resource

import io.github.dexclub.core.api.resource.ResourceDecodeError
import io.github.dexclub.core.api.resource.ResourceDecodeErrorReason
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class ResourceSearchQueryParser {
    private val json = Json

    fun parse(queryText: String): ResourceSearchQuery {
        val root = try {
            json.parseToJsonElement(queryText).jsonObject
        } catch (error: Throwable) {
            throw ResourceDecodeError(
                reason = ResourceDecodeErrorReason.ResourceQueryInvalid,
                message = "Resource query JSON is not valid",
                cause = error,
            )
        }
        val type = root.requiredString("resourceType")
        val value = root.requiredString("value")
        return ResourceSearchQuery(
            resourceType = type,
            value = value,
            contains = root.optionalBoolean("contains") ?: false,
            ignoreCase = root.optionalBoolean("ignoreCase") ?: false,
            packageName = root.optionalString("packageName"),
            qualifier = root.optionalString("qualifier"),
            valueKind = root.optionalString("valueKind"),
            matchTarget = root.optionalString("matchTarget")?.toMatchTarget() ?: ResourceValueMatchTarget.DecodedValue,
        )
    }

    private fun JsonObject.requiredString(key: String): String =
        this[key]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.takeIf { it.isNotEmpty() }
            ?: throw ResourceDecodeError(
                reason = ResourceDecodeErrorReason.ResourceQueryInvalid,
                message = "Resource query field '$key' must be a non-empty string",
            )

    private fun JsonObject.optionalBoolean(key: String): Boolean? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        return primitive.booleanOrNull ?: throw ResourceDecodeError(
            reason = ResourceDecodeErrorReason.ResourceQueryInvalid,
            message = "Resource query field '$key' must be a boolean",
        )
    }

    private fun JsonObject.optionalString(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() }

    private fun String.toMatchTarget(): ResourceValueMatchTarget =
        when (this) {
            "decoded_value" -> ResourceValueMatchTarget.DecodedValue
            "raw_data" -> ResourceValueMatchTarget.RawData
            "reference" -> ResourceValueMatchTarget.Reference
            "bag_key" -> ResourceValueMatchTarget.BagKey
            "any" -> ResourceValueMatchTarget.Any
            else -> throw ResourceDecodeError(
                reason = ResourceDecodeErrorReason.ResourceQueryInvalid,
                message = "Unsupported resource query matchTarget: $this",
            )
        }
}
