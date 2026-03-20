package io.github.dexclub.codeview.runtime.degrade

import io.github.dexclub.codeview.core.api.InternalCodeViewApi
import io.github.dexclub.codeview.core.document.CodeDocumentSnapshot

@InternalCodeViewApi
internal class DefaultSurfaceDegradePolicy : SurfaceDegradePolicy {
    private val largeFileThreshold = 512 * 1024
    private val oversizedFileThreshold = 2 * 1024 * 1024
    private val longLineThreshold = 4096

    override fun evaluate(snapshot: CodeDocumentSnapshot): DegradeDecision {
        val sizeBytes = snapshot.textLength
        
        if (sizeBytes > oversizedFileThreshold) {
            return DegradeDecision.OversizedFile(sizeBytes)
        }
        
        if (sizeBytes > largeFileThreshold) {
            return DegradeDecision.LargeFile(sizeBytes)
        }
        
        val longLines = findLongLines(snapshot)
        if (longLines.isNotEmpty()) {
            return DegradeDecision.LongLines(longLines)
        }
        
        return DegradeDecision.None
    }

    private fun findLongLines(snapshot: CodeDocumentSnapshot): List<Int> {
        val text = snapshot.text
        val lines = mutableListOf<Int>()
        var lineIndex = 0
        var lineStart = 0
        
        for (i in text.indices) {
            if (text[i] == '\n') {
                val lineLength = i - lineStart
                if (lineLength > longLineThreshold) {
                    lines.add(lineIndex)
                }
                lineIndex++
                lineStart = i + 1
            }
        }
        
        val lastLineLength = text.length - lineStart
        if (lastLineLength > longLineThreshold) {
            lines.add(lineIndex)
        }
        
        return lines
    }
}
