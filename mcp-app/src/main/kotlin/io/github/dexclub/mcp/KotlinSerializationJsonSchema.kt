package io.github.dexclub.mcp

import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal data class JsonSchemaBundle(
    val root: JsonObject,
    val defs: JsonObject,
)

internal fun jsonSchemaFor(descriptor: SerialDescriptor): JsonSchemaBundle =
    KotlinSerializationJsonSchema().build(descriptor)

private class KotlinSerializationJsonSchema {
    private val definitions = linkedMapOf<String, JsonObject>()
    private val definitionNames = mutableMapOf<String, String>()

    fun build(descriptor: SerialDescriptor): JsonSchemaBundle {
        val root = schemaFor(descriptor)
        return JsonSchemaBundle(
            root = root,
            defs = JsonObject(definitions),
        )
    }

    private fun schemaFor(descriptor: SerialDescriptor): JsonObject {
        val schema = when (descriptor.kind) {
            PrimitiveKind.BOOLEAN -> typedSchema("boolean")
            PrimitiveKind.BYTE,
            PrimitiveKind.SHORT,
            PrimitiveKind.INT,
            PrimitiveKind.LONG,
            -> typedSchema("integer")
            PrimitiveKind.FLOAT,
            PrimitiveKind.DOUBLE,
            -> typedSchema("number")
            PrimitiveKind.CHAR,
            PrimitiveKind.STRING,
            -> typedSchema("string")
            SerialKind.ENUM -> buildJsonObject {
                put("type", "string")
                put("enum", JsonArray((0 until descriptor.elementsCount).map { JsonPrimitive(descriptor.getElementName(it)) }))
            }
            StructureKind.LIST -> buildJsonObject {
                put("type", "array")
                put("items", schemaFor(descriptor.getElementDescriptor(0)))
            }
            StructureKind.MAP -> buildJsonObject {
                put("type", "object")
                put("additionalProperties", schemaFor(descriptor.getElementDescriptor(1)))
            }
            StructureKind.CLASS,
            StructureKind.OBJECT,
            -> referenceFor(descriptor)
            else -> error("Unsupported serial descriptor kind ${descriptor.kind} for ${descriptor.serialName}")
        }
        if (!descriptor.isNullable) return schema
        return buildJsonObject {
            put("anyOf", JsonArray(listOf(schema, typedSchema("null"))))
        }
    }

    private fun referenceFor(descriptor: SerialDescriptor): JsonObject {
        val definitionName = definitionNames.getOrPut(descriptor.serialName) {
            uniqueDefinitionName(descriptor.serialName.substringAfterLast('.'))
        }
        if (definitionName !in definitions) {
            definitions[definitionName] = JsonObject(emptyMap())
            definitions[definitionName] = buildJsonObject {
                put("type", "object")
                put("additionalProperties", false)
                put("properties", buildJsonObject {
                    repeat(descriptor.elementsCount) { index ->
                        put(descriptor.getElementName(index), schemaFor(descriptor.getElementDescriptor(index)))
                    }
                })
                val required = (0 until descriptor.elementsCount)
                    .filterNot(descriptor::isElementOptional)
                    .map(descriptor::getElementName)
                if (required.isNotEmpty()) {
                    put("required", JsonArray(required.map(::JsonPrimitive)))
                }
            }
        }
        return buildJsonObject { put("\$ref", "#/\$defs/$definitionName") }
    }

    private fun uniqueDefinitionName(base: String): String {
        var candidate = base
        var suffix = 2
        while (candidate in definitions || candidate in definitionNames.values) {
            candidate = "$base$suffix"
            suffix += 1
        }
        return candidate
    }

    private fun typedSchema(type: String): JsonObject = buildJsonObject { put("type", type) }
}
