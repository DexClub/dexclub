package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CodeEditorTouchInteractionStateTest {
    @Test
    fun idleState_showsToolbarAndHandlesWithoutAutoScrollSession() {
        val state = TouchSelectionInteractionState.Idle

        assertTrue(state.showSelectionToolbar)
        assertTrue(state.showSelectionHandles)
        assertNull(state.autoScrollSession)
    }

    @Test
    fun longPressSelecting_hidesToolbarAndHandles() {
        val session = LongPressTouchSelectionAutoScrollSession(
            initialSelection = TextRange(3, 7),
            initialViewportPosition = Offset.Zero,
        )
        val state = TouchSelectionInteractionState.LongPressSelecting(
            autoScrollSession = session,
        )

        assertFalse(state.showSelectionToolbar)
        assertFalse(state.showSelectionHandles)
        assertTrue(state.autoScrollSession === session)
    }

    @Test
    fun draggingHandle_hidesToolbarButKeepsHandlesVisible() {
        val session = HandleTouchSelectionAutoScrollSession(
            target = CursorHandleAutoScrollTarget,
            initialViewportPosition = Offset.Zero,
        )
        val state = TouchSelectionInteractionState.DraggingHandle(
            autoScrollSession = session,
        )

        assertFalse(state.showSelectionToolbar)
        assertTrue(state.showSelectionHandles)
        assertTrue(state.autoScrollSession === session)
    }
}
