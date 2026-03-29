package io.github.dexclub.codeview.treesitter.java.session

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.document.CodeDocumentSnapshot
import io.github.dexclub.codeview.core.token.CodeTokenSpan
import io.github.dexclub.codeview.language.session.CodeLanguageSession
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
        return highlighter.highlight(snapshot.text, JavaQueries.highlights())
    }

    override suspend fun annotations(snapshot: CodeDocumentSnapshot): List<CodeAnnotation> {
        return JavaAnnotationBuilder.build(snapshot.text, language)
    }

    override fun close() {
        highlighter.close()
    }
}
