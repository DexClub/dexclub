package io.github.dexclub.codeview.treesitter.smali.session

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.document.CodeDocumentSnapshot
import io.github.dexclub.codeview.core.token.CodeTokenSpan
import io.github.dexclub.codeview.language.session.CodeLanguageSession
import io.github.dexclub.codeview.treesitter.highlight.TreeSitterHighlighter
import io.github.dexclub.codeview.treesitter.smali.internal.query.SmaliQueries
import io.github.dexclub.codeview.treesitter.smali.internal.semantic.SmaliAnnotationBuilder
import io.github.dexclub.treesitter.smali.TreeSitterSmali
import io.github.treesitter.ktreesitter.Language

internal class SmaliLanguageSession(
    private val document: CodeDocument,
) : CodeLanguageSession {

    private val language = Language(TreeSitterSmali.language())
    private val highlighter = TreeSitterHighlighter(language)

    override suspend fun highlightTokens(snapshot: CodeDocumentSnapshot): List<CodeTokenSpan> {
        return highlighter.highlight(snapshot.text, SmaliQueries.highlights())
    }

    override suspend fun annotations(snapshot: CodeDocumentSnapshot): List<CodeAnnotation> {
        return SmaliAnnotationBuilder.build(snapshot.text, language)
    }

    override fun close() {
        highlighter.close()
    }
}
