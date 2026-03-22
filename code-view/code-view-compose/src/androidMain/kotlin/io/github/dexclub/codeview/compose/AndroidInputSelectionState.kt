package io.github.dexclub.codeview.compose

import android.view.KeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dexclub.codeview.compose.internal.editor.CodeEditorImeSelectionState

internal class AndroidInputSelectionState {
    private val state = CodeEditorImeSelectionState()

    fun shouldExtendSelection(isShiftPressed: Boolean): Boolean {
        return state.shouldExtendSelection(isShiftPressed)
    }

    fun activateSelectionAction(fieldValue: TextFieldValue) {
        state.activateSelectionAction(fieldValue)
    }

    fun deactivateSelectionAction(fieldValue: TextFieldValue): Int {
        return state.deactivateSelectionAction(fieldValue)
    }

    fun clear() {
        state.clear()
    }

    fun resolveSelection(
        fieldValue: TextFieldValue,
        mappedStart: Int,
        mappedEnd: Int,
    ): TextRange {
        return state.resolveSelection(
            fieldValue = fieldValue,
            mappedStart = mappedStart,
            mappedEnd = mappedEnd,
        )
    }

    fun onSelectionApplied(nextSelection: TextRange) {
        state.onSelectionApplied(nextSelection)
    }

    fun handleSoftKeyboardShiftKey(
        event: KeyEvent,
        fieldValue: TextFieldValue,
    ): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_SHIFT_LEFT && event.keyCode != KeyEvent.KEYCODE_SHIFT_RIGHT) {
            return false
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> state.updateSoftKeyboardShiftState(
                active = true,
                fieldValue = fieldValue,
            )

            KeyEvent.ACTION_UP -> state.updateSoftKeyboardShiftState(
                active = false,
                fieldValue = fieldValue,
            )
        }
        return true
    }
}
