package io.github.dexclub.codeview.treesitter.highlight

import io.github.dexclub.codeview.core.text.TextOffsetRange
import io.github.dexclub.codeview.core.token.CodeTokenSpan
import io.github.dexclub.codeview.treesitter.bridge.TSLanguage
import io.github.dexclub.codeview.treesitter.bridge.TSTree
import io.github.dexclub.codeview.treesitter.bridge.parseString
import io.github.treesitter.ktreesitter.Parser
import io.github.treesitter.ktreesitter.Query

/**
 * Reusable tree-sitter highlight engine for a single language session.
 *
 * Holds a [Parser] and the last parsed [TSTree] for incremental re-parsing.
 * Call [close] when the session ends to release native resources.
 */
class TreeSitterHighlighter(private val language: TSLanguage) {
    private val parser = Parser(language)
    private var tree: TSTree? = null

    fun highlight(text: String, highlightsQuery: String): List<CodeTokenSpan> {
        tree = parser.parseString(tree, text)
        val root = tree!!.rootNode

        val query = Query(language, highlightsQuery)
        val spans = mutableListOf<CodeTokenSpan>()

        for ((_, match) in query.captures(root)) {
            for (capture in match.captures) {
                val kind = TreeSitterTokenMapper.map(capture.name) ?: continue
                val start = capture.node.startByte.toInt()
                val end = capture.node.endByte.toInt()
                if (start >= end) continue
                spans.add(CodeTokenSpan(range = TextOffsetRange(start, end), kind = kind))
            }
        }

        return spans
    }

    fun close() {
        tree = null
        // Parser and Tree are managed by the GC; no explicit close needed in ktreesitter 0.24.x
    }
}
