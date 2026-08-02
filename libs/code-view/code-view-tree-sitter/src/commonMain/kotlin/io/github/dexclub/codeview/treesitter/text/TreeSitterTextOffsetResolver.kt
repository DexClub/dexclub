package io.github.dexclub.codeview.treesitter.text

import io.github.dexclub.codeview.core.api.InternalCodeViewApi
import io.github.dexclub.codeview.core.text.TextOffsetRange

@InternalCodeViewApi
public class TreeSitterTextOffsetResolver(
    private val text: String,
) {
    private var currentByteOffset: Int = 0
    private var currentCharIndex: Int = 0

    public fun charIndexAt(byteOffset: Int): Int {
        require(byteOffset >= 0) { "byteOffset 不能为负数: $byteOffset" }
        if (byteOffset < currentByteOffset) {
            currentByteOffset = 0
            currentCharIndex = 0
        }

        while (currentCharIndex < text.length && currentByteOffset < byteOffset) {
            val currentByteLength = utf8ByteLength(
                text = text,
                index = currentCharIndex,
            )
            if (currentByteOffset + currentByteLength > byteOffset) {
                break
            }
            currentByteOffset += currentByteLength
            currentCharIndex += charStepAt(text, currentCharIndex)
        }

        return currentCharIndex
    }

    public fun substring(
        startByte: Int,
        endByte: Int,
    ): String {
        val start = charIndexAt(startByte)
        val end = charIndexAt(endByte)
        if (start >= end) {
            return ""
        }
        return text.substring(start, end)
    }

    public fun range(
        startByte: Int,
        endByte: Int,
    ): TextOffsetRange {
        val start = charIndexAt(startByte)
        val end = charIndexAt(endByte)
        return TextOffsetRange(
            start = start,
            end = end.coerceAtLeast(start),
        )
    }
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

private fun charStepAt(
    text: String,
    index: Int,
): Int {
    val char = text[index]
    return if (char.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
        2
    } else {
        1
    }
}

private fun Char.isHighSurrogate(): Boolean = code in 0xD800..0xDBFF

private fun Char.isLowSurrogate(): Boolean = code in 0xDC00..0xDFFF
