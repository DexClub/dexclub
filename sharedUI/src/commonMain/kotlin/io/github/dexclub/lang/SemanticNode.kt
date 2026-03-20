package io.github.dexclub.lang

import io.github.dexclub.loggerWarn
import io.github.dexclub.utils.JsonFactory
import kotlinx.serialization.Serializable

@Serializable
data class SemanticNode(
    val schema: Int = SCHEMA_V1,
    val lang: String,
    val kind: String,
    val name: String,
    val owner: String = "",
    val descriptor: String = "",
    val extras: Map<String, String> = emptyMap(),
) {
    companion object {
        const val SCHEMA_V1 = 1
    }
}

object SemanticNodeCodec {
    private const val TAG = "SemanticNodeCodec"

    fun encode(node: SemanticNode): String = JsonFactory.encodeToString(node)

    fun decode(payload: String): SemanticNode? {
        if (payload.isBlank()) return null

        val node = runCatching {
            JsonFactory.decodeFromString<SemanticNode>(payload)
        }.onFailure { throwable ->
            loggerWarn(
                text = "节点 payload 解析失败",
                throwable = throwable,
                tag = TAG,
            )
        }.getOrNull() ?: return null

        if (node.lang.isBlank() || node.kind.isBlank()) return null
        if (node.schema != SemanticNode.SCHEMA_V1) {
            loggerWarn(
                text = "节点 schema 不匹配: ${node.schema}",
                tag = TAG,
            )
        }
        return node
    }
}
