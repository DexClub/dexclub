package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshotFactory
import io.github.dexclub.codeview.core.text.CodeSelection
import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeEditorStateMappingTest {
    @Test
    fun resolveSynchronizedFieldValue_keepsLocalCaretWhenTextRefreshUsesStaleExternalSelection() {
        val result = resolveSynchronizedFieldValue(
            snapshotText = "hello!",
            fieldValue = TextFieldValue(
                text = "hello!",
                selection = TextRange(6),
            ),
            externalSelection = CodeSelection.collapsed(1),
            readOnly = false,
            syncExternalSelection = false,
        )

        assertEquals(
            expected = TextFieldValue(
                text = "hello!",
                selection = TextRange(6),
            ),
            actual = result,
        )
    }

    @Test
    fun resolveSynchronizedFieldValue_appliesExternalSelectionWhenCallerRequestsResync() {
        val result = resolveSynchronizedFieldValue(
            snapshotText = "hello!",
            fieldValue = TextFieldValue(
                text = "hello!",
                selection = TextRange(6),
            ),
            externalSelection = CodeSelection.collapsed(1),
            readOnly = false,
            syncExternalSelection = true,
        )

        assertEquals(
            expected = TextFieldValue(
                text = "hello!",
                selection = TextRange(1),
            ),
            actual = result,
        )
    }

    @Test
    fun shouldSyncExternalSelection_returnsFalseWhenRawExternalInputHasNotChanged() {
        val input = ExternalSelectionSyncInput(
            selection = LineSelection.collapsed(line = 10, offset = 5),
            cursor = Cursor(line = 10, offset = 5),
        )

        assertFalse(
            shouldSyncExternalSelection(
                readOnly = false,
                currentInput = input,
                previousInput = input.copy(),
            )
        )
    }

    @Test
    fun shouldSyncExternalSelection_returnsTrueWhenRawExternalCursorChanges() {
        val previousInput = ExternalSelectionSyncInput(
            selection = LineSelection.collapsed(line = 10, offset = 5),
            cursor = Cursor(line = 10, offset = 5),
        )
        val currentInput = previousInput.copy(
            cursor = Cursor(line = 12, offset = 1),
        )

        assertTrue(
            shouldSyncExternalSelection(
                readOnly = false,
                currentInput = currentInput,
                previousInput = previousInput,
            )
        )
    }

    @Test
    fun sameRawExternalCursor_canMapToDifferentOffsetsWithoutTriggeringResync() {
        val cursor = Cursor(line = 1, offset = 2)
        val previousInput = ExternalSelectionSyncInput(
            selection = null,
            cursor = cursor,
        )
        val currentInput = ExternalSelectionSyncInput(
            selection = null,
            cursor = cursor,
        )
        val beforeLayout = CodeLayoutSnapshotFactory.create(
            text = "abc\nxy",
        )
        val afterLayout = CodeLayoutSnapshotFactory.create(
            text = "abcZ\nxy",
        )

        val beforeExternalSelection = resolveExternalSelection(
            layoutSnapshot = beforeLayout,
            selection = null,
            cursor = cursor,
        )
        val afterExternalSelection = resolveExternalSelection(
            layoutSnapshot = afterLayout,
            selection = null,
            cursor = cursor,
        )

        assertTrue(beforeExternalSelection != afterExternalSelection)
        assertFalse(
            shouldSyncExternalSelection(
                readOnly = false,
                currentInput = currentInput,
                previousInput = previousInput,
            )
        )
    }
}
