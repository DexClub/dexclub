package io.github.dexclub.app.scene.workspace

import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection

internal data class WorkspaceInPageSearchMatch(
    val line: Int,
    val startOffset: Int,
    val endOffset: Int,
) {
    val cursor: Cursor
        get() = Cursor(
            line = line,
            offset = startOffset,
        )

    val selection: LineSelection
        get() = LineSelection(
            startLine = line,
            startOffset = startOffset,
            endLine = line,
            endOffset = endOffset,
        )
}

internal fun resolveInPageSearchMatches(
    text: String,
    query: String,
): List<WorkspaceInPageSearchMatch> {
    return resolveInPageSearchMatches(
        lines = splitCodeViewLines(text),
        query = query,
    )
}

internal fun resolveInPageSearchMatches(
    lines: List<String>,
    query: String,
): List<WorkspaceInPageSearchMatch> {
    if (lines.isEmpty() || query.isEmpty()) {
        return emptyList()
    }

    return buildList {
        lines.forEachIndexed { lineIndex, lineText ->
            var searchStart = 0
            while (searchStart <= lineText.length - query.length) {
                val matchStart = lineText.indexOf(
                    string = query,
                    startIndex = searchStart,
                )
                if (matchStart < 0) {
                    break
                }
                add(
                    WorkspaceInPageSearchMatch(
                        line = lineIndex,
                        startOffset = matchStart,
                        endOffset = matchStart + query.length,
                    ),
                )
                searchStart = matchStart + query.length
            }
        }
    }
}

internal fun findInPageSearchMatchIndex(
    matches: List<WorkspaceInPageSearchMatch>,
    selection: LineSelection,
): Int? {
    val normalizedSelection = selection.normalized()
    return matches.indexOfFirst { match ->
        match.selection == normalizedSelection
    }.takeIf { index -> index >= 0 }
}
