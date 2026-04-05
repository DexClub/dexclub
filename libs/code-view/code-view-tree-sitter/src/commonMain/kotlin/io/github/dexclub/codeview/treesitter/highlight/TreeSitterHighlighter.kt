package io.github.dexclub.codeview.treesitter.highlight

import io.github.dexclub.codeview.core.text.TextOffsetRange
import io.github.dexclub.codeview.core.token.CodeTokenSpan
import io.github.dexclub.codeview.treesitter.bridge.TSLanguage
import io.github.dexclub.codeview.treesitter.bridge.TSNode
import io.github.dexclub.codeview.treesitter.bridge.TSTree
import io.github.dexclub.codeview.treesitter.bridge.parseString
import io.github.treesitter.ktreesitter.InputEdit
import io.github.treesitter.ktreesitter.Parser
import io.github.treesitter.ktreesitter.Point
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
    private var previousText: String? = null

    fun highlight(
        text: String,
        highlightsQuery: String,
        postProcessor: ((root: TSNode, spans: List<CodeTokenSpan>) -> List<CodeTokenSpan>)? = null,
    ): List<CodeTokenSpan> {
        val previousTree = tree
        val nextTreeBase = if (previousTree != null) {
            val edit = previousText?.let { oldText ->
                buildInputEdit(
                    oldText = oldText,
                    newText = text,
                )
            }
            if (edit != null) {
                previousTree.edit(edit)
                previousTree
            } else {
                null
            }
        } else {
            null
        }

        val query = Query(language, highlightsQuery)
        val incrementalResult = runCatching {
            parseAndCollectSpans(
                text = text,
                query = query,
                previousTree = nextTreeBase,
            )
        }

        val (resolvedTree, spans) = incrementalResult.getOrElse {
            parseAndCollectSpans(
                text = text,
                query = query,
                previousTree = null,
            )
        }
        tree = resolvedTree
        previousText = text
        return postProcessor?.invoke(resolvedTree.rootNode, spans) ?: spans
    }

    fun close() {
        tree = null
        previousText = null
        // Parser and Tree are managed by the GC; no explicit close needed in ktreesitter 0.24.x
    }

    private fun parseAndCollectSpans(
        text: String,
        query: Query,
        previousTree: TSTree?,
    ): Pair<TSTree, List<CodeTokenSpan>> {
        val parsedTree = parser.parseString(previousTree, text)
        val root = parsedTree.rootNode
        val spans = mutableListOf<CodeTokenSpan>()

        for ((_, match) in query.captures(root)) {
            for (capture in match.captures) {
                val kind = TreeSitterTokenMapper.map(capture.name) ?: continue
                val start = byteOffsetToCharIndex(
                    text = text,
                    byteOffset = capture.node.startByte.toInt(),
                )
                val end = byteOffsetToCharIndex(
                    text = text,
                    byteOffset = capture.node.endByte.toInt(),
                )
                if (start >= end) continue
                spans.add(CodeTokenSpan(range = TextOffsetRange(start, end), kind = kind))
            }
        }

        return parsedTree to spans
    }
}

private fun buildInputEdit(
    oldText: String,
    newText: String,
): InputEdit? {
    if (oldText == newText) return null

    val prefixLength = longestCommonPrefixLength(
        oldText = oldText,
        newText = newText,
    )
    val suffixLength = longestCommonSuffixLength(
        oldText = oldText,
        newText = newText,
        prefixLength = prefixLength,
    )
    val oldEndCharIndex = oldText.length - suffixLength
    val newEndCharIndex = newText.length - suffixLength

    return InputEdit(
        startByte = utf8ByteOffsetAt(oldText, prefixLength).toUInt(),
        oldEndByte = utf8ByteOffsetAt(oldText, oldEndCharIndex).toUInt(),
        newEndByte = utf8ByteOffsetAt(newText, newEndCharIndex).toUInt(),
        startPoint = pointAt(oldText, prefixLength),
        oldEndPoint = pointAt(oldText, oldEndCharIndex),
        newEndPoint = pointAt(newText, newEndCharIndex),
    )
}

private fun longestCommonPrefixLength(
    oldText: String,
    newText: String,
): Int {
    val limit = minOf(oldText.length, newText.length)
    var index = 0
    while (index < limit && oldText[index] == newText[index]) {
        index += 1
    }
    return index
}

private fun longestCommonSuffixLength(
    oldText: String,
    newText: String,
    prefixLength: Int,
): Int {
    val oldRemaining = oldText.length - prefixLength
    val newRemaining = newText.length - prefixLength
    val limit = minOf(oldRemaining, newRemaining)
    var suffixLength = 0
    while (
        suffixLength < limit &&
        oldText[oldText.length - 1 - suffixLength] == newText[newText.length - 1 - suffixLength]
    ) {
        suffixLength += 1
    }
    return suffixLength
}

private fun utf8ByteOffsetAt(
    text: String,
    charIndex: Int,
): Int {
    require(charIndex in 0..text.length) { "charIndex 超出范围: $charIndex" }
    var byteOffset = 0
    var index = 0

    while (index < charIndex) {
        val char = text[index]
        byteOffset += utf8ByteLength(
            text = text,
            index = index,
        )
        index += if (char.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
            2
        } else {
            1
        }
    }

    return byteOffset
}

private fun byteOffsetToCharIndex(
    text: String,
    byteOffset: Int,
): Int {
    require(byteOffset >= 0) { "byteOffset 不能为负数: $byteOffset" }
    var consumedBytes = 0
    var charIndex = 0

    while (charIndex < text.length && consumedBytes < byteOffset) {
        val currentByteLength = utf8ByteLength(
            text = text,
            index = charIndex,
        )
        if (consumedBytes + currentByteLength > byteOffset) {
            break
        }
        consumedBytes += currentByteLength
        charIndex += if (
            text[charIndex].isHighSurrogate() &&
            charIndex + 1 < text.length &&
            text[charIndex + 1].isLowSurrogate()
        ) {
            2
        } else {
            1
        }
    }

    return charIndex
}

private fun pointAt(
    text: String,
    charIndex: Int,
): Point {
    require(charIndex in 0..text.length) { "charIndex 超出范围: $charIndex" }
    var row = 0
    var column = 0
    var index = 0

    while (index < charIndex) {
        val char = text[index]
        if (char == '\n') {
            row += 1
            column = 0
            index += 1
            continue
        }

        column += utf8ByteLength(
            text = text,
            index = index,
        )
        index += if (char.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
            2
        } else {
            1
        }
    }

    return Point(
        row = row.toUInt(),
        column = column.toUInt(),
    )
}

private fun utf8ByteLength(
    text: String,
    index: Int,
): Int {
    val char = text[index]
    return when {
        char.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate() -> 4
        char.code <= 0x7F -> 1
        char.code <= 0x7FF -> 2
        else -> 3
    }
}

private fun Char.isHighSurrogate(): Boolean = code in 0xD800..0xDBFF

private fun Char.isLowSurrogate(): Boolean = code in 0xDC00..0xDFFF
