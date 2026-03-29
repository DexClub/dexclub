package io.github.dexclub.codeview.treesitter.semantic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
public data class SemanticNode(
    val schema: Int = SCHEMA_V1,
    val lang: String,
    val kind: String,
    val name: String,
    val owner: String = "",
    val descriptor: String = "",
    val extras: Map<String, String> = emptyMap(),
) {
    public companion object {
        public const val SCHEMA_V1: Int = 1
    }
}

public object SemanticNodeCodec {
    private val json = Json { ignoreUnknownKeys = true }

    public fun encode(node: SemanticNode): String = json.encodeToString(SemanticNode.serializer(), node)

    public fun decode(payload: String): SemanticNode? {
        if (payload.isBlank()) return null
        return runCatching {
            json.decodeFromString(SemanticNode.serializer(), payload)
        }.getOrNull()?.takeIf { it.lang.isNotBlank() && it.kind.isNotBlank() }
    }
}
