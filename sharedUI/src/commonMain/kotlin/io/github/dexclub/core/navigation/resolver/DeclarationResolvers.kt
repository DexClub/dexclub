package io.github.dexclub.core.navigation.resolver

import io.github.dexclub.core.navigation.DeclarationResolver
import io.github.dexclub.core.navigation.JumpResolveResult
import io.github.dexclub.core.navigation.JumpTarget
import io.github.dexclub.core.navigation.NavigateRequestContext
import io.github.dexclub.core.navigation.ResolverEnv
import io.github.dexclub.utils.SignatureUtils

class JavaDeclarationResolver : DeclarationResolver {
    override suspend fun resolve(
        context: NavigateRequestContext,
        env: ResolverEnv,
    ): JumpResolveResult {
        if (context.semanticNode.lang.lowercase() != "java") {
            return JumpResolveResult.Unsupported("Resolver 与语言不匹配: ${context.semanticNode.lang}")
        }
        if (env.activeLines.isEmpty()) {
            return JumpResolveResult.Error("当前 Java 源码未加载")
        }

        val name = context.semanticNode.name
        if (name.isBlank() && context.semanticNode.kind != "class" && context.semanticNode.kind != "annotation") {
            return JumpResolveResult.Unsupported("空标识符")
        }

        when (context.semanticNode.kind) {
            "class", "annotation", "import" -> {
                val targetClass = resolveClassName(
                    candidate = firstNonBlank(
                        context.semanticNode.descriptor,
                        context.semanticNode.owner,
                        context.semanticNode.name,
                    ),
                    fallback = env.sourceClassName,
                )
                if (targetClass.isNotEmpty()) {
                    return JumpResolveResult.Resolved(
                        target = JumpTarget(
                            targetClassName = targetClass,
                            targetKind = "java",
                            reason = "java_${context.semanticNode.kind}",
                        )
                    )
                }
            }

            "method" -> {
                val targetClass = resolveClassName(
                    candidate = firstNonBlank(
                        extractOwnerFromMemberDescriptor(context.semanticNode.descriptor),
                        context.semanticNode.owner,
                    ),
                    fallback = env.sourceClassName,
                )
                if (targetClass.isNotEmpty()) {
                    return JumpResolveResult.Resolved(
                        target = JumpTarget(
                            targetClassName = targetClass,
                            targetKind = "java",
                            reason = "java_method",
                        )
                    )
                }
            }
        }

        val (rawClickedLine, clickedLineOffset) = globalOffsetToLineOffset(env.activeLines, context.annotationHit.range.start)
        val clickedLine = rawClickedLine.coerceIn(0, env.activeLines.lastIndex)
        val depthBefore = computeBraceDepthBefore(env.activeLines)
        val typeContext = findEnclosingTypeContext(
            lines = env.activeLines,
            depthBefore = depthBefore,
            clickedLine = clickedLine,
        )

        val local = if (context.semanticNode.kind == "identifier") {
            findLocalDeclaration(
                lines = env.activeLines,
                depthBefore = depthBefore,
                name = name,
                clickedLine = clickedLine,
                clickedTokenStart = clickedLineOffset,
                typeContext = typeContext,
            )
        } else {
            null
        }
        if (local != null) {
            return JumpResolveResult.Resolved(
                target = JumpTarget(
                    targetClassName = env.sourceClassName,
                    targetKind = "java",
                    targetLine = local.line,
                    targetOffset = local.offset,
                    reason = local.reason,
                )
            )
        }

        val parameter = if (context.semanticNode.kind == "identifier") {
            findParameterDeclaration(
                lines = env.activeLines,
                depthBefore = depthBefore,
                name = name,
                clickedLine = clickedLine,
                typeContext = typeContext,
            )
        } else {
            null
        }
        if (parameter != null) {
            return JumpResolveResult.Resolved(
                target = JumpTarget(
                    targetClassName = env.sourceClassName,
                    targetKind = "java",
                    targetLine = parameter.line,
                    targetOffset = parameter.offset,
                    reason = parameter.reason,
                )
            )
        }

        val field = if (context.semanticNode.kind == "identifier" || context.semanticNode.kind == "field") {
            findFieldDeclaration(
                lines = env.activeLines,
                depthBefore = depthBefore,
                name = name,
                clickedLine = clickedLine,
                typeContext = typeContext,
            )
        } else {
            null
        }
        if (field != null) {
            return JumpResolveResult.Resolved(
                target = JumpTarget(
                    targetClassName = env.sourceClassName,
                    targetKind = "java",
                    targetLine = field.line,
                    targetOffset = field.offset,
                    reason = field.reason,
                )
            )
        }

        val ownerClass = resolveClassName(
            candidate = firstNonBlank(
                extractOwnerFromMemberDescriptor(context.semanticNode.descriptor),
                context.semanticNode.owner,
            ),
            fallback = env.sourceClassName,
        )
        if (ownerClass.isNotEmpty()) {
            return JumpResolveResult.Resolved(
                target = JumpTarget(
                    targetClassName = ownerClass,
                    targetKind = "java",
                    reason = "java_owner_fallback",
                )
            )
        }

        return JumpResolveResult.NotFound(
            reason = "同文件未找到声明: kind=${context.semanticNode.kind}, name=$name",
        )
    }
}

class SmaliDeclarationResolver : DeclarationResolver {
    override suspend fun resolve(
        context: NavigateRequestContext,
        env: ResolverEnv,
    ): JumpResolveResult {
        if (context.semanticNode.lang.lowercase() != "smali") {
            return JumpResolveResult.Unsupported("Resolver 与语言不匹配: ${context.semanticNode.lang}")
        }

        val classCandidate = when (context.semanticNode.kind) {
            "class" -> firstNonBlank(
                context.semanticNode.descriptor,
                context.semanticNode.owner,
                context.semanticNode.name,
            )

            "method", "field" -> firstNonBlank(
                extractOwnerFromMemberDescriptor(context.semanticNode.descriptor),
                context.semanticNode.owner,
            )

            else -> ""
        }
        val targetClass = resolveClassName(
            candidate = classCandidate,
            fallback = env.sourceClassName,
        )
        if (targetClass.isEmpty()) {
            return JumpResolveResult.NotFound(
                reason = "Smali 无法解析目标类: kind=${context.semanticNode.kind}, name=${context.semanticNode.name}",
            )
        }

        when (context.semanticNode.kind) {
            "class" -> {
                return JumpResolveResult.Resolved(
                    target = JumpTarget(
                        targetClassName = targetClass,
                        targetKind = "smali",
                        reason = "smali_class",
                    )
                )
            }

            "method" -> {
                if (targetClass == env.sourceClassName && env.activeLines.isNotEmpty()) {
                    val local = findSmaliMethodDeclaration(
                        lines = env.activeLines,
                        name = context.semanticNode.name,
                    )
                    if (local != null) {
                        return JumpResolveResult.Resolved(
                            target = JumpTarget(
                                targetClassName = targetClass,
                                targetKind = "smali",
                                targetLine = local.line,
                                targetOffset = local.offset,
                                reason = local.reason,
                            )
                        )
                    }
                }
                return JumpResolveResult.Resolved(
                    target = JumpTarget(
                        targetClassName = targetClass,
                        targetKind = "smali",
                        reason = "smali_method_owner",
                    )
                )
            }

            "field" -> {
                if (targetClass == env.sourceClassName && env.activeLines.isNotEmpty()) {
                    val local = findSmaliFieldDeclaration(
                        lines = env.activeLines,
                        name = context.semanticNode.name,
                    )
                    if (local != null) {
                        return JumpResolveResult.Resolved(
                            target = JumpTarget(
                                targetClassName = targetClass,
                                targetKind = "smali",
                                targetLine = local.line,
                                targetOffset = local.offset,
                                reason = local.reason,
                            )
                        )
                    }
                }
                return JumpResolveResult.Resolved(
                    target = JumpTarget(
                        targetClassName = targetClass,
                        targetKind = "smali",
                        reason = "smali_field_owner",
                    )
                )
            }

            else -> {
                return JumpResolveResult.Unsupported(
                    reason = "Smali 暂不支持 kind=${context.semanticNode.kind}",
                )
            }
        }
    }
}

private data class SourceAnchor(
    val line: Int,
    val offset: Int,
    val reason: String,
)

private data class MethodContext(
    val signatureStartLine: Int,
    val signatureEndLine: Int,
    val bodyStartLine: Int,
    val bodyEndLine: Int,
)

private data class TypeContext(
    val bodyStartLine: Int,
    val bodyEndLine: Int,
    val memberDepth: Int,
)

private fun findLocalDeclaration(
    lines: List<String>,
    depthBefore: IntArray,
    name: String,
    clickedLine: Int,
    clickedTokenStart: Int,
    typeContext: TypeContext?,
): SourceAnchor? {
    if (name.isBlank()) return null

    val method = findEnclosingMethodContext(
        lines = lines,
        depthBefore = depthBefore,
        clickedLine = clickedLine,
        typeContext = typeContext,
    )
    val localDepthFloor = if (method != null) {
        depthBefore.getOrElse(method.bodyStartLine) { 0 }
    } else {
        typeContext?.memberDepth ?: 1
    }
    val lowerBound = method?.bodyStartLine ?: findNearestLocalSearchLowerBound(
        depthBefore = depthBefore,
        clickedLine = clickedLine,
        depthFloor = localDepthFloor,
    )
    for (line in clickedLine downTo lowerBound) {
        if (depthBefore.getOrElse(line) { 0 } <= localDepthFloor) continue
        val text = stripInlineComment(lines[line])
        val offsets = findIdentifierOffsets(text, name)
        if (offsets.isEmpty()) continue
        for (offset in offsets.asReversed()) {
            if (line == clickedLine && offset >= clickedTokenStart) continue
            if (looksLikeVariableDeclaration(text, offset, name)) {
                return SourceAnchor(line = line, offset = offset, reason = "local")
            }
        }
    }
    return null
}

private fun findNearestLocalSearchLowerBound(
    depthBefore: IntArray,
    clickedLine: Int,
    depthFloor: Int,
): Int {
    for (line in clickedLine downTo 0) {
        if (depthBefore.getOrElse(line) { 0 } <= depthFloor) {
            return (line + 1).coerceAtMost(clickedLine)
        }
    }
    return 0
}

private fun findParameterDeclaration(
    lines: List<String>,
    depthBefore: IntArray,
    name: String,
    clickedLine: Int,
    typeContext: TypeContext?,
): SourceAnchor? {
    val method = findEnclosingMethodContext(
        lines = lines,
        depthBefore = depthBefore,
        clickedLine = clickedLine,
        typeContext = typeContext,
    ) ?: return null

    val signatureText = buildString {
        for (line in method.signatureStartLine..method.signatureEndLine) {
            append(stripInlineComment(lines[line])).append('\n')
        }
    }
    val parameterText = extractParameterSection(signatureText) ?: return null
    val parameterNames = splitTopLevel(parameterText, ',')
        .mapNotNull(::extractParameterName)

    if (name !in parameterNames) return null

    for (line in method.signatureStartLine..method.signatureEndLine) {
        val text = stripInlineComment(lines[line])
        val offsets = findIdentifierOffsets(text, name)
        if (offsets.isEmpty()) continue
        for (offset in offsets.asReversed()) {
            val after = text.substring(offset + name.length).trimStart()
            if (after.startsWith(",") || after.startsWith(")") || after.startsWith("[") || after.startsWith("...")) {
                return SourceAnchor(line = line, offset = offset, reason = "param")
            }
        }
    }
    return null
}

private fun findFieldDeclaration(
    lines: List<String>,
    depthBefore: IntArray,
    name: String,
    clickedLine: Int,
    typeContext: TypeContext?,
): SourceAnchor? {
    if (lines.isEmpty()) return null
    val activeType = typeContext ?: findEnclosingTypeContext(
        lines = lines,
        depthBefore = depthBefore,
        clickedLine = clickedLine,
    )
    val memberDepth = activeType?.memberDepth ?: 1
    val startLine = activeType?.bodyStartLine ?: 0
    val endLine = activeType?.bodyEndLine ?: lines.lastIndex

    for (line in startLine..endLine) {
        if (depthBefore.getOrElse(line) { 0 } != memberDepth) continue
        val text = stripInlineComment(lines[line])
        val offsets = findIdentifierOffsets(text, name)
        if (offsets.isEmpty()) continue
        for (offset in offsets.asReversed()) {
            if (looksLikeFieldDeclaration(text, offset, name)) {
                return SourceAnchor(line = line, offset = offset, reason = "field")
            }
        }
    }
    return null
}

private fun findEnclosingMethodContext(
    lines: List<String>,
    depthBefore: IntArray,
    clickedLine: Int,
    typeContext: TypeContext?,
): MethodContext? {
    val maxLine = lines.lastIndex
    for (start in clickedLine downTo 0) {
        val startDepth = depthBefore.getOrElse(start) { 0 }
        if (typeContext != null) {
            if (start !in typeContext.bodyStartLine..typeContext.bodyEndLine) continue
            if (startDepth != typeContext.memberDepth) continue
        }
        if (!stripInlineComment(lines[start]).contains("(")) continue

        val maxSignatureEnd = (start + 8).coerceAtMost(maxLine)
        var signatureEndLine = -1
        for (line in start..maxSignatureEnd) {
            if (stripInlineComment(lines[line]).contains(")")) {
                signatureEndLine = line
                break
            }
        }
        if (signatureEndLine < 0) continue

        val signatureText = buildString {
            for (line in start..signatureEndLine) {
                append(stripInlineComment(lines[line])).append('\n')
            }
        }
        if (!looksLikeMethodSignature(signatureText)) continue

        var bodyStartLine = -1
        for (line in start..maxSignatureEnd) {
            if (stripInlineComment(lines[line]).contains("{")) {
                bodyStartLine = line
                break
            }
        }
        if (bodyStartLine < 0) continue

        val baseDepth = depthBefore.getOrElse(bodyStartLine) { 0 }
        if (typeContext != null && baseDepth != typeContext.memberDepth) continue
        var bodyEndLine = maxLine
        for (line in (bodyStartLine + 1)..maxLine) {
            if (depthBefore.getOrElse(line) { 0 } <= baseDepth) {
                bodyEndLine = line - 1
                break
            }
        }
        if (clickedLine !in bodyStartLine..bodyEndLine) continue

        return MethodContext(
            signatureStartLine = start,
            signatureEndLine = signatureEndLine,
            bodyStartLine = bodyStartLine,
            bodyEndLine = bodyEndLine,
        )
    }
    return null
}

private fun findEnclosingTypeContext(
    lines: List<String>,
    depthBefore: IntArray,
    clickedLine: Int,
): TypeContext? {
    val maxLine = lines.lastIndex
    for (start in clickedLine downTo 0) {
        if (!looksLikeTypeDeclarationHeader(stripInlineComment(lines[start]))) continue

        val maxHeaderEnd = (start + MAX_TYPE_HEADER_SPAN).coerceAtMost(maxLine)
        var bodyStartLine = -1
        for (line in start..maxHeaderEnd) {
            if (stripInlineComment(lines[line]).contains("{")) {
                bodyStartLine = line
                break
            }
        }
        if (bodyStartLine < 0) continue

        val baseDepth = depthBefore.getOrElse(bodyStartLine) { 0 }
        var bodyEndLine = maxLine
        for (line in (bodyStartLine + 1)..maxLine) {
            if (depthBefore.getOrElse(line) { 0 } <= baseDepth) {
                bodyEndLine = line - 1
                break
            }
        }
        if (clickedLine !in bodyStartLine..bodyEndLine) continue

        return TypeContext(
            bodyStartLine = bodyStartLine,
            bodyEndLine = bodyEndLine,
            memberDepth = baseDepth + 1,
        )
    }
    return null
}

private fun looksLikeTypeDeclarationHeader(headerText: String): Boolean {
    val normalized = headerText.trim()
    if (normalized.isEmpty()) return false
    return TYPE_DECLARATION_HEADER_REGEX.containsMatchIn(normalized)
}

private fun looksLikeMethodSignature(signatureText: String): Boolean {
    val normalized = signatureText.replace('\n', ' ').trim()
    if (!normalized.contains("(") || !normalized.contains(")")) return false
    if (normalized.contains("=")) return false
    if (normalized.contains(" class ") || normalized.contains(" interface ")) return false
    if (normalized.startsWith("if ") || normalized.startsWith("for ") || normalized.startsWith("while ")) return false
    if (normalized.startsWith("switch ") || normalized.startsWith("catch ") || normalized.startsWith("return ")) return false

    val head = normalized.substringBefore("(").trim()
    val methodName = head.substringAfterLast(' ').trim()
    if (methodName.isEmpty()) return false
    if (methodName == "if" || methodName == "for" || methodName == "while" || methodName == "switch") return false
    return true
}

private fun extractParameterSection(signatureText: String): String? {
    val open = signatureText.indexOf('(')
    val close = signatureText.lastIndexOf(')')
    if (open < 0 || close <= open) return null
    return signatureText.substring(open + 1, close).trim()
}

private fun extractParameterName(parameterText: String): String? {
    val raw = parameterText.trim()
    if (raw.isEmpty()) return null

    val withoutAnnotations = raw.replace(ANNOTATION_TOKEN_REGEX, " ").replace("final ", " ")
    val token = withoutAnnotations.trim().substringAfterLast(' ').trim()
        .removeSuffix("[]")
        .removePrefix("...")
        .removeSuffix("...")
    if (token.isEmpty()) return null
    if (!token.first().isLetter() && token.first() != '_' && token.first() != '$') return null
    return token
}

private fun splitTopLevel(text: String, delimiter: Char): List<String> {
    if (text.isEmpty()) return emptyList()
    val result = mutableListOf<String>()
    var angleDepth = 0
    var bracketDepth = 0
    var parenDepth = 0
    var start = 0
    for (index in text.indices) {
        when (text[index]) {
            '<' -> angleDepth++
            '>' -> if (angleDepth > 0) angleDepth--
            '[' -> bracketDepth++
            ']' -> if (bracketDepth > 0) bracketDepth--
            '(' -> parenDepth++
            ')' -> if (parenDepth > 0) parenDepth--
            delimiter -> {
                if (angleDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                    result += text.substring(start, index)
                    start = index + 1
                }
            }
        }
    }
    result += text.substring(start)
    return result
}

private fun looksLikeVariableDeclaration(
    line: String,
    nameOffset: Int,
    name: String,
): Boolean {
    if (nameOffset < 0) return false
    if (nameOffset > 0 && line[nameOffset - 1] == '.') return false

    val before = line.substring(0, nameOffset).trimEnd()
    val after = line.substring(nameOffset + name.length).trimStart()
    if (before.isEmpty()) return false
    if (!line.contains(';')) return false
    if (!(after.startsWith("=") || after.startsWith(";") || after.startsWith(",")
                || after.startsWith("[") || after.startsWith(":"))
    ) {
        return false
    }

    val beforeToken = before.substringAfterLast(' ').substringAfterLast('\t')
    if (beforeToken in NON_DECLARATION_KEYWORDS) return false
    if (before.endsWith("return") || before.endsWith("throw")) return false
    if (before.endsWith(".")) return false
    if (hasDisallowedVariableDeclarationParenContext(before)) return false
    return true
}

private fun looksLikeFieldDeclaration(
    line: String,
    nameOffset: Int,
    name: String,
): Boolean {
    if (!line.contains(';')) return false
    if (nameOffset > 0 && line[nameOffset - 1] == '.') return false

    val before = line.substring(0, nameOffset).trimEnd()
    val after = line.substring(nameOffset + name.length).trimStart()
    if (before.isEmpty()) return false
    if (before.contains("(")) return false
    if (!(after.startsWith("=") || after.startsWith(";") || after.startsWith(",") || after.startsWith("["))) return false
    return true
}

private fun computeBraceDepthBefore(lines: List<String>): IntArray {
    val depthBefore = IntArray(lines.size)
    var depth = 0
    for (line in lines.indices) {
        depthBefore[line] = depth
        val text = stripInlineComment(lines[line])
        for (ch in text) {
            when (ch) {
                '{' -> depth++
                '}' -> depth = (depth - 1).coerceAtLeast(0)
            }
        }
    }
    return depthBefore
}

private fun stripInlineComment(line: String): String = line.substringBefore("//")

private fun findIdentifierOffsets(text: String, name: String): List<Int> {
    val result = mutableListOf<Int>()
    var fromIndex = 0
    while (true) {
        val index = text.indexOf(name, fromIndex)
        if (index < 0) return result
        val before = if (index == 0) null else text[index - 1]
        val afterIndex = index + name.length
        val after = if (afterIndex >= text.length) null else text[afterIndex]
        val isBoundary = (before == null || !before.isJavaIdentifierChar()) &&
                (after == null || !after.isJavaIdentifierChar())
        if (isBoundary) {
            result += index
        }
        fromIndex = index + 1
    }
}

private fun hasDisallowedVariableDeclarationParenContext(before: String): Boolean {
    val openParenIndex = before.lastIndexOf('(')
    if (openParenIndex < 0) return false

    val closeParenIndex = before.lastIndexOf(')')
    if (closeParenIndex > openParenIndex) return false

    val contextPrefix = before.substring(0, openParenIndex).trimEnd()
    val contextToken = contextPrefix
        .takeLastWhile { it.isJavaIdentifierChar() }
        .lowercase()
    return contextToken !in VARIABLE_DECLARATION_PAREN_CONTEXTS
}

private fun Char.isJavaIdentifierChar(): Boolean = isLetterOrDigit() || this == '_' || this == '$'

private fun firstNonBlank(vararg values: String): String {
    for (value in values) {
        if (value.isNotBlank()) return value
    }
    return ""
}

private fun resolveClassName(candidate: String, fallback: String): String {
    val normalized = normalizeClassName(candidate)
    if (normalized.isNotEmpty()) return normalized
    return normalizeClassName(fallback)
}

private fun normalizeClassName(value: String): String {
    val raw = value.trim()
    if (raw.isEmpty()) return ""

    val descriptor = CLASS_DESCRIPTOR_REGEX.find(raw)?.value
    if (!descriptor.isNullOrEmpty()) {
        return SignatureUtils.typeName(descriptor)
    }

    val beforeMember = raw.substringBefore("->").trim()
    if (beforeMember.startsWith('L') && beforeMember.endsWith(';')) {
        return SignatureUtils.typeName(beforeMember)
    }

    return beforeMember
        .replace('/', '.')
        .removePrefix("L")
        .removeSuffix(";")
        .trim()
}

private fun extractOwnerFromMemberDescriptor(descriptor: String): String {
    val raw = descriptor.trim()
    if (raw.isEmpty()) return ""
    return raw.substringBefore("->").trim()
}

private fun findSmaliMethodDeclaration(
    lines: List<String>,
    name: String,
): SourceAnchor? {
    if (name.isBlank()) return null
    val marker = "$name("
    for (line in lines.indices) {
        val text = stripSmaliComment(lines[line])
        if (!text.trimStart().startsWith(".method ")) continue
        val offset = text.indexOf(marker)
        if (offset >= 0) {
            return SourceAnchor(line = line, offset = offset, reason = "smali_method_local")
        }
    }
    return null
}

private fun findSmaliFieldDeclaration(
    lines: List<String>,
    name: String,
): SourceAnchor? {
    if (name.isBlank()) return null
    val fieldRegex = Regex("""\b${Regex.escape(name)}\s*:""")
    for (line in lines.indices) {
        val text = stripSmaliComment(lines[line])
        if (!text.trimStart().startsWith(".field ")) continue
        val match = fieldRegex.find(text) ?: continue
        return SourceAnchor(line = line, offset = match.range.first, reason = "smali_field_local")
    }
    return null
}

private fun stripSmaliComment(line: String): String = line.substringBefore("#")

private val NON_DECLARATION_KEYWORDS = setOf(
    "return",
    "throw",
    "if",
    "for",
    "while",
    "switch",
    "catch",
    "else",
    "new",
)
private val VARIABLE_DECLARATION_PAREN_CONTEXTS = setOf("for", "try", "catch")

private val ANNOTATION_TOKEN_REGEX = Regex("@[A-Za-z_\\$][A-Za-z\\d_\\$.]*(\\([^)]*\\))?")
private val CLASS_DESCRIPTOR_REGEX = Regex("L[\\w/$]+;")
private val TYPE_DECLARATION_HEADER_REGEX = Regex(
    "(^|\\s)(class|interface|enum|record)\\s+[A-Za-z_\\$]|(^|\\s)@interface\\s+[A-Za-z_\\$]",
)
private const val MAX_TYPE_HEADER_SPAN = 12

private fun globalOffsetToLineOffset(lines: List<String>, globalOffset: Int): Pair<Int, Int> {
    var remaining = globalOffset
    for (i in lines.indices) {
        val lineLen = lines[i].length + 1 // +1 for newline
        if (remaining < lineLen || i == lines.lastIndex) {
            return Pair(i, remaining.coerceIn(0, lines[i].length))
        }
        remaining -= lineLen
    }
    return Pair(0, 0)
}
