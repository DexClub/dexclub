package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class CodeEditorInputAnchorStateTest {
    @Test
    fun handleInputAnchorValueChange_selectionOnlyMoveDuringComposing_movesRealCaretAndClearsAnchor() {
        val inputAnchorState = CodeEditorInputAnchorState().apply {
            update(
                newValue = TextFieldValue(
                    text = "zh",
                    selection = TextRange(1),
                    composition = TextRange(0, 2),
                ),
                anchorSelection = TextRange(3),
                consumedSelectionOnCompose = false,
            )
        }
        val fieldValue = TextFieldValue(
            text = "hello",
            selection = TextRange(3),
        )
        var preferredColumn: Int? = 7
        var dispatchedFieldValue: TextFieldValue? = null

        handleInputAnchorValueChange(
            inputAnchorState = inputAnchorState,
            newValue = TextFieldValue(
                text = "zh",
                selection = TextRange(0),
                composition = TextRange(0, 2),
            ),
            fieldValue = fieldValue,
            onPreferredColumnChange = { preferredColumn = it },
            onFieldValueChange = { dispatchedFieldValue = it },
        )

        assertEquals(
            expected = TextFieldValue(
                text = "hello",
                selection = TextRange(2),
            ),
            actual = dispatchedFieldValue,
        )
        assertNull(inputAnchorState.anchorSelection)
        assertEquals(TextFieldValue(""), inputAnchorState.imeFieldValue)
        assertFalse(inputAnchorState.consumedSelectionOnCompose)
        assertNull(preferredColumn)
    }

    @Test
    fun handleInputAnchorValueChange_selectionMoveDuringComposingStillWorksWhenCompositionRangeChanges() {
        val inputAnchorState = CodeEditorInputAnchorState().apply {
            update(
                newValue = TextFieldValue(
                    text = "zh",
                    selection = TextRange(2),
                    composition = TextRange(0, 2),
                ),
                anchorSelection = TextRange(3),
                consumedSelectionOnCompose = false,
            )
        }
        val fieldValue = TextFieldValue(
            text = "hello",
            selection = TextRange(3),
        )
        var dispatchedFieldValue: TextFieldValue? = null

        handleInputAnchorValueChange(
            inputAnchorState = inputAnchorState,
            newValue = TextFieldValue(
                text = "zh",
                selection = TextRange(1),
                composition = TextRange(0, 1),
            ),
            fieldValue = fieldValue,
            onPreferredColumnChange = {},
            onFieldValueChange = { dispatchedFieldValue = it },
        )

        assertEquals(
            expected = TextFieldValue(
                text = "hello",
                selection = TextRange(2),
            ),
            actual = dispatchedFieldValue,
        )
        assertEquals(TextFieldValue(""), inputAnchorState.imeFieldValue)
        assertNull(inputAnchorState.anchorSelection)
    }
}
