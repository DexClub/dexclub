package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal class CodeEditorImeSelectionState {
    // Tracks IME-driven selection sessions where anchor/caret direction matters.
    private var actionSelectionActive: Boolean = false
    private var anchorOffset: Int? = null
    private var softKeyboardShiftActive: Boolean = false
    // Some IMEs exit selection mode by collapsing back to the old anchor.
    private var pendingCollapseRewriteStartOffset: Int? = null
    private var pendingCollapseOffset: Int? = null
    // Some soft keyboards emit one extra collapsed setSelection with an unrelated offset
    // after Shift-selection finishes. Rewrite only the next collapsed request to the live caret.
    private var pendingSoftKeyboardCollapseOffset: Int? = null

    fun shouldExtendSelection(isHardwareShiftPressed: Boolean): Boolean {
        return isHardwareShiftPressed || actionSelectionActive || softKeyboardShiftActive
    }

    fun activateSelectionAction(fieldValue: TextFieldValue) {
        actionSelectionActive = true
        anchorOffset = selectionAnchor(fieldValue)
        clearPendingCollapseRequests()
    }

    fun deactivateSelectionAction(fieldValue: TextFieldValue): Int {
        actionSelectionActive = false
        val selectionAnchor = anchorOffset ?: selectionAnchor(fieldValue)
        val collapseOffset = selectionCaret(fieldValue)
        if (!fieldValue.selection.collapsed && selectionAnchor != collapseOffset) {
            pendingCollapseRewriteStartOffset = selectionAnchor
            pendingCollapseOffset = collapseOffset
        } else {
            clearPendingActionCollapseRewrite()
        }
        clearPendingSoftKeyboardCollapse()
        if (!softKeyboardShiftActive) {
            anchorOffset = null
        }
        return collapseOffset
    }

    fun clear() {
        clearSelectionModes()
        clearPendingCollapseRequests()
    }

    fun resolveSelection(
        fieldValue: TextFieldValue,
        mappedStart: Int,
        mappedEnd: Int,
    ): TextRange {
        val currentSelectionStart = selectionAnchor(fieldValue)
        val currentSelectionEnd = selectionCaret(fieldValue)
        val currentSelectionMin = minOf(currentSelectionStart, currentSelectionEnd)
        val currentSelectionMax = maxOf(currentSelectionStart, currentSelectionEnd)
        val collapsedCurrentCaretSelection = collapseToOffset(currentSelectionEnd, fieldValue)

        // Some IMEs occasionally report a collapsed caret completely outside the live selection.
        // Treat that as "finish selection at the current caret" instead of trusting the offset.
        if (
            shouldRewriteExternalCollapsedSelection(
                fieldValue = fieldValue,
                mappedStart = mappedStart,
                mappedEnd = mappedEnd,
                currentSelectionMin = currentSelectionMin,
                currentSelectionMax = currentSelectionMax,
            )
        ) {
            clearSelectionModes()
            clearPendingCollapseRequests()
            return collapsedCurrentCaretSelection
        }

        val pendingSoftKeyboardCollapse = pendingSoftKeyboardCollapseOffset
        if (
            pendingSoftKeyboardCollapse != null &&
            !fieldValue.selection.collapsed &&
            mappedStart == mappedEnd
        ) {
            // Shift-selection may end with one trailing collapsed request. Rewrite only that one.
            clearSelectionModes()
            clearPendingCollapseRequests()
            return collapseToOffset(pendingSoftKeyboardCollapse, fieldValue)
        }

        if (
            softKeyboardShiftActive &&
            !fieldValue.selection.collapsed &&
            mappedStart == mappedEnd
        ) {
            clearSelectionModes()
            clearPendingCollapseRequests()
            return collapsedCurrentCaretSelection
        }

        if (
            !actionSelectionActive &&
            !fieldValue.selection.collapsed &&
            mappedStart == mappedEnd &&
            mappedStart == currentSelectionStart &&
            currentSelectionEnd != currentSelectionStart
        ) {
            actionSelectionActive = false
            clearPendingActionCollapseRewrite()
            if (!softKeyboardShiftActive) {
                anchorOffset = null
            }
            return collapsedCurrentCaretSelection
        }

        if (!actionSelectionActive) {
            val pendingStart = pendingCollapseRewriteStartOffset
            val pendingCollapse = pendingCollapseOffset
            if (
                mappedStart == mappedEnd &&
                pendingStart != null &&
                pendingCollapse != null &&
                mappedStart == pendingStart
            ) {
                clearPendingCollapseRequests()
                return collapseToOffset(pendingCollapse, fieldValue)
            }
            clearPendingCollapseRequests()
            return TextRange(
                start = mappedStart,
                end = mappedEnd,
            )
        }

        val selectionAnchor = anchorOffset
            ?: selectionAnchor(fieldValue).also { anchorOffset = it }
        val collapseOffset = selectionCaret(fieldValue)
        return TextRange(
            start = selectionAnchor,
            end = mappedEnd,
        )
    }

    fun onSelectionApplied(nextSelection: TextRange) {
        val collapsedAtActionAnchor =
            actionSelectionActive &&
                nextSelection.collapsed &&
                anchorOffset != null &&
                nextSelection.end == anchorOffset

        if (actionSelectionActive && nextSelection.collapsed && !collapsedAtActionAnchor) {
            actionSelectionActive = false
        }
        if (!(nextSelection.collapsed && pendingCollapseOffset == nextSelection.end)) {
            clearPendingActionCollapseRewrite()
        }
        if (nextSelection.collapsed && !actionSelectionActive) {
            softKeyboardShiftActive = false
        }
        if (!(nextSelection.collapsed && pendingSoftKeyboardCollapseOffset == nextSelection.end)) {
            clearPendingSoftKeyboardCollapse()
        }
        if (!actionSelectionActive && !softKeyboardShiftActive) {
            anchorOffset = null
        }
    }

    fun updateSoftKeyboardShiftState(
        active: Boolean,
        fieldValue: TextFieldValue,
    ) {
        when {
            active -> {
                softKeyboardShiftActive = true
                clearPendingSoftKeyboardCollapse()
                if (anchorOffset == null) {
                    anchorOffset = selectionAnchor(fieldValue)
                }
            }

            else -> {
                softKeyboardShiftActive = false
                // Remember where the visual caret should stay if the IME sends one last collapse.
                pendingSoftKeyboardCollapseOffset = if (!actionSelectionActive && !fieldValue.selection.collapsed) {
                    selectionCaret(fieldValue)
                } else {
                    null
                }
                if (!actionSelectionActive) {
                    anchorOffset = null
                }
            }
        }
    }

    private fun shouldRewriteExternalCollapsedSelection(
        fieldValue: TextFieldValue,
        mappedStart: Int,
        mappedEnd: Int,
        currentSelectionMin: Int,
        currentSelectionMax: Int,
    ): Boolean {
        if (fieldValue.selection.collapsed) return false
        if (mappedStart != mappedEnd) return false
        return mappedStart !in currentSelectionMin..currentSelectionMax
    }

    private fun selectionAnchor(fieldValue: TextFieldValue): Int {
        return fieldValue.selection.start.coerceIn(0, fieldValue.text.length)
    }

    private fun selectionCaret(fieldValue: TextFieldValue): Int {
        return fieldValue.selection.end.coerceIn(0, fieldValue.text.length)
    }

    private fun collapseToOffset(
        offset: Int,
        fieldValue: TextFieldValue,
    ): TextRange {
        val safeOffset = offset.coerceIn(0, fieldValue.text.length)
        return TextRange(
            start = safeOffset,
            end = safeOffset,
        )
    }

    private fun clearPendingActionCollapseRewrite() {
        pendingCollapseRewriteStartOffset = null
        pendingCollapseOffset = null
    }

    private fun clearPendingSoftKeyboardCollapse() {
        pendingSoftKeyboardCollapseOffset = null
    }

    private fun clearPendingCollapseRequests() {
        clearPendingActionCollapseRewrite()
        clearPendingSoftKeyboardCollapse()
    }

    private fun clearSelectionModes() {
        actionSelectionActive = false
        softKeyboardShiftActive = false
        anchorOffset = null
    }
}
