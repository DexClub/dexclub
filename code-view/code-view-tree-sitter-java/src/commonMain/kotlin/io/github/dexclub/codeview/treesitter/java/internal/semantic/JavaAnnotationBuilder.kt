package io.github.dexclub.codeview.treesitter.java.internal.semantic

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.core.text.TextOffsetRange
import io.github.dexclub.codeview.treesitter.bridge.parseString
import io.github.dexclub.codeview.treesitter.semantic.SemanticNode
import io.github.dexclub.codeview.treesitter.semantic.SemanticNodeCodec
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Parser

internal object JavaAnnotationBuilder {

    private const val SCHEMA_ID = "tree-sitter-java-semantic"
    private const val SCHEMA_VERSION = 1

    fun build(text: String, language: Language): List<CodeAnnotation> {
        val parser = Parser(language)
        return try {
            val tree = parser.parseString(null, text)
            val annotations = mutableListOf<CodeAnnotation>()
            collectAnnotations(tree.rootNode, text, annotations)
            annotations
        } finally {
        }
    }

    private fun collectAnnotations(node: Node, text: String, out: MutableList<CodeAnnotation>) {
        when (node.type) {
            "class_declaration",
            "interface_declaration",
            "enum_declaration",
            "record_declaration",
            "annotation_type_declaration" -> {
                val nameNode = node.childByFieldName("name")
                if (nameNode != null) {
                    val name = text.substring(nameNode.startByte.toInt(), nameNode.endByte.toInt())
                    out.add(buildAnnotation("class", name, resolveOwner(node, text), "", nameNode))
                }
            }

            "method_declaration",
            "constructor_declaration" -> {
                val nameNode = node.childByFieldName("name")
                if (nameNode != null) {
                    val name = text.substring(nameNode.startByte.toInt(), nameNode.endByte.toInt())
                    out.add(buildAnnotation("method", name, resolveOwner(node, text), resolveMethodDescriptor(node, text), nameNode))
                }
            }

            "field_declaration" -> {
                val owner = resolveOwner(node, text)
                for (child in node.children) {
                    if (child.type == "variable_declarator") {
                        val nameNode = child.childByFieldName("name")
                        if (nameNode != null) {
                            val name = text.substring(nameNode.startByte.toInt(), nameNode.endByte.toInt())
                            out.add(buildAnnotation("field", name, owner, "", nameNode))
                        }
                    }
                }
            }
        }

        for (child in node.children) {
            collectAnnotations(child, text, out)
        }
    }

    private fun resolveOwner(node: Node, text: String): String {
        val parts = mutableListOf<String>()
        var current = node.parent
        while (current != null) {
            when (current.type) {
                "class_declaration",
                "interface_declaration",
                "enum_declaration",
                "record_declaration",
                "annotation_type_declaration" -> {
                    val nameNode = current.childByFieldName("name")
                    if (nameNode != null) {
                        parts.add(0, text.substring(nameNode.startByte.toInt(), nameNode.endByte.toInt()))
                    }
                }
            }
            current = current.parent
        }
        return parts.joinToString(".")
    }

    private fun resolveMethodDescriptor(node: Node, text: String): String {
        val params = node.childByFieldName("parameters") ?: return "()"
        val sb = StringBuilder("(")
        for (child in params.children) {
            if (child.type == "formal_parameter" || child.type == "spread_parameter") {
                val typeNode = child.childByFieldName("type")
                if (typeNode != null) {
                    sb.append(text.substring(typeNode.startByte.toInt(), typeNode.endByte.toInt()))
                    sb.append(",")
                }
            }
        }
        sb.append(")")
        return sb.toString()
    }

    private fun buildAnnotation(
        kind: String,
        name: String,
        owner: String,
        descriptor: String,
        node: Node,
    ): CodeAnnotation {
        val semantic = SemanticNode(lang = "java", kind = kind, name = name, owner = owner, descriptor = descriptor)
        return CodeAnnotation(
            range = TextOffsetRange(
                start = node.startByte.toInt(),
                end = node.endByte.toInt(),
            ),
            kind = kind,
            schemaId = SCHEMA_ID,
            schemaVersion = SCHEMA_VERSION,
            payload = SemanticNodeCodec.encode(semantic),
        )
    }
}
