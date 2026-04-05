package io.github.dexclub.core.navigation

import io.github.dexclub.app.model.OpenTabUiModel
import io.github.dexclub.core.editor.EDITOR_SESSION_KIND_JAVA
import io.github.dexclub.core.editor.EDITOR_SESSION_KIND_SMALI
import io.github.dexclub.core.editor.EDITOR_SESSION_TARGET_TYPE_CLASS
import io.github.dexclub.core.editor.ExportCodePathForClass
import io.github.dexclub.core.editor.OpenTabService
import io.github.dexclub.core.editor.toSessionTabRecord
import io.github.dexclub.core.navigation.resolver.JavaDeclarationResolver
import io.github.dexclub.core.navigation.resolver.SmaliDeclarationResolver
import io.github.dexclub.core.workspace.WorkspaceIndexService
import io.github.dexclub.core.workspace.WorkspaceIndexedClassRecord
import io.github.dexclub.utils.SignatureUtils

data class PreparedNavigationDestination(
    val tabId: String,
    val kind: String,
    val paneIndex: Int,
)

class NavigationService(
    private val openTabService: OpenTabService,
    private val workspaceIndexService: WorkspaceIndexService,
    private val javaDeclarationResolver: DeclarationResolver = JavaDeclarationResolver(),
    private val smaliDeclarationResolver: DeclarationResolver = SmaliDeclarationResolver(),
) {
    suspend fun resolveDeclaration(
        context: NavigateRequestContext,
        activeLines: List<String>,
        sourceClassName: String,
        workspaceId: Long?,
        workspaceName: String,
    ): JumpResolveResult {
        val resolver = when (context.semanticNode.lang.lowercase()) {
            "java" -> javaDeclarationResolver
            "smali" -> smaliDeclarationResolver
            else -> {
                return JumpResolveResult.Unsupported(
                    reason = "不支持语言: ${context.semanticNode.lang}",
                )
            }
        }

        if (activeLines.isEmpty()) {
            return JumpResolveResult.Error(
                reason = "当前代码未加载: tabId=${context.tabId}, kind=${context.activeKind}",
            )
        }

        val env = ResolverEnv(
            workspaceId = workspaceId,
            workspaceName = workspaceName,
            activeLines = activeLines,
            sourceClassName = sourceClassName,
        )
        return runCatching {
            resolver.resolve(context, env)
        }.getOrElse { throwable ->
            JumpResolveResult.Error(
                reason = throwable.message ?: "解析异常",
            )
        }
    }

    suspend fun prepareDeclarationDestination(
        sourceTab: OpenTabUiModel,
        targetClassName: String,
        preferredKind: String,
        exportCodePathForClass: ExportCodePathForClass,
    ): PreparedNavigationDestination? {
        val normalizedTargetClass = normalizeTargetClassName(targetClassName)
            .ifEmpty { sourceTab.targetKey }

        val targetTab = if (normalizedTargetClass == sourceTab.targetKey) {
            openTabService.getTabById(sourceTab.tabId) ?: sourceTab.toSessionTabRecord()
        } else {
            val cls = findTargetClassRecord(normalizedTargetClass) ?: return null
            openTabService.getTabByTarget(EDITOR_SESSION_TARGET_TYPE_CLASS, cls.className)
                ?: openTabService.ensureClassTab(cls)
        }
        val destinationPreferredKind = if (normalizedTargetClass == sourceTab.targetKey) {
            normalizeNavigationKind(
                targetKind = preferredKind,
                fallbackKind = sourceTab.activeKind,
            )
        } else {
            val existing = openTabService.getTabByTarget(EDITOR_SESSION_TARGET_TYPE_CLASS, normalizedTargetClass)
            if (existing != null) {
                normalizeNavigationKind(
                    targetKind = preferredKind,
                    fallbackKind = existing.activeKind,
                )
            } else {
                EDITOR_SESSION_KIND_SMALI
            }
        }

        val usableKind = openTabService.ensureTabReadyForNavigationKind(
            tab = targetTab,
            preferredKind = destinationPreferredKind,
            exportCodePathForClass = exportCodePathForClass,
        ) ?: return null

        return buildPreparedDestination(targetTab.tabId, usableKind)
    }

    suspend fun prepareSearchDestination(
        className: String,
        preferredKind: String,
        exportCodePathForClass: ExportCodePathForClass,
    ): PreparedNavigationDestination? {
        val cls = findTargetClassRecord(className) ?: return null
        val targetTab = openTabService.getTabByTarget(EDITOR_SESSION_TARGET_TYPE_CLASS, cls.className)
            ?: openTabService.ensureClassTab(cls)
        val usableKind = openTabService.ensureTabReadyForNavigationKind(
            tab = targetTab,
            preferredKind = normalizeNavigationKind(
                targetKind = preferredKind,
                fallbackKind = targetTab.activeKind,
            ),
            exportCodePathForClass = exportCodePathForClass,
        ) ?: return null

        return buildPreparedDestination(targetTab.tabId, usableKind)
    }

    fun normalizeTargetClassName(value: String): String {
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

    fun resolveTargetCursor(
        context: NavigateRequestContext,
        target: JumpTarget,
        lines: List<String>,
        targetKind: String,
    ): Pair<Int, Int> {
        if (lines.isEmpty()) return 0 to 0

        var line = target.targetLine
        var offset = target.targetOffset

        if (line !in lines.indices || (line == 0 && offset == 0)) {
            val inferred = inferAnchorBySemanticKind(
                name = context.semanticNode.name,
                semanticKind = context.semanticNode.kind,
                targetKind = targetKind,
                lines = lines,
            )
            if (inferred != null) {
                line = inferred.first
                offset = inferred.second
            }
        }

        val safeLine = line.coerceIn(0, lines.lastIndex)
        val safeOffset = offset.coerceIn(0, lines[safeLine].length)
        return safeLine to safeOffset
    }

    private suspend fun buildPreparedDestination(
        tabId: String,
        kind: String,
    ): PreparedNavigationDestination? {
        val latest = openTabService.getTabById(tabId) ?: return null
        val panes = openTabService.getPanesByTabId(tabId)
        val paneIndex = panes.firstOrNull { pane -> pane.kind == kind }?.paneIndex
            ?: latest.activePaneIndex.coerceAtLeast(0)

        return PreparedNavigationDestination(
            tabId = latest.tabId,
            kind = kind,
            paneIndex = paneIndex,
        )
    }

    private suspend fun findTargetClassRecord(targetClassName: String): WorkspaceIndexedClassRecord? {
        val normalized = normalizeTargetClassName(targetClassName)
        if (normalized.isEmpty()) return null
        return workspaceIndexService.findByName(normalized)
    }

    private fun normalizeNavigationKind(
        targetKind: String,
        fallbackKind: String,
    ): String {
        val normalizedTarget = targetKind.lowercase()
        if (normalizedTarget == EDITOR_SESSION_KIND_JAVA || normalizedTarget == EDITOR_SESSION_KIND_SMALI) {
            return normalizedTarget
        }

        val normalizedFallback = fallbackKind.lowercase()
        if (normalizedFallback == EDITOR_SESSION_KIND_JAVA || normalizedFallback == EDITOR_SESSION_KIND_SMALI) {
            return normalizedFallback
        }
        return EDITOR_SESSION_KIND_SMALI
    }

    private fun inferAnchorBySemanticKind(
        name: String,
        semanticKind: String,
        targetKind: String,
        lines: List<String>,
    ): Pair<Int, Int>? {
        if (name.isBlank()) return null
        return when (targetKind) {
            EDITOR_SESSION_KIND_JAVA -> findJavaDeclarationAnchor(lines, name, semanticKind)
            EDITOR_SESSION_KIND_SMALI -> findSmaliDeclarationAnchor(lines, name, semanticKind)
            else -> null
        }
    }

    private fun findJavaDeclarationAnchor(
        lines: List<String>,
        name: String,
        semanticKind: String,
    ): Pair<Int, Int>? {
        val escaped = Regex.escape(name)
        val classRegex = Regex("""\b(class|interface|enum|record)\s+$escaped\b|\b@interface\s+$escaped\b""")
        val methodRegex = Regex("""\b$escaped\s*\(""")

        for (line in lines.indices) {
            val text = stripJavaLineComment(lines[line])
            when (semanticKind) {
                "class", "annotation", "import" -> {
                    val match = classRegex.find(text) ?: continue
                    return line to match.range.first
                }

                "method" -> {
                    for (match in methodRegex.findAll(text)) {
                        if (looksLikeJavaMethodDeclaration(text, match.range.first, name)) {
                            return line to match.range.first
                        }
                    }
                }

                "field" -> return findJavaFieldDeclarationAnchor(lines, name)
                "identifier" -> return findJavaIdentifierDeclarationAnchor(lines, name)
            }
        }
        return null
    }

    private fun findJavaFieldDeclarationAnchor(
        lines: List<String>,
        name: String,
    ): Pair<Int, Int>? {
        if (name.isBlank()) return null
        val fieldRegex = Regex("""\b${Regex.escape(name)}\b""")
        val depthBefore = computeBraceDepthBefore(lines)
        val classMemberDepths = collectJavaTypeMemberDepths(lines, depthBefore)
            .ifEmpty { setOf(1) }

        for (line in lines.indices) {
            if (depthBefore.getOrElse(line) { 0 } !in classMemberDepths) continue
            val text = stripJavaLineComment(lines[line])
            if (';' !in text) continue

            for (match in fieldRegex.findAll(text)) {
                if (looksLikeJavaFieldDeclaration(text, match.range.first, name)) {
                    return line to match.range.first
                }
            }
        }
        return null
    }

    private fun findJavaIdentifierDeclarationAnchor(
        lines: List<String>,
        name: String,
    ): Pair<Int, Int>? {
        if (name.isBlank()) return null
        val identifierRegex = Regex("""\b${Regex.escape(name)}\b""")
        for (line in lines.indices) {
            val text = stripJavaLineComment(lines[line])
            if (';' !in text) continue

            for (match in identifierRegex.findAll(text)) {
                if (looksLikeJavaVariableDeclaration(text, match.range.first, name)) {
                    return line to match.range.first
                }
            }
        }
        return null
    }

    private fun findSmaliDeclarationAnchor(
        lines: List<String>,
        name: String,
        semanticKind: String,
    ): Pair<Int, Int>? {
        val methodMarker = "$name("
        val fieldRegex = Regex("""\b${Regex.escape(name)}\s*:""")

        for (line in lines.indices) {
            val text = lines[line].substringBefore('#')
            when (semanticKind) {
                "class", "annotation", "import" -> {
                    if (text.trimStart().startsWith(".class ")) {
                        val offset = text.indexOf(".class")
                        return line to offset.coerceAtLeast(0)
                    }
                }

                "method" -> {
                    if (!text.trimStart().startsWith(".method ")) continue
                    val offset = text.indexOf(methodMarker)
                    if (offset >= 0) return line to offset
                }

                "field", "identifier" -> {
                    if (!text.trimStart().startsWith(".field ")) continue
                    val match = fieldRegex.find(text) ?: continue
                    return line to match.range.first
                }
            }
        }
        return null
    }

    private fun looksLikeJavaMethodDeclaration(
        line: String,
        nameOffset: Int,
        name: String,
    ): Boolean {
        if (nameOffset < 0) return false
        if (nameOffset > 0 && line[nameOffset - 1] == '.') return false

        val before = line.substring(0, nameOffset).trimEnd()
        if (before.isEmpty()) return false
        if (before.contains("=")) return false

        val beforeToken = before.substringAfterLast(' ').substringAfterLast('\t')
        if (beforeToken in JAVA_NON_DECLARATION_PREFIX_KEYWORDS) return false
        if (before.endsWith("return") || before.endsWith("throw") || before.endsWith("new")) return false

        val after = line.substring(nameOffset + name.length).trimStart()
        if (!after.startsWith("(")) return false

        val closeParen = after.indexOf(')')
        if (closeParen < 0) return true

        val afterParen = after.substring(closeParen + 1).trimStart()
        if (afterParen.startsWith(".") || afterParen.startsWith("->")) return false
        if (afterParen.isEmpty()) return true

        return afterParen.startsWith("{") ||
                afterParen.startsWith("throws") ||
                afterParen.startsWith(";") ||
                afterParen.startsWith("default")
    }

    private fun looksLikeJavaVariableDeclaration(
        line: String,
        nameOffset: Int,
        name: String,
    ): Boolean {
        if (nameOffset < 0) return false
        if (nameOffset > 0 && line[nameOffset - 1] == '.') return false
        if (';' !in line) return false

        val before = line.substring(0, nameOffset).trimEnd()
        val after = line.substring(nameOffset + name.length).trimStart()
        if (before.isEmpty()) return false
        if (!(after.startsWith("=") || after.startsWith(";") || after.startsWith(",")
                    || after.startsWith("[") || after.startsWith(":"))
        ) {
            return false
        }
        if (before.contains("=")) return false
        if (before.endsWith(".")) return false

        val beforeToken = before.substringAfterLast(' ').substringAfterLast('\t')
        if (beforeToken in JAVA_NON_DECLARATION_PREFIX_KEYWORDS) return false
        return true
    }

    private fun looksLikeJavaFieldDeclaration(
        line: String,
        nameOffset: Int,
        name: String,
    ): Boolean {
        if (nameOffset < 0) return false
        if (nameOffset > 0 && line[nameOffset - 1] == '.') return false
        if (';' !in line) return false

        val before = line.substring(0, nameOffset).trimEnd()
        val after = line.substring(nameOffset + name.length).trimStart()
        if (before.isEmpty()) return false
        if (before.contains("(")) return false
        if (before.endsWith(".")) return false
        if (before.startsWith("import ") || before.startsWith("package ")) return false
        if (!(after.startsWith("=") || after.startsWith(";") || after.startsWith(",") || after.startsWith("["))) {
            return false
        }
        val beforeToken = before.substringAfterLast(' ').substringAfterLast('\t')
        if (beforeToken in JAVA_NON_DECLARATION_PREFIX_KEYWORDS) return false
        return true
    }

    private fun computeBraceDepthBefore(lines: List<String>): IntArray {
        val depthBefore = IntArray(lines.size)
        var depth = 0
        for (line in lines.indices) {
            depthBefore[line] = depth
            val text = stripJavaLineComment(lines[line])
            for (ch in text) {
                when (ch) {
                    '{' -> depth++
                    '}' -> depth = (depth - 1).coerceAtLeast(0)
                }
            }
        }
        return depthBefore
    }

    private fun collectJavaTypeMemberDepths(
        lines: List<String>,
        depthBefore: IntArray,
    ): Set<Int> {
        if (lines.isEmpty()) return emptySet()
        val memberDepths = linkedSetOf<Int>()
        val maxLine = lines.lastIndex

        for (start in lines.indices) {
            val header = stripJavaLineComment(lines[start])
            if (!looksLikeJavaTypeDeclarationHeader(header)) continue

            val maxHeaderEnd = (start + MAX_JAVA_TYPE_HEADER_SPAN).coerceAtMost(maxLine)
            var bodyStartLine = -1
            for (line in start..maxHeaderEnd) {
                if (stripJavaLineComment(lines[line]).contains("{")) {
                    bodyStartLine = line
                    break
                }
            }
            if (bodyStartLine < 0) continue

            val baseDepth = depthBefore.getOrElse(bodyStartLine) { 0 }
            memberDepths += (baseDepth + 1)
        }

        return memberDepths
    }

    private fun looksLikeJavaTypeDeclarationHeader(headerText: String): Boolean {
        val normalized = headerText.trim()
        if (normalized.isEmpty()) return false
        return JAVA_TYPE_DECLARATION_HEADER_REGEX.containsMatchIn(normalized)
    }

    private fun stripJavaLineComment(line: String): String = line.substringBefore("//")

    private companion object {
        private val CLASS_SIGNATURE_REGEX = Regex("L[\\w/$]+;")
        private val JAVA_TYPE_DECLARATION_HEADER_REGEX = Regex(
            "(^|\\s)(class|interface|enum|record)\\s+[A-Za-z_\\$]|(^|\\s)@interface\\s+[A-Za-z_\\$]",
        )
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
        private const val MAX_JAVA_TYPE_HEADER_SPAN = 12
    }
}
