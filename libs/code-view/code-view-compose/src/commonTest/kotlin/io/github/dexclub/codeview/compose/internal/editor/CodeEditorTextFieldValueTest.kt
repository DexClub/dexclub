package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeEditorTextFieldValueTest {
    @Test
    fun deleteSurroundingText_deletesAroundCollapsedCaret() {
        val result = TextFieldValue(
            text = "abcdef",
            selection = TextRange(3),
        ).deleteSurroundingText(
            beforeLength = 2,
            afterLength = 1,
        )

        assertEquals(
            expected = TextFieldValue(
                text = "aef",
                selection = TextRange(1),
            ),
            actual = result,
        )
    }

    @Test
    fun deleteSurroundingText_deletesSelectionWhenRangeExists() {
        val result = TextFieldValue(
            text = "abcdef",
            selection = TextRange(2, 5),
        ).deleteSurroundingText(
            beforeLength = 1,
            afterLength = 1,
        )

        assertEquals(
            expected = TextFieldValue(
                text = "abf",
                selection = TextRange(2),
            ),
            actual = result,
        )
    }

    @Test
    fun selectAll_selectsWholeTextAndClearsComposition() {
        val result = TextFieldValue(
            text = "abcdef",
            selection = TextRange(3),
            composition = TextRange(1, 4),
        ).selectAll()

        assertEquals(
            expected = TextFieldValue(
                text = "abcdef",
                selection = TextRange(0, 6),
            ),
            actual = result,
        )
    }

    @Test
    fun moveCaretHorizontally_supportsMultiStepDelta() {
        val result = moveCaretHorizontally(
            fieldValue = TextFieldValue(
                text = "abcdef",
                selection = TextRange(2),
            ),
            delta = 3,
            extendSelection = false,
        )

        assertEquals(
            expected = TextFieldValue(
                text = "abcdef",
                selection = TextRange(5),
            ),
            actual = result,
        )
    }

    @Test
    fun moveCaretHorizontally_collapsesExpandedSelectionToVisualEndBeforeMoving() {
        val result = moveCaretHorizontally(
            fieldValue = TextFieldValue(
                text = "abcdef",
                selection = TextRange(5, 2),
            ),
            delta = 1,
            extendSelection = false,
        )

        assertEquals(
            expected = TextFieldValue(
                text = "abcdef",
                selection = TextRange(5),
            ),
            actual = result,
        )
    }
}
