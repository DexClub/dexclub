package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeEditorSelectionHandlesTest {
    @Test
    fun resolveHandleDragSelection_startHandleCanCrossEndHandle() {
        val selection = resolveHandleDragSelection(
            kind = SelectionHandleKind.Start,
            draggedTextOffset = 12,
            fixedTextOffset = 10,
        )

        assertEquals(TextRange(12, 10), selection)
        assertFalse(selection.collapsed)
    }

    @Test
    fun resolveHandleDragSelection_endHandleCanCrossStartHandle() {
        val selection = resolveHandleDragSelection(
            kind = SelectionHandleKind.End,
            draggedTextOffset = 3,
            fixedTextOffset = 5,
        )

        assertEquals(TextRange(5, 3), selection)
        assertFalse(selection.collapsed)
    }

    @Test
    fun shouldShowSelectionHandles_keepsHandlesVisibleWhileDragSessionIsActive() {
        assertTrue(
            shouldShowSelectionHandles(
                selection = TextRange(10),
                hasActiveRangeHandleSession = true,
            )
        )
        assertFalse(
            shouldShowSelectionHandles(
                selection = TextRange(10),
                hasActiveRangeHandleSession = false,
            )
        )
    }

    @Test
    fun resolveCursorHandleSelection_returnsCollapsedSelectionAtDraggedOffset() {
        val selection = resolveCursorHandleSelection(draggedTextOffset = 7)

        assertEquals(TextRange(7), selection)
        assertTrue(selection.collapsed)
    }

    @Test
    fun rangeHandleAutoScrollTarget_usesSameSelectionRulesAsRangeHandleDrag() {
        val target = RangeHandleAutoScrollTarget(
            kind = SelectionHandleKind.Start,
            fixedOffset = 10,
        )

        assertEquals(TextRange(12, 10), target.resolveSelection(draggedTextOffset = 12))
    }

    @Test
    fun cursorHandleAutoScrollTarget_returnsCollapsedSelection() {
        assertEquals(TextRange(9), CursorHandleAutoScrollTarget.resolveSelection(draggedTextOffset = 9))
    }

    @Test
    fun shouldShowCursorHandle_returnsTrueOnlyForCollapsedSelection() {
        assertTrue(shouldShowCursorHandle(TextRange(4)))
        assertFalse(shouldShowCursorHandle(TextRange(4, 6)))
    }
}
