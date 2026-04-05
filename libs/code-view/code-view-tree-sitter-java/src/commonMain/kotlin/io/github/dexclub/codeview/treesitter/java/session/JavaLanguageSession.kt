package io.github.dexclub.codeview.treesitter.java.session

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.document.CodeDocumentSnapshot
import io.github.dexclub.codeview.core.token.CodeTokenKind
import io.github.dexclub.codeview.core.token.CodeTokenSpan
import io.github.dexclub.codeview.language.session.CodeLanguageSession
import io.github.dexclub.codeview.treesitter.bridge.TSNode
import io.github.dexclub.codeview.treesitter.highlight.TreeSitterHighlighter
import io.github.dexclub.codeview.treesitter.java.internal.query.JavaQueries
import io.github.dexclub.codeview.treesitter.java.internal.semantic.JavaAnnotationBuilder
import io.github.dexclub.treesitter.java.TreeSitterJava
import io.github.treesitter.ktreesitter.Language

internal class JavaLanguageSession(
    private val document: CodeDocument,
) : CodeLanguageSession {

    private val language = Language(TreeSitterJava.language())
    private val highlighter = TreeSitterHighlighter(language)

    override suspend fun highlightTokens(snapshot: CodeDocumentSnapshot): List<CodeTokenSpan> {
        return highlighter.highlight(
            text = snapshot.text,
            highlightsQuery = JavaQueries.highlights(),
            postProcessor = { root, spans ->
                filterMisclassifiedTypeTokens(
                    text = snapshot.text,
                    root = root,
                    spans = spans,
                )
            },
        )
    }

    override suspend fun annotations(snapshot: CodeDocumentSnapshot): List<CodeAnnotation> {
        return JavaAnnotationBuilder.build(snapshot.text, language)
    }

    override fun close() {
        highlighter.close()
    }

    private fun filterMisclassifiedTypeTokens(
        text: String,
        root: TSNode,
        spans: List<CodeTokenSpan>,
    ): List<CodeTokenSpan> {
        if (spans.isEmpty()) return spans
        val sortedSpans = spans.sortedBy { token -> token.range.start }

        return sortedSpans.filterIndexed { index, token ->
            !shouldDropToken(
                text = text,
                token = token,
                nextToken = sortedSpans.getOrNull(index + 1),
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun shouldDropToken(
        text: String,
        token: CodeTokenSpan,
        nextToken: CodeTokenSpan?,
    ): Boolean {
        val tokenText = text.substring(token.range.start, token.range.end)
        return when (token.kind) {
            CodeTokenKind.TypeName -> shouldDropTypeNameToken(
                text = text,
                token = token,
                tokenText = tokenText,
                nextToken = nextToken,
            )

            CodeTokenKind.ConstantName -> !tokenText.matches(JAVA_UPPERCASE_CONSTANT_REGEX)
            else -> false
        }
    }

    private fun shouldDropTypeNameToken(
        text: String,
        token: CodeTokenSpan,
        tokenText: String,
        nextToken: CodeTokenSpan?,
    ): Boolean {
        if (tokenText.isEmpty()) return false
        if (tokenText.firstOrNull()?.isUpperCase() == true) return false
        if (!tokenText.looksLikeLowercaseIdentifier()) return true
        if (isWholeMalformedLineToken(text, token)) return true
        if (!hasDeclarationBoundaryBefore(text, token.range.start)) return true

        val next = nextToken ?: return token.range.end == text.length
        val nextText = text.substring(next.range.start, next.range.end)
        return next.kind == CodeTokenKind.KeywordType ||
            next.kind == CodeTokenKind.TypeName ||
            nextText.firstOrNull()?.isUpperCase() == true ||
            nextText in JAVA_DECLARATION_PREFIX_KEYWORDS
    }

    private fun isWholeMalformedLineToken(
        text: String,
        token: CodeTokenSpan,
    ): Boolean {
        val lineStart = text.lastIndexOf('\n', startIndex = (token.range.start - 1).coerceAtLeast(0))
            .let { if (it == -1) 0 else it + 1 }
        val lineEndExclusive = text.indexOf('\n', startIndex = token.range.end)
            .let { if (it == -1) text.length else it }
        val beforeToken = text.substring(lineStart, token.range.start)
        val afterToken = text.substring(token.range.end, lineEndExclusive)
        val trimmedSuffix = afterToken.trimStart()
        return beforeToken.isBlank() && (trimmedSuffix.isEmpty() || trimmedSuffix.startsWith("//"))
    }

    private fun hasDeclarationBoundaryBefore(
        text: String,
        start: Int,
    ): Boolean {
        var index = start - 1
        while (index >= 0 && text[index].isWhitespace()) {
            index -= 1
        }
        if (index < 0) return true
        return text[index] in JAVA_DECLARATION_BOUNDARY_CHARS
    }

    private fun String.looksLikeLowercaseIdentifier(): Boolean {
        if (isEmpty()) return false
        val first = first()
        if (!(first == '_' || first in 'a'..'z')) return false
        return all { char ->
            char == '_' || char in 'a'..'z' || char in 'A'..'Z' || char.isDigit()
        }
    }

    private companion object {
        val JAVA_DECLARATION_PREFIX_KEYWORDS: Set<String> = setOf(
            "abstract",
            "class",
            "enum",
            "final",
            "interface",
            "native",
            "non-sealed",
            "private",
            "protected",
            "public",
            "record",
            "sealed",
            "static",
            "strictfp",
            "synchronized",
            "transient",
            "volatile",
        )

        val JAVA_DECLARATION_BOUNDARY_CHARS: Set<Char> = setOf(
            '\n',
            '\r',
            '{',
            '}',
            ';',
            '(',
        )

        val JAVA_UPPERCASE_CONSTANT_REGEX: Regex = Regex("^_*[A-Z][A-Z\\d_]+$")
    }
}
