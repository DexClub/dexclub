package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dexclub.codeview.compose.copyText
import io.github.dexclub.codeview.compose.isModifierKeyHeld
import io.github.dexclub.codeview.compose.pasteText
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun Modifier.codeEditorKeyInput(
    fieldValue: TextFieldValue,
    layoutSnapshot: CodeLayoutSnapshot,
    clipboard: androidx.compose.ui.platform.Clipboard,
    scope: CoroutineScope,
    preferredColumn: Int?,
    onPreferredColumnChange: (Int?) -> Unit,
    onInterruptInputAnchor: () -> Unit,
    onFieldValueChange: (TextFieldValue) -> Unit,
): Modifier {
    return onKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

        val extendSelection = keyEvent.isShiftPressed
        val modifierHeld = isModifierKeyHeld(keyEvent)

        fun dispatch(nextValue: TextFieldValue, nextPreferredColumn: Int? = null): Boolean {
            if (fieldValue != nextValue) {
                onInterruptInputAnchor()
                onFieldValueChange(nextValue)
            }
            onPreferredColumnChange(nextPreferredColumn)
            return true
        }

        when {
            modifierHeld && keyEvent.key == Key.A -> {
                return@onKeyEvent dispatch(
                    nextValue = fieldValue.copy(
                        selection = TextRange(0, fieldValue.text.length),
                        composition = null,
                    ),
                )
            }

            modifierHeld && keyEvent.key == Key.C -> {
                val selectedText = fieldValue.selectedText()
                if (selectedText.isNotEmpty()) {
                    scope.launch { clipboard.copyText(selectedText) }
                }
                return@onKeyEvent true
            }

            modifierHeld && keyEvent.key == Key.V -> {
                scope.launch {
                    val pastedText = clipboard.pasteText() ?: return@launch
                    if (pastedText.isEmpty()) return@launch
                    onPreferredColumnChange(null)
                    onFieldValueChange(fieldValue.replaceSelection(pastedText))
                }
                return@onKeyEvent true
            }

            keyEvent.key == Key.Backspace -> {
                return@onKeyEvent dispatch(fieldValue.deleteBackward())
            }

            keyEvent.key == Key.Delete -> {
                return@onKeyEvent dispatch(fieldValue.deleteForward())
            }

            keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter -> {
                return@onKeyEvent dispatch(fieldValue.replaceSelection("\n"))
            }

            keyEvent.key == Key.Tab -> {
                return@onKeyEvent dispatch(fieldValue.replaceSelection("\t"))
            }

            keyEvent.key == Key.DirectionLeft -> {
                return@onKeyEvent dispatch(moveCaretHorizontally(fieldValue, -1, extendSelection))
            }

            keyEvent.key == Key.DirectionRight -> {
                return@onKeyEvent dispatch(moveCaretHorizontally(fieldValue, 1, extendSelection))
            }

            keyEvent.key == Key.MoveHome -> {
                val cursorOffset = fieldValue.normalizedCaretOffset()
                val cursorPosition = layoutSnapshot.offsetToPosition(cursorOffset)
                val targetOffset = layoutSnapshot.positionToOffset(cursorPosition.lineIndex, 0)
                return@onKeyEvent dispatch(
                    nextValue = fieldValue.moveCaretTo(targetOffset, extendSelection),
                )
            }

            keyEvent.key == Key.MoveEnd -> {
                val cursorOffset = fieldValue.normalizedCaretOffset()
                val cursorPosition = layoutSnapshot.offsetToPosition(cursorOffset)
                val targetOffset = layoutSnapshot.positionToOffset(
                    cursorPosition.lineIndex,
                    layoutSnapshot.lineLength(cursorPosition.lineIndex),
                )
                return@onKeyEvent dispatch(
                    nextValue = fieldValue.moveCaretTo(targetOffset, extendSelection),
                )
            }

            keyEvent.key == Key.DirectionUp || keyEvent.key == Key.DirectionDown -> {
                val delta = if (keyEvent.key == Key.DirectionUp) -1 else 1
                val cursorOffset = fieldValue.normalizedCaretOffset()
                val cursorPosition = layoutSnapshot.offsetToPosition(cursorOffset)
                val targetColumn = preferredColumn ?: cursorPosition.columnIndex
                val targetLine = (cursorPosition.lineIndex + delta).coerceIn(0, layoutSnapshot.lineCount - 1)
                val targetOffset = layoutSnapshot.positionToOffset(targetLine, targetColumn)
                return@onKeyEvent dispatch(
                    nextValue = fieldValue.moveCaretTo(targetOffset, extendSelection),
                    nextPreferredColumn = targetColumn,
                )
            }

            !modifierHeld &&
                    !keyEvent.key.isTypingModifier &&
                    keyEvent.utf16CodePoint in 0x20..0xD7FF -> {
                val typedChar = keyEvent.utf16CodePoint.toChar().toString()
                return@onKeyEvent dispatch(fieldValue.replaceSelection(typedChar))
            }

            else -> false
        }
    }
}

internal fun Modifier.codeEditorCommandKeyInput(
    fieldValue: TextFieldValue,
    layoutSnapshot: CodeLayoutSnapshot,
    clipboard: androidx.compose.ui.platform.Clipboard,
    scope: CoroutineScope,
    preferredColumn: Int?,
    onPreferredColumnChange: (Int?) -> Unit,
    onInterruptInputAnchor: () -> Unit,
    onFieldValueChange: (TextFieldValue) -> Unit,
): Modifier {
    return onPreviewKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

        val extendSelection = keyEvent.isShiftPressed
        val modifierHeld = isModifierKeyHeld(keyEvent)

        fun dispatch(nextValue: TextFieldValue, nextPreferredColumn: Int? = null): Boolean {
            if (fieldValue != nextValue) {
                onInterruptInputAnchor()
                onFieldValueChange(nextValue)
            }
            onPreferredColumnChange(nextPreferredColumn)
            return true
        }

        when {
            modifierHeld && keyEvent.key == Key.A -> {
                dispatch(
                    nextValue = fieldValue.copy(
                        selection = TextRange(0, fieldValue.text.length),
                        composition = null,
                    ),
                )
            }

            modifierHeld && keyEvent.key == Key.C -> {
                val selectedText = fieldValue.selectedText()
                if (selectedText.isNotEmpty()) {
                    scope.launch { clipboard.copyText(selectedText) }
                }
                true
            }

            modifierHeld && keyEvent.key == Key.V -> {
                scope.launch {
                    val pastedText = clipboard.pasteText() ?: return@launch
                    if (pastedText.isEmpty()) return@launch
                    onPreferredColumnChange(null)
                    onFieldValueChange(fieldValue.replaceSelection(pastedText))
                }
                true
            }

            keyEvent.key == Key.Backspace -> {
                dispatch(fieldValue.deleteBackward())
            }

            keyEvent.key == Key.Delete -> {
                dispatch(fieldValue.deleteForward())
            }

            keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter -> {
                dispatch(fieldValue.replaceSelection("\n"))
            }

            keyEvent.key == Key.Tab -> {
                dispatch(fieldValue.replaceSelection("\t"))
            }

            keyEvent.key == Key.Escape -> {
                if (fieldValue.selection.collapsed) {
                    false
                } else {
                    dispatch(fieldValue.moveCaretTo(fieldValue.normalizedCaretOffset(), extendSelection = false))
                }
            }

            keyEvent.key == Key.DirectionLeft -> {
                dispatch(moveCaretHorizontally(fieldValue, -1, extendSelection))
            }

            keyEvent.key == Key.DirectionRight -> {
                dispatch(moveCaretHorizontally(fieldValue, 1, extendSelection))
            }

            keyEvent.key == Key.MoveHome -> {
                val cursorOffset = fieldValue.normalizedCaretOffset()
                val cursorPosition = layoutSnapshot.offsetToPosition(cursorOffset)
                val targetOffset = layoutSnapshot.positionToOffset(cursorPosition.lineIndex, 0)
                dispatch(
                    nextValue = fieldValue.moveCaretTo(targetOffset, extendSelection),
                )
            }

            keyEvent.key == Key.MoveEnd -> {
                val cursorOffset = fieldValue.normalizedCaretOffset()
                val cursorPosition = layoutSnapshot.offsetToPosition(cursorOffset)
                val targetOffset = layoutSnapshot.positionToOffset(
                    cursorPosition.lineIndex,
                    layoutSnapshot.lineLength(cursorPosition.lineIndex),
                )
                dispatch(
                    nextValue = fieldValue.moveCaretTo(targetOffset, extendSelection),
                )
            }

            keyEvent.key == Key.DirectionUp || keyEvent.key == Key.DirectionDown -> {
                val delta = if (keyEvent.key == Key.DirectionUp) -1 else 1
                val cursorOffset = fieldValue.normalizedCaretOffset()
                val cursorPosition = layoutSnapshot.offsetToPosition(cursorOffset)
                val targetColumn = preferredColumn ?: cursorPosition.columnIndex
                val targetLine = (cursorPosition.lineIndex + delta).coerceIn(0, layoutSnapshot.lineCount - 1)
                val targetOffset = layoutSnapshot.positionToOffset(targetLine, targetColumn)
                dispatch(
                    nextValue = fieldValue.moveCaretTo(targetOffset, extendSelection),
                    nextPreferredColumn = targetColumn,
                )
            }

            else -> false
        }
    }
}

internal fun moveCaretHorizontally(
    fieldValue: TextFieldValue,
    delta: Int,
    extendSelection: Boolean,
): TextFieldValue {
    if (delta == 0) {
        return fieldValue.copy(composition = null)
    }
    if (!extendSelection && !fieldValue.selection.collapsed) {
        return when {
            delta < 0 -> fieldValue.moveCaretTo(fieldValue.selection.normalizedStart, false)
            else -> fieldValue.moveCaretTo(fieldValue.selection.normalizedEnd, false)
        }
    }

    val targetOffset = (fieldValue.normalizedCaretOffset() + delta)
        .coerceIn(0, fieldValue.text.length)
    return fieldValue.moveCaretTo(targetOffset, extendSelection)
}

internal val Key.isTypingModifier: Boolean
    get() = this == Key.ShiftLeft || this == Key.ShiftRight ||
            this == Key.AltLeft || this == Key.AltRight ||
            this == Key.CtrlLeft || this == Key.CtrlRight ||
            this == Key.MetaLeft || this == Key.MetaRight ||
            this == Key.CapsLock || this == Key.NumLock || this == Key.ScrollLock
