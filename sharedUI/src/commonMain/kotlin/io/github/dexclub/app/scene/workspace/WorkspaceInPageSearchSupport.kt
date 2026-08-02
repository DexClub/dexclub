package io.github.dexclub.app.scene.workspace

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
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
            offset = endOffset,
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
    caseSensitive: Boolean = false,
    wholeWord: Boolean = false,
): List<WorkspaceInPageSearchMatch> {
    return resolveInPageSearchMatches(
        lines = splitCodeViewLines(text),
        query = query,
        caseSensitive = caseSensitive,
        wholeWord = wholeWord,
    )
}

internal fun resolveInPageSearchMatches(
    lines: List<String>,
    query: String,
    caseSensitive: Boolean = false,
    wholeWord: Boolean = false,
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
                    ignoreCase = !caseSensitive,
                )
                if (matchStart < 0) {
                    break
                }
                val matchEnd = matchStart + query.length
                if (wholeWord && !isWholeWordSearchMatch(lineText, matchStart, matchEnd)) {
                    searchStart = matchStart + 1
                    continue
                }
                add(
                    WorkspaceInPageSearchMatch(
                        line = lineIndex,
                        startOffset = matchStart,
                        endOffset = matchEnd,
                    ),
                )
                searchStart = matchEnd
            }
        }
    }
}

private fun isWholeWordSearchMatch(
    lineText: String,
    matchStart: Int,
    matchEnd: Int,
): Boolean {
    val beforeChar = lineText.getOrNull(matchStart - 1)
    val afterChar = lineText.getOrNull(matchEnd)
    return !beforeChar.isSearchTokenChar() && !afterChar.isSearchTokenChar()
}

private fun Char?.isSearchTokenChar(): Boolean {
    val value = this ?: return false
    return value.isLetterOrDigit() || value == '_' || value == '$'
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

internal fun isInPageSearchShortcut(
    keyEvent: KeyEvent,
): Boolean {
    return keyEvent.key == Key.F &&
            (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)
}

internal fun isCodeCopyShortcut(
    keyEvent: KeyEvent,
): Boolean {
    return keyEvent.key == Key.C &&
            (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)
}
