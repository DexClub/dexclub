package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeEditorImeSelectionStateTest {
    @Test
    fun resolveSelection_rewritesCollapsedSelectionOutsideForwardRangeToCaretEnd() {
        val state = CodeEditorImeSelectionState()
        val result = state.resolveSelection(
            fieldValue = TextFieldValue(
                text = buildString { repeat(9000) { append('a') } },
                selection = TextRange(8107, 8200),
            ),
            mappedStart = 1024,
            mappedEnd = 1024,
        )

        assertEquals(TextRange(8200), result)
    }

    @Test
    fun resolveSelection_rewritesCollapsedSelectionOutsideBackwardRangeToCaretEnd() {
        val state = CodeEditorImeSelectionState()
        val result = state.resolveSelection(
            fieldValue = TextFieldValue(
                text = buildString { repeat(9000) { append('a') } },
                selection = TextRange(6986, 6796),
            ),
            mappedStart = 1024,
            mappedEnd = 1024,
        )

        assertEquals(TextRange(6796), result)
    }

    @Test
    fun resolveSelection_rewritesTrailingSoftKeyboardCollapseAfterShiftSelection() {
        val state = CodeEditorImeSelectionState()
        val fieldValue = TextFieldValue(
            text = "0123456789",
            selection = TextRange(2, 7),
        )

        state.updateSoftKeyboardShiftState(
            active = true,
            fieldValue = TextFieldValue("0123456789", TextRange(2)),
        )
        state.updateSoftKeyboardShiftState(
            active = false,
            fieldValue = fieldValue,
        )

        val result = state.resolveSelection(
            fieldValue = fieldValue,
            mappedStart = 0,
            mappedEnd = 0,
        )

        assertEquals(TextRange(7), result)
    }

    @Test
    fun resolveSelection_rewritesAnchorCollapseAfterSelectionActionStops() {
        val state = CodeEditorImeSelectionState()
        val fieldValue = TextFieldValue(
            text = "0123456789",
            selection = TextRange(3, 8),
        )

        state.activateSelectionAction(TextFieldValue("0123456789", TextRange(3)))
        val collapseOffset = state.deactivateSelectionAction(fieldValue)
        assertEquals(8, collapseOffset)

        val result = state.resolveSelection(
            fieldValue = fieldValue,
            mappedStart = 3,
            mappedEnd = 3,
        )

        assertEquals(TextRange(8), result)
    }

    @Test
    fun resolveSelection_keepsSelectionActionActiveWhenCrossingAnchorIntoReverseSelection() {
        val state = CodeEditorImeSelectionState()
        state.activateSelectionAction(
            TextFieldValue(
                text = "0123456789",
                selection = TextRange(3),
            )
        )

        val collapsedAtAnchor = state.resolveSelection(
            fieldValue = TextFieldValue(
                text = "0123456789",
                selection = TextRange(3, 8),
            ),
            mappedStart = 3,
            mappedEnd = 3,
        )
        assertEquals(TextRange(3), collapsedAtAnchor)

        state.onSelectionApplied(collapsedAtAnchor)

        val reversedSelection = state.resolveSelection(
            fieldValue = TextFieldValue(
                text = "0123456789",
                selection = collapsedAtAnchor,
            ),
            mappedStart = 2,
            mappedEnd = 2,
        )

        assertEquals(TextRange(3, 2), reversedSelection)
    }
}
