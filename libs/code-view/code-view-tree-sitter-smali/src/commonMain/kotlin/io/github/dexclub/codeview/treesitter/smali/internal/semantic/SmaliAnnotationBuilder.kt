package io.github.dexclub.codeview.treesitter.smali.internal.semantic

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.treesitter.bridge.parseString
import io.github.dexclub.codeview.treesitter.semantic.SemanticNode
import io.github.dexclub.codeview.treesitter.semantic.SemanticNodeCodec
import io.github.dexclub.codeview.treesitter.text.TreeSitterTextOffsetResolver
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Parser

internal object SmaliAnnotationBuilder {

    private const val SCHEMA_ID = "tree-sitter-smali-semantic"
    private const val SCHEMA_VERSION = 1

    fun build(text: String, language: Language): List<CodeAnnotation> {
        val parser = Parser(language)
        return try {
            val tree = parser.parseString(null, text)
            val offsetResolver = TreeSitterTextOffsetResolver(text)
            val annotations = mutableListOf<CodeAnnotation>()
            collectAnnotations(tree.rootNode, offsetResolver, annotations)
            annotations
        } finally {
        }
    }

    private fun collectAnnotations(
        node: Node,
        offsetResolver: TreeSitterTextOffsetResolver,
        out: MutableList<CodeAnnotation>,
    ) {
        when (node.type) {
            "class_definition" -> {
                val classIdNode = node.children.firstOrNull { it.type == "class_identifier" }
                if (classIdNode != null) {
                    val className = offsetResolver.substring(classIdNode.startByte.toInt(), classIdNode.endByte.toInt())
                    out.add(buildAnnotation("class", className, "", "", classIdNode, offsetResolver))
                }
                for (child in node.children) {
                    when (child.type) {
                        "method_definition", "field_definition" ->
                            collectAnnotations(child, offsetResolver, out)
                    }
                }
                return
            }

            "method_definition" -> {
                val sigNode = node.children.firstOrNull { it.type == "method_signature" }
                if (sigNode != null) {
                    val methodIdNode = sigNode.children.firstOrNull { it.type == "method_identifier" }
                    if (methodIdNode != null) {
                        val name = offsetResolver.substring(methodIdNode.startByte.toInt(), methodIdNode.endByte.toInt())
                        out.add(
                            buildAnnotation(
                                kind = "method",
                                name = name,
                                owner = resolveClassOwner(node, offsetResolver),
                                descriptor = resolveMethodDescriptor(sigNode, offsetResolver),
                                node = methodIdNode,
                                offsetResolver = offsetResolver,
                            )
                        )
                    }
                }
                return
            }

            "field_definition" -> {
                val fieldIdNode = node.children.firstOrNull { it.type == "field_identifier" }
                if (fieldIdNode != null) {
                    val name = offsetResolver.substring(fieldIdNode.startByte.toInt(), fieldIdNode.endByte.toInt())
                    out.add(buildAnnotation("field", name, resolveClassOwner(node, offsetResolver), "", fieldIdNode, offsetResolver))
                }
                return
            }
        }

        for (child in node.children) {
            collectAnnotations(child, offsetResolver, out)
        }
    }

    private fun resolveClassOwner(
        node: Node,
        offsetResolver: TreeSitterTextOffsetResolver,
    ): String {
        var current = node.parent
        while (current != null) {
            if (current.type == "class_definition") {
                val classIdNode = current.children.firstOrNull { it.type == "class_identifier" }
                if (classIdNode != null) {
                    return offsetResolver.substring(classIdNode.startByte.toInt(), classIdNode.endByte.toInt())
                }
            }
            current = current.parent
        }
        return ""
    }

    private fun resolveMethodDescriptor(
        methodSignatureNode: Node,
        offsetResolver: TreeSitterTextOffsetResolver,
    ): String {
        val signatureText = offsetResolver.substring(
            methodSignatureNode.startByte.toInt(),
            methodSignatureNode.endByte.toInt(),
        )
        val methodNameNode = methodSignatureNode.children.firstOrNull { it.type == "method_identifier" }
            ?: return signatureText
        val methodName = offsetResolver.substring(
            methodNameNode.startByte.toInt(),
            methodNameNode.endByte.toInt(),
        )
        return signatureText.removePrefix(methodName)
    }

    private fun buildAnnotation(
        kind: String,
        name: String,
        owner: String,
        descriptor: String,
        node: Node,
        offsetResolver: TreeSitterTextOffsetResolver,
    ): CodeAnnotation {
        val semantic = SemanticNode(lang = "smali", kind = kind, name = name, owner = owner, descriptor = descriptor)
        return CodeAnnotation(
            range = offsetResolver.range(node.startByte.toInt(), node.endByte.toInt()),
            kind = kind,
            schemaId = SCHEMA_ID,
            schemaVersion = SCHEMA_VERSION,
            payload = SemanticNodeCodec.encode(semantic),
        )
    }
}
