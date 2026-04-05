package io.github.dexclub.core.search

import io.github.dexclub.codeview.core.text.LineSelection
import io.github.dexclub.core.editor.EDITOR_SESSION_KIND_JAVA
import io.github.dexclub.core.editor.EDITOR_SESSION_KIND_SMALI
import io.github.dexclub.utils.SignatureUtils

data class StringSearchLocation(
    val line: Int,
    val offset: Int,
    val matchLength: Int,
) {
    val selection: LineSelection
        get() = LineSelection(
            startLine = line,
            startOffset = offset,
            endLine = line,
            endOffset = offset + matchLength,
        )
}

class StringSearchLocationResolver {
    fun resolveLocation(
        lines: List<String>,
        query: String,
        targetKind: String,
        methodDescriptor: String,
        methodName: String,
    ): StringSearchLocation? {
        if (lines.isEmpty()) return null

        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return null

        val candidates = buildStringSearchCandidates(normalizedQuery)
        if (candidates.isEmpty()) return null

        val preferredRange = when (targetKind) {
            EDITOR_SESSION_KIND_SMALI -> findSmaliMethodLineRange(
                lines = lines,
                methodDescriptor = methodDescriptor,
            )

            EDITOR_SESSION_KIND_JAVA -> findJavaMethodLineRange(
                lines = lines,
                methodDescriptor = methodDescriptor,
                methodName = methodName,
            )

            else -> null
        }

        if (preferredRange != null) {
            findStringOccurrence(
                lines = lines,
                queries = candidates,
                targetKind = targetKind,
                startLine = preferredRange.first,
                endLine = preferredRange.last,
            )?.let { return it }
        }

        return findStringOccurrence(
            lines = lines,
            queries = candidates,
            targetKind = targetKind,
            startLine = 0,
            endLine = lines.lastIndex,
        )
    }

    private fun findJavaMethodLineRange(
        lines: List<String>,
        methodDescriptor: String,
        methodName: String,
    ): IntRange? {
        val normalizedMethodName = methodName.trim()
        if (normalizedMethodName.isEmpty()) {
            return null
        }

        val declarationName = resolveJavaDeclarationMethodName(
            methodDescriptor = methodDescriptor,
            methodName = normalizedMethodName,
        )
        if (declarationName.isEmpty()) return null

        val methodRegex = Regex("""\b${Regex.escape(declarationName)}\s*\(""")
        val expectedParamTypes = parseMethodDescriptorParameterTypes(methodDescriptor)
        val candidates = mutableListOf<JavaMethodRangeCandidate>()

        for (lineIndex in lines.indices) {
            val line = stripJavaLineComment(lines[lineIndex])
            val matches = methodRegex.findAll(line).toList()
            if (matches.isEmpty()) continue

            val signatureEndLine = findJavaMethodSignatureEndLine(
                lines = lines,
                startLine = lineIndex,
            ) ?: continue
            val signatureText = buildJavaSignatureText(
                lines = lines,
                startLine = lineIndex,
                endLine = signatureEndLine,
            )
            if (!looksLikeJavaMethodSignature(signatureText)) continue

            val methodOffset = matches.firstOrNull { match ->
                isValidJavaMethodDeclarationContext(
                    signatureText = line,
                    methodOffset = match.range.first,
                )
            }?.range?.first ?: continue

            val parameterSection = extractJavaParameterSection(
                signatureText = signatureText,
                methodOffset = methodOffset,
            ) ?: continue
            val actualParamTypes = extractJavaParameterTypeNames(parameterSection)
            val parameterMatch = matchJavaMethodParameters(
                expected = expectedParamTypes,
                actual = actualParamTypes,
            )
            if (parameterMatch == ParameterMatchLevel.None) continue

            var bodyStartLine = -1
            for (candidate in lineIndex..signatureEndLine) {
                if (stripJavaLineComment(lines[candidate]).contains("{")) {
                    bodyStartLine = candidate
                    break
                }
            }
            if (bodyStartLine < 0) continue

            var braceDepth = 0
            var enteredBody = false
            var bodyEndLine = lines.lastIndex
            for (candidateLine in bodyStartLine..lines.lastIndex) {
                val text = stripJavaLineComment(lines[candidateLine])
                text.forEach { char ->
                    when (char) {
                        '{' -> {
                            braceDepth += 1
                            enteredBody = true
                        }

                        '}' -> {
                            if (braceDepth > 0) {
                                braceDepth -= 1
                            }
                        }
                    }
                }
                if (enteredBody && braceDepth == 0) {
                    bodyEndLine = candidateLine
                    break
                }
            }

            candidates += JavaMethodRangeCandidate(
                range = lineIndex..bodyEndLine,
                parameterMatch = parameterMatch,
                declarationLine = lineIndex,
            )
        }

        return candidates.minWithOrNull(
            compareBy<JavaMethodRangeCandidate> { it.parameterMatch.rank }
                .thenBy { it.declarationLine },
        )?.range
    }

    private fun findSmaliMethodLineRange(
        lines: List<String>,
        methodDescriptor: String,
    ): IntRange? {
        val descriptorSuffix = methodDescriptor.substringAfter("->", "").trim()
        if (descriptorSuffix.isEmpty()) return null

        var startLine = -1
        for (lineIndex in lines.indices) {
            val text = lines[lineIndex].substringBefore("#")
            if (!text.trimStart().startsWith(".method ")) continue
            if (!text.contains(descriptorSuffix)) continue
            startLine = lineIndex
            break
        }

        if (startLine < 0) return null

        var endLine = lines.lastIndex
        for (lineIndex in (startLine + 1)..lines.lastIndex) {
            val text = lines[lineIndex].substringBefore("#").trimStart()
            if (text.startsWith(".end method")) {
                endLine = lineIndex
                break
            }
        }

        return startLine..endLine
    }

    private fun findStringOccurrence(
        lines: List<String>,
        queries: List<String>,
        targetKind: String,
        startLine: Int,
        endLine: Int,
    ): StringSearchLocation? {
        if (queries.isEmpty()) return null
        val safeStartLine = startLine.coerceIn(0, lines.lastIndex)
        val safeEndLine = endLine.coerceIn(safeStartLine, lines.lastIndex)

        for (lineIndex in safeStartLine..safeEndLine) {
            val line = lines[lineIndex]
            val literalRanges = findStringLiteralRanges(
                line = line,
                targetKind = targetKind,
            )
            if (literalRanges.isEmpty()) continue

            for (query in queries) {
                val matchOffset = findMatchOffsetInRanges(
                    line = line,
                    query = query,
                    ranges = literalRanges,
                    ignoreCase = false,
                )
                if (matchOffset >= 0) {
                    return StringSearchLocation(
                        line = lineIndex,
                        offset = matchOffset,
                        matchLength = query.length,
                    )
                }
            }
            for (query in queries) {
                val matchOffset = findMatchOffsetInRanges(
                    line = line,
                    query = query,
                    ranges = literalRanges,
                    ignoreCase = true,
                )
                if (matchOffset >= 0) {
                    return StringSearchLocation(
                        line = lineIndex,
                        offset = matchOffset,
                        matchLength = query.length,
                    )
                }
            }
        }

        for (lineIndex in safeStartLine..safeEndLine) {
            val line = lines[lineIndex]
            for (query in queries) {
                val exactOffset = line.indexOf(query)
                if (exactOffset >= 0) {
                    return StringSearchLocation(
                        line = lineIndex,
                        offset = exactOffset,
                        matchLength = query.length,
                    )
                }
            }
            for (query in queries) {
                val ignoreCaseOffset = line.indexOf(query, ignoreCase = true)
                if (ignoreCaseOffset >= 0) {
                    return StringSearchLocation(
                        line = lineIndex,
                        offset = ignoreCaseOffset,
                        matchLength = query.length,
                    )
                }
            }
        }

        return null
    }

    private fun buildStringSearchCandidates(
        query: String,
    ): List<String> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()

        val escaped = escapeStringSearchCandidate(
            value = normalized,
            unicodeNonAscii = false,
        )
        val unicodeEscaped = escapeStringSearchCandidate(
            value = normalized,
            unicodeNonAscii = true,
        )

        return listOf(normalized, escaped, unicodeEscaped)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
    }

    private fun escapeStringSearchCandidate(
        value: String,
        unicodeNonAscii: Boolean,
    ): String {
        return buildString {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    else -> {
                        val shouldUnicodeEscape = char.code < 0x20 ||
                                char.code == 0x7F ||
                                (unicodeNonAscii && char.code > 0x7E)
                        if (shouldUnicodeEscape) {
                            append("\\u")
                            append(char.code.toString(16).padStart(4, '0'))
                        } else {
                            append(char)
                        }
                    }
                }
            }
        }
    }

    private fun resolveJavaDeclarationMethodName(
        methodDescriptor: String,
        methodName: String,
    ): String {
        if (!methodName.startsWith("<")) {
            return methodName
        }

        return when (methodName) {
            "<init>" -> normalizeTargetClassName(
                methodDescriptor.substringBefore("->"),
            ).substringAfterLast('.').substringAfterLast('$')

            else -> ""
        }
    }

    private fun parseMethodDescriptorParameterTypes(
        methodDescriptor: String,
    ): List<String> {
        val signature = methodDescriptor.substringAfter("->", "")
        val openParen = signature.indexOf('(')
        val closeParen = signature.indexOf(')', startIndex = openParen + 1)
        if (openParen < 0 || closeParen < 0 || closeParen <= openParen) return emptyList()

        val paramsDescriptor = signature.substring(openParen + 1, closeParen)
        if (paramsDescriptor.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        var index = 0
        while (index < paramsDescriptor.length) {
            val start = index
            while (index < paramsDescriptor.length && paramsDescriptor[index] == '[') {
                index += 1
            }
            if (index >= paramsDescriptor.length) break

            if (paramsDescriptor[index] == 'L') {
                val end = paramsDescriptor.indexOf(';', startIndex = index)
                if (end < 0) break
                index = end + 1
            } else {
                index += 1
            }
            result += paramsDescriptor.substring(start, index)
        }
        return result
    }

    private fun findJavaMethodSignatureEndLine(
        lines: List<String>,
        startLine: Int,
    ): Int? {
        val maxLine = minOf(startLine + JAVA_METHOD_HEADER_SCAN_LINES, lines.lastIndex)
        var parenDepth = 0
        var seenOpenParen = false
        for (lineIndex in startLine..maxLine) {
            val text = stripJavaLineComment(lines[lineIndex])
            text.forEach { char ->
                when (char) {
                    '(' -> {
                        parenDepth += 1
                        seenOpenParen = true
                    }

                    ')' -> {
                        if (parenDepth > 0) {
                            parenDepth -= 1
                        }
                    }
                }
            }
            if (seenOpenParen && parenDepth == 0 && (text.contains("{") || text.contains(";"))) {
                return lineIndex
            }
        }
        return null
    }

    private fun buildJavaSignatureText(
        lines: List<String>,
        startLine: Int,
        endLine: Int,
    ): String {
        return buildString {
            for (lineIndex in startLine..endLine) {
                if (isNotEmpty()) {
                    append('\n')
                }
                append(stripJavaLineComment(lines[lineIndex]).trim())
            }
        }
    }

    private fun looksLikeJavaMethodSignature(
        signatureText: String,
    ): Boolean {
        val normalized = signatureText.replace('\n', ' ').trim()
        if (!normalized.contains("(") || !normalized.contains(")")) return false
        if (normalized.contains("=")) return false
        if (normalized.contains(" class ") || normalized.contains(" interface ")) return false
        if (normalized.startsWith("if ") || normalized.startsWith("for ") || normalized.startsWith("while ")) return false
        if (normalized.startsWith("switch ") || normalized.startsWith("catch ") || normalized.startsWith("return ")) return false
        return true
    }

    private fun isValidJavaMethodDeclarationContext(
        signatureText: String,
        methodOffset: Int,
    ): Boolean {
        if (methodOffset <= 0) return true
        val before = signatureText.substring(0, methodOffset).trimEnd()
        if (before.isEmpty()) return false
        if (before.last() == '.') return false

        val beforeToken = before
            .takeLastWhile { it.isJavaIdentifierChar() }
            .lowercase()
        return beforeToken !in JAVA_NON_DECLARATION_PREFIX_KEYWORDS
    }

    private fun extractJavaParameterSection(
        signatureText: String,
        methodOffset: Int,
    ): String? {
        val open = signatureText.indexOf('(', startIndex = methodOffset)
        if (open < 0) return null

        var parenDepth = 0
        for (index in open until signatureText.length) {
            when (signatureText[index]) {
                '(' -> parenDepth += 1
                ')' -> {
                    if (parenDepth > 0) {
                        parenDepth -= 1
                    }
                    if (parenDepth == 0) {
                        return signatureText.substring(open + 1, index).trim()
                    }
                }
            }
        }
        return null
    }

    private fun extractJavaParameterTypeNames(
        parameterSection: String,
    ): List<String> {
        if (parameterSection.isBlank()) return emptyList()

        return splitJavaTopLevel(parameterSection, ',')
            .mapNotNull(::extractJavaParameterTypeName)
    }

    private fun splitJavaTopLevel(
        text: String,
        delimiter: Char,
    ): List<String> {
        if (text.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        var angleDepth = 0
        var bracketDepth = 0
        var parenDepth = 0
        var start = 0
        for (index in text.indices) {
            when (text[index]) {
                '<' -> angleDepth += 1
                '>' -> if (angleDepth > 0) angleDepth -= 1
                '[' -> bracketDepth += 1
                ']' -> if (bracketDepth > 0) bracketDepth -= 1
                '(' -> parenDepth += 1
                ')' -> if (parenDepth > 0) parenDepth -= 1
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

    private fun extractJavaParameterTypeName(
        parameterText: String,
    ): String? {
        val raw = parameterText.trim()
        if (raw.isEmpty()) return null

        val withoutAnnotations = raw
            .replace(JAVA_ANNOTATION_TOKEN_REGEX, " ")
            .replace("final ", " ")
            .replace("volatile ", " ")
            .replace("transient ", " ")
            .trim()
        val lastSpace = withoutAnnotations.lastIndexOf(' ')
        if (lastSpace <= 0) return withoutAnnotations.takeIf(String::isNotEmpty)

        val typePart = withoutAnnotations.substring(0, lastSpace)
            .trim()
        return normalizeJavaTypeReference(typePart)
    }

    private fun matchJavaMethodParameters(
        expected: List<String>,
        actual: List<String>,
    ): ParameterMatchLevel {
        if (expected.isEmpty() && actual.isEmpty()) {
            return ParameterMatchLevel.Exact
        }
        if (expected.size != actual.size) {
            return ParameterMatchLevel.None
        }

        var allExact = true
        for (index in expected.indices) {
            val actualType = normalizeJavaTypeReference(actual[index])
            val candidates = buildJavaTypeCandidates(expected[index])
            if (actualType !in candidates) {
                return ParameterMatchLevel.None
            }
            if (actualType != candidates.firstOrNull()) {
                allExact = false
            }
        }
        return if (allExact) {
            ParameterMatchLevel.Exact
        } else {
            ParameterMatchLevel.Compatible
        }
    }

    private fun buildJavaTypeCandidates(
        dexType: String,
    ): Set<String> {
        val arrayDepth = dexType.takeWhile { it == '[' }.length
        val elementDescriptor = dexType.drop(arrayDepth)
        val baseType = when (elementDescriptor) {
            "V" -> "void"
            "Z" -> "boolean"
            "B" -> "byte"
            "C" -> "char"
            "S" -> "short"
            "I" -> "int"
            "J" -> "long"
            "F" -> "float"
            "D" -> "double"
            else -> normalizeTargetClassName(elementDescriptor)
        }
        if (baseType.isEmpty()) return emptySet()

        val arraySuffix = "[]".repeat(arrayDepth)
        val normalizedBase = baseType.replace('$', '.')
        val simpleBase = normalizedBase.substringAfterLast('.')
        val packageLessQualifiedBase = normalizedBase
            .split('.')
            .dropWhile { segment -> segment.firstOrNull()?.isLowerCase() == true }
            .joinToString(".")
        return buildSet {
            add("$normalizedBase$arraySuffix")
            add("$simpleBase$arraySuffix")
            if (packageLessQualifiedBase.isNotEmpty()) {
                add("$packageLessQualifiedBase$arraySuffix")
            }
        }
    }

    private fun normalizeJavaTypeReference(
        typeName: String,
    ): String {
        val normalized = typeName
            .replace(JAVA_ANNOTATION_TOKEN_REGEX, " ")
            .replace("final ", " ")
            .replace("volatile ", " ")
            .replace("transient ", " ")
            .trim()
        if (normalized.isEmpty()) return ""

        val withoutGenerics = stripJavaGenericArguments(normalized)
        return withoutGenerics
            .replace(" ?", "?")
            .replace("? ", "?")
            .replace("...", "[]")
            .replace(" [", "[")
            .replace("[ ", "[")
            .replace(" ]", "]")
            .replace("] ", "]")
            .replace('$', '.')
            .trim()
    }

    private fun stripJavaGenericArguments(
        typeName: String,
    ): String {
        if (typeName.isEmpty()) return typeName

        val builder = StringBuilder(typeName.length)
        var angleDepth = 0
        for (char in typeName) {
            when (char) {
                '<' -> angleDepth += 1
                '>' -> if (angleDepth > 0) angleDepth -= 1
                else -> if (angleDepth == 0) {
                    builder.append(char)
                }
            }
        }
        return builder.toString()
    }

    private fun findStringLiteralRanges(
        line: String,
        targetKind: String,
    ): List<IntRange> {
        return when (targetKind) {
            EDITOR_SESSION_KIND_SMALI -> findQuotedStringRanges(
                line = line,
                commentPrefix = '#',
            )

            EDITOR_SESSION_KIND_JAVA -> findQuotedStringRanges(
                line = line,
                commentPrefix = '/',
            )

            else -> emptyList()
        }
    }

    private fun findQuotedStringRanges(
        line: String,
        commentPrefix: Char,
    ): List<IntRange> {
        val result = mutableListOf<IntRange>()
        var inString = false
        var escaped = false
        var start = -1
        var index = 0
        while (index < line.length) {
            val char = line[index]
            if (!inString && isCommentStart(line, index, commentPrefix)) {
                break
            }

            if (char == '"' && !escaped) {
                if (inString) {
                    result += start..index
                    inString = false
                    start = -1
                } else {
                    inString = true
                    start = index
                }
            }

            escaped = if (inString && char == '\\' && !escaped) {
                true
            } else {
                false
            }
            index += 1
        }
        return result
    }

    private fun isCommentStart(
        line: String,
        index: Int,
        commentPrefix: Char,
    ): Boolean {
        return when (commentPrefix) {
            '#' -> line[index] == '#'
            '/' -> line[index] == '/' && index + 1 < line.length && line[index + 1] == '/'
            else -> false
        }
    }

    private fun findMatchOffsetInRanges(
        line: String,
        query: String,
        ranges: List<IntRange>,
        ignoreCase: Boolean,
    ): Int {
        if (query.isEmpty()) return -1

        for (range in ranges) {
            val start = range.first.coerceAtLeast(0)
            val endExclusive = (range.last + 1).coerceAtMost(line.length)
            val fromIndex = line.indexOf(
                string = query,
                startIndex = start,
                ignoreCase = ignoreCase,
            )
            if (fromIndex >= start && fromIndex + query.length <= endExclusive) {
                return fromIndex
            }
        }
        return -1
    }

    private fun stripJavaLineComment(line: String): String = line.substringBefore("//")

    private fun normalizeTargetClassName(value: String): String {
        val raw = value.trim()
        if (raw.isEmpty()) return ""

        val descriptor = CLASS_SIGNATURE_REGEX.find(raw)?.value
        if (!descriptor.isNullOrEmpty()) {
            return SignatureUtils.typeName(descriptor)
        }

        val beforeMember = raw.substringBefore("->").trim()
        if (beforeMember.startsWith('L') && beforeMember.endsWith(';')) {
            return SignatureUtils.typeName(beforeMember)
        }

        val normalized = beforeMember
            .replace('/', '.')
            .removeSuffix(";")
            .trim()
        return if ('/' in beforeMember) {
            normalized.removePrefix("L")
        } else {
            normalized
        }
    }

    private fun Char.isJavaIdentifierChar(): Boolean = isLetterOrDigit() || this == '_' || this == '$'

    private data class JavaMethodRangeCandidate(
        val range: IntRange,
        val parameterMatch: ParameterMatchLevel,
        val declarationLine: Int,
    )

    private enum class ParameterMatchLevel(
        val rank: Int,
    ) {
        Exact(rank = 0),
        Compatible(rank = 1),
        None(rank = Int.MAX_VALUE),
    }

    private companion object {
        private val JAVA_NON_DECLARATION_PREFIX_KEYWORDS = setOf(
            "if",
            "for",
            "while",
            "switch",
            "catch",
            "return",
            "throw",
            "new",
            "do",
            "else",
        )
        private const val JAVA_METHOD_HEADER_SCAN_LINES = 12
        private val JAVA_ANNOTATION_TOKEN_REGEX = Regex("""@[\w.]+(?:\([^)]*\))?\s*""")
        private val CLASS_SIGNATURE_REGEX = Regex("L[\\w/$]+;")
    }
}
