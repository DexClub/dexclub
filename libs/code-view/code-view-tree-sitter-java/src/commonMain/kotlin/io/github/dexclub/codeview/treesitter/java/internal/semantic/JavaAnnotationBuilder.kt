package io.github.dexclub.codeview.treesitter.java.internal.semantic

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.treesitter.bridge.parseString
import io.github.dexclub.codeview.treesitter.semantic.SemanticNode
import io.github.dexclub.codeview.treesitter.semantic.SemanticNodeCodec
import io.github.dexclub.codeview.treesitter.text.TreeSitterTextOffsetResolver
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Parser

internal object JavaAnnotationBuilder {

    private const val SCHEMA_ID = "tree-sitter-java-semantic"
    private const val SCHEMA_VERSION = 1
    private val PACKAGE_REGEX = Regex("""(?m)^\s*package\s+([A-Za-z_][\w.]*)\s*;""")
    private val IMPORT_REGEX = Regex("""(?m)^\s*import\s+(?:static\s+)?([A-Za-z_][\w.]*(?:\.\*)?)\s*;""")
    private val ANNOTATION_REGEX = Regex("""@\w+(?:\([^)]*\))?""")
    private val PRIMITIVE_TYPE_DESCRIPTORS = mapOf(
        "void" to "V",
        "boolean" to "Z",
        "byte" to "B",
        "char" to "C",
        "short" to "S",
        "int" to "I",
        "long" to "J",
        "float" to "F",
        "double" to "D",
    )

    fun build(text: String, language: Language): List<CodeAnnotation> {
        val parser = Parser(language)
        return try {
            val tree = parser.parseString(null, text)
            val offsetResolver = TreeSitterTextOffsetResolver(text)
            val descriptorContext = JavaDescriptorContext.create(text)
            val annotations = mutableListOf<CodeAnnotation>()
            collectAnnotations(
                node = tree.rootNode,
                offsetResolver = offsetResolver,
                descriptorContext = descriptorContext,
                out = annotations,
            )
            annotations
        } finally {
        }
    }

    private fun collectAnnotations(
        node: Node,
        offsetResolver: TreeSitterTextOffsetResolver,
        descriptorContext: JavaDescriptorContext,
        out: MutableList<CodeAnnotation>,
    ) {
        when (node.type) {
            "class_declaration",
            "interface_declaration",
            "enum_declaration",
            "record_declaration",
            "annotation_type_declaration" -> {
                val nameNode = node.childByFieldName("name")
                if (nameNode != null) {
                    val name = offsetResolver.substring(nameNode.startByte.toInt(), nameNode.endByte.toInt())
                    out.add(
                        buildAnnotation(
                            kind = "class",
                            name = name,
                            owner = resolveOwner(node, offsetResolver),
                            descriptor = "",
                            node = nameNode,
                            offsetResolver = offsetResolver,
                        )
                    )
                }
            }

            "method_declaration",
            "constructor_declaration" -> {
                val nameNode = node.childByFieldName("name")
                if (nameNode != null) {
                    val name = offsetResolver.substring(nameNode.startByte.toInt(), nameNode.endByte.toInt())
                    out.add(
                        buildAnnotation(
                            kind = "method",
                            name = name,
                            owner = resolveOwner(node, offsetResolver),
                            descriptor = resolveMethodDescriptor(node, offsetResolver, descriptorContext),
                            node = nameNode,
                            offsetResolver = offsetResolver,
                        )
                    )
                }
            }

            "field_declaration" -> {
                val owner = resolveOwner(node, offsetResolver)
                for (child in node.children) {
                    if (child.type == "variable_declarator") {
                        val nameNode = child.childByFieldName("name")
                        if (nameNode != null) {
                            val name = offsetResolver.substring(nameNode.startByte.toInt(), nameNode.endByte.toInt())
                            out.add(buildAnnotation("field", name, owner, "", nameNode, offsetResolver))
                        }
                    }
                }
            }
        }

        for (child in node.children) {
            collectAnnotations(
                node = child,
                offsetResolver = offsetResolver,
                descriptorContext = descriptorContext,
                out = out,
            )
        }
    }

    private fun resolveOwner(
        node: Node,
        offsetResolver: TreeSitterTextOffsetResolver,
    ): String {
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
                        parts.add(0, offsetResolver.substring(nameNode.startByte.toInt(), nameNode.endByte.toInt()))
                    }
                }
            }
            current = current.parent
        }
        return parts.joinToString(".")
    }

    private fun resolveMethodDescriptor(
        node: Node,
        offsetResolver: TreeSitterTextOffsetResolver,
        descriptorContext: JavaDescriptorContext,
    ): String {
        val parameterDescriptor = buildString {
            append('(')
            val paramsNode = node.childByFieldName("parameters")
            if (paramsNode != null) {
                for (child in paramsNode.children) {
                    if (child.type == "formal_parameter" || child.type == "spread_parameter") {
                        val typeNode = child.childByFieldName("type")
                        if (typeNode != null) {
                            append(
                                descriptorContext.resolveTypeDescriptor(
                                    typeText = offsetResolver.substring(typeNode.startByte.toInt(), typeNode.endByte.toInt()),
                                    forceArray = child.type == "spread_parameter",
                                )
                            )
                        }
                    }
                }
            }
            append(')')
        }
        val returnDescriptor = when (node.type) {
            "constructor_declaration" -> "V"
            else -> {
                val typeNode = node.childByFieldName("type")
                if (typeNode != null) {
                    descriptorContext.resolveTypeDescriptor(
                        typeText = offsetResolver.substring(typeNode.startByte.toInt(), typeNode.endByte.toInt()),
                    )
                } else {
                    "V"
                }
            }
        }
        return parameterDescriptor + returnDescriptor
    }

    private fun buildAnnotation(
        kind: String,
        name: String,
        owner: String,
        descriptor: String,
        node: Node,
        offsetResolver: TreeSitterTextOffsetResolver,
    ): CodeAnnotation {
        val semantic = SemanticNode(lang = "java", kind = kind, name = name, owner = owner, descriptor = descriptor)
        return CodeAnnotation(
            range = offsetResolver.range(node.startByte.toInt(), node.endByte.toInt()),
            kind = kind,
            schemaId = SCHEMA_ID,
            schemaVersion = SCHEMA_VERSION,
            payload = SemanticNodeCodec.encode(semantic),
        )
    }

    private data class JavaDescriptorContext(
        val packageName: String,
        val importedTypes: Map<String, String>,
        val wildcardImports: List<String>,
    ) {
        fun resolveTypeDescriptor(
            typeText: String,
            forceArray: Boolean = false,
        ): String {
            val normalizedType = normalizeTypeText(typeText)
            if (normalizedType.isEmpty()) {
                return "Ljava/lang/Object;"
            }

            var componentType = normalizedType
            var arrayDepth = 0
            while (componentType.endsWith("[]")) {
                arrayDepth += 1
                componentType = componentType.removeSuffix("[]")
            }
            if (componentType.endsWith("...")) {
                arrayDepth += 1
                componentType = componentType.removeSuffix("...")
            }
            if (forceArray) {
                arrayDepth += 1
            }

            val baseDescriptor = PRIMITIVE_TYPE_DESCRIPTORS[componentType]
                ?: resolveReferenceTypeDescriptor(componentType)
            return "[".repeat(arrayDepth) + baseDescriptor
        }

        private fun resolveReferenceTypeDescriptor(typeName: String): String {
            if (typeName.startsWith('L') && typeName.endsWith(';')) {
                return typeName
            }

            val binaryName = resolveBinaryClassName(typeName)
            return "L${binaryName.replace('.', '/')};"
        }

        private fun resolveBinaryClassName(typeName: String): String {
            val rootType = typeName.substringBefore('.')
            val nestedSuffix = typeName.substringAfter('.', missingDelimiterValue = "")

            importedTypes[rootType]?.let { importedRoot ->
                return appendNestedType(importedRoot, nestedSuffix)
            }

            inferQualifiedBinaryName(typeName)?.let { return it }

            if (isLikelySimpleClassName(rootType)) {
                val localName = appendNestedType(rootType, nestedSuffix)
                return if (packageName.isBlank()) {
                    localName
                } else {
                    "$packageName.$localName"
                }
            }

            wildcardImports.firstOrNull()?.let { wildcardPackage ->
                return "$wildcardPackage.${appendNestedType(rootType, nestedSuffix)}"
            }

            return "java.lang.${appendNestedType(rootType, nestedSuffix)}"
        }

        private fun inferQualifiedBinaryName(typeName: String): String? {
            val segments = typeName.split('.').filter { it.isNotBlank() }
            if (segments.isEmpty()) return null

            val firstClassIndex = segments.indexOfFirst { segment ->
                segment.firstOrNull()?.isUpperCase() == true || segment.contains('$')
            }
            return when {
                firstClassIndex > 0 -> {
                    val packagePart = segments.take(firstClassIndex).joinToString(".")
                    val classPart = segments.drop(firstClassIndex).joinToString("$")
                    "$packagePart.$classPart"
                }

                else -> null
            }
        }

        companion object {
            fun create(text: String): JavaDescriptorContext {
                val packageName = PACKAGE_REGEX.find(text)?.groupValues?.getOrNull(1).orEmpty()
                val importedTypes = linkedMapOf<String, String>()
                val wildcardImports = mutableListOf<String>()

                IMPORT_REGEX.findAll(text).forEach { match ->
                    val importPath = match.groupValues.getOrNull(1).orEmpty()
                    if (importPath.isBlank()) return@forEach
                    if (importPath.endsWith(".*")) {
                        wildcardImports += importPath.removeSuffix(".*")
                    } else {
                        importedTypes.putIfAbsent(
                            importPath.substringAfterLast('.'),
                            normalizeImportedTypePath(importPath),
                        )
                    }
                }

                return JavaDescriptorContext(
                    packageName = packageName,
                    importedTypes = importedTypes,
                    wildcardImports = wildcardImports,
                )
            }

            private fun normalizeTypeText(typeText: String): String {
                val noAnnotations = typeText.replace(ANNOTATION_REGEX, " ")
                val noGenerics = stripGenericSections(noAnnotations)
                val noBounds = noGenerics
                    .replace(Regex("""\?\s+extends\s+"""), "")
                    .replace(Regex("""\?\s+super\s+[^,\])]+"""), "java.lang.Object")
                    .replace("?", "java.lang.Object")
                return noBounds
                    .replace(Regex("""\s+"""), "")
                    .trim()
            }

            private fun stripGenericSections(text: String): String {
                val builder = StringBuilder(text.length)
                var depth = 0
                for (char in text) {
                    when (char) {
                        '<' -> depth += 1
                        '>' -> if (depth > 0) depth -= 1
                        else -> if (depth == 0) builder.append(char)
                    }
                }
                return builder.toString()
            }

            private fun appendNestedType(rootType: String, nestedSuffix: String): String {
                return if (nestedSuffix.isEmpty()) {
                    rootType
                } else {
                    rootType + "$" + nestedSuffix.replace('.', '$')
                }
            }

            private fun isLikelySimpleClassName(typeName: String): Boolean {
                val first = typeName.firstOrNull() ?: return false
                return first.isUpperCase() || first == '_' || first == '$'
            }

            private fun normalizeImportedTypePath(importPath: String): String {
                val segments = importPath.split('.').filter { it.isNotBlank() }
                if (segments.isEmpty()) return importPath

                val firstClassIndex = segments.indexOfFirst { segment ->
                    segment.firstOrNull()?.isUpperCase() == true || segment.contains('$')
                }
                return if (firstClassIndex > 0) {
                    val packagePart = segments.take(firstClassIndex).joinToString(".")
                    val classPart = segments.drop(firstClassIndex).joinToString("$")
                    "$packagePart.$classPart"
                } else {
                    importPath
                }
            }
        }
    }
}
