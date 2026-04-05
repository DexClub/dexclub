package io.github.dexclub.codeview.treesitter.smali.session

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.document.CodeDocumentSnapshot
import io.github.dexclub.codeview.core.text.TextOffsetRange
import io.github.dexclub.codeview.core.token.CodeTokenKind
import io.github.dexclub.codeview.core.token.CodeTokenSpan
import io.github.dexclub.codeview.language.session.CodeLanguageSession
import io.github.dexclub.codeview.treesitter.highlight.TreeSitterHighlighter
import io.github.dexclub.codeview.treesitter.smali.internal.query.SmaliQueries
import io.github.dexclub.codeview.treesitter.smali.internal.semantic.SmaliAnnotationBuilder
import io.github.dexclub.treesitter.smali.TreeSitterSmali
import io.github.treesitter.ktreesitter.Language

internal class SmaliLanguageSession(
    document: CodeDocument,
) : CodeLanguageSession {

    private val language = Language(TreeSitterSmali.language())
    private val highlighter = TreeSitterHighlighter(language)

    override suspend fun highlightTokens(snapshot: CodeDocumentSnapshot): List<CodeTokenSpan> {
        return highlighter.highlight(
            text = snapshot.text,
            highlightsQuery = SmaliQueries.highlights(),
            postProcessor = { _, spans ->
                appendDirectiveKeywordTokens(
                    text = snapshot.text,
                    spans = spans,
                )
            },
        )
    }

    override suspend fun annotations(snapshot: CodeDocumentSnapshot): List<CodeAnnotation> {
        return SmaliAnnotationBuilder.build(snapshot.text, language)
    }

    override fun close() {
        highlighter.close()
    }

    private fun appendDirectiveKeywordTokens(
        text: String,
        spans: List<CodeTokenSpan>,
    ): List<CodeTokenSpan> {
        return (spans + buildDirectiveKeywordSpans(text))
            .distinctBy { span -> Triple(span.range.start, span.range.end, span.kind) }
            .sortedWith(
                compareBy<CodeTokenSpan> { span -> span.range.start }
                    .thenBy { span -> span.range.end }
                    .thenBy { span -> span.kind.ordinal },
            )
    }

    private fun buildDirectiveKeywordSpans(text: String): List<CodeTokenSpan> {
        val spans = mutableListOf<CodeTokenSpan>()
        var lineStart = 0

        while (lineStart < text.length) {
            val lineEnd = text.indexOf('\n', startIndex = lineStart).let { index ->
                if (index == -1) text.length else index
            }
            val contentEnd = if (lineEnd > lineStart && text[lineEnd - 1] == '\r') {
                lineEnd - 1
            } else {
                lineEnd
            }
            val line = text.substring(lineStart, contentEnd)
            val directiveRange = findDirectiveRange(line)
            if (directiveRange != null) {
                val start = lineStart + directiveRange.first
                val endExclusive = lineStart + directiveRange.last + 1
                spans += CodeTokenSpan(
                    range = TextOffsetRange(
                        start = start,
                        end = endExclusive,
                    ),
                    kind = CodeTokenKind.Keyword,
                )
            }
            if (lineEnd == text.length) break
            lineStart = lineEnd + 1
        }

        return spans
    }

    private fun findDirectiveRange(line: String): IntRange? {
        val directiveStart = line.indexOfFirst { char -> !char.isWhitespace() }
        if (directiveStart == -1 || line[directiveStart] != '.') return null

        val firstWordEnd = line.indexOfFirstWhitespace(start = directiveStart).orLength(line.length)
        val firstWord = line.substring(directiveStart, firstWordEnd)
        if (firstWord in SMALI_SINGLE_WORD_DIRECTIVES) {
            return directiveStart until firstWordEnd
        }

        return when (firstWord) {
            ".end" -> line.buildCompoundDirectiveRange(
                directiveStart = directiveStart,
                firstWordEnd = firstWordEnd,
                allowedSecondWords = SMALI_END_DIRECTIVE_SUFFIXES,
            )

            ".restart" -> line.buildCompoundDirectiveRange(
                directiveStart = directiveStart,
                firstWordEnd = firstWordEnd,
                allowedSecondWords = SMALI_RESTART_DIRECTIVE_SUFFIXES,
            )

            else -> null
        }
    }

    private fun String.buildCompoundDirectiveRange(
        directiveStart: Int,
        firstWordEnd: Int,
        allowedSecondWords: Set<String>,
    ): IntRange? {
        val secondWordStart = indexOfFirstNonWhitespace(start = firstWordEnd)
        if (secondWordStart == -1) return null
        val secondWordEnd = indexOfFirstWhitespace(start = secondWordStart).orLength(length)
        val secondWord = substring(secondWordStart, secondWordEnd)
        if (secondWord !in allowedSecondWords) return null
        return directiveStart until secondWordEnd
    }

    private fun String.indexOfFirstWhitespace(start: Int): Int {
        for (index in start until length) {
            if (this[index].isWhitespace()) {
                return index
            }
        }
        return -1
    }

    private fun String.indexOfFirstNonWhitespace(start: Int): Int {
        for (index in start until length) {
            if (!this[index].isWhitespace()) {
                return index
            }
        }
        return -1
    }

    private fun Int.orLength(length: Int): Int {
        return if (this == -1) length else this
    }

    private companion object {
        val SMALI_SINGLE_WORD_DIRECTIVES: Set<String> = setOf(
            ".annotation",
            ".array-data",
            ".catch",
            ".catchall",
            ".class",
            ".enum",
            ".epilogue",
            ".field",
            ".implements",
            ".line",
            ".local",
            ".locals",
            ".method",
            ".packed-switch",
            ".param",
            ".parameter",
            ".prologue",
            ".registers",
            ".source",
            ".sparse-switch",
            ".subannotation",
            ".super",
        )

        val SMALI_END_DIRECTIVE_SUFFIXES: Set<String> = setOf(
            "annotation",
            "array-data",
            "field",
            "local",
            "method",
            "packed-switch",
            "param",
            "parameter",
            "sparse-switch",
            "subannotation",
        )

        val SMALI_RESTART_DIRECTIVE_SUFFIXES: Set<String> = setOf(
            "local",
        )
    }
}
