package io.github.dexclub.codeview.compose

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dexclub.codeview.compose.internal.editor.CodeEditorInputAnchorState
import io.github.dexclub.codeview.compose.internal.editor.deleteBackward
import io.github.dexclub.codeview.compose.internal.editor.deleteForward
import io.github.dexclub.codeview.compose.internal.editor.deleteSurroundingText
import io.github.dexclub.codeview.compose.internal.editor.handleInputAnchorValueChange
import io.github.dexclub.codeview.compose.internal.editor.moveCaretHorizontally
import io.github.dexclub.codeview.compose.internal.editor.moveCaretTo
import io.github.dexclub.codeview.compose.internal.editor.normalizedCaretOffset
import io.github.dexclub.codeview.compose.internal.editor.normalizedEnd
import io.github.dexclub.codeview.compose.internal.editor.normalizedStart
import io.github.dexclub.codeview.compose.internal.editor.replaceSelection
import io.github.dexclub.codeview.compose.internal.editor.selectAll
import io.github.dexclub.codeview.compose.internal.editor.selectedText
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class AndroidInputHostView(
    context: Context,
) : View(context) {
    private val inputMethodManager: InputMethodManager? =
        context.getSystemService(InputMethodManager::class.java)

    private var inputAnchorState: CodeEditorInputAnchorState? = null
    private var fieldValue: TextFieldValue = TextFieldValue("")
    private var layoutSnapshot: CodeLayoutSnapshot? = null
    private var clipboard: Clipboard? = null
    private var scope: CoroutineScope? = null
    private var preferredColumn: Int? = null
    private var onPreferredColumnChange: ((Int?) -> Unit)? = null
    private var onInterruptInputAnchor: (() -> Unit)? = null
    private var onFindRequested: ((String) -> Unit)? = null
    private var onFieldValueChange: ((TextFieldValue) -> Unit)? = null
    private var lastPublishedSnapshot: AndroidEditingSnapshot? = null
    private val selectionState = AndroidInputSelectionState()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = false
        alpha = 0f
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
    }

    fun updateSession(
        inputAnchorState: CodeEditorInputAnchorState,
        fieldValue: TextFieldValue,
        layoutSnapshot: CodeLayoutSnapshot,
        clipboard: Clipboard,
        scope: CoroutineScope,
        preferredColumn: Int?,
        onPreferredColumnChange: (Int?) -> Unit,
        onInterruptInputAnchor: () -> Unit,
        onFindRequested: (String) -> Unit,
        onFieldValueChange: (TextFieldValue) -> Unit,
    ) {
        val previousFieldValue = this.fieldValue
        this.inputAnchorState = inputAnchorState
        this.fieldValue = fieldValue
        this.layoutSnapshot = layoutSnapshot
        this.clipboard = clipboard
        this.scope = scope
        this.preferredColumn = preferredColumn
        this.onPreferredColumnChange = onPreferredColumnChange
        this.onInterruptInputAnchor = onInterruptInputAnchor
        this.onFindRequested = onFindRequested
        this.onFieldValueChange = onFieldValueChange

        val textChangedExternally = previousFieldValue.text != fieldValue.text
        publishSnapshotToIme(restartOnTextChange = textChangedExternally)
    }

    fun requestEditorFocus() {
        val requestedFocus = !isFocused
        if (requestedFocus) {
            requestFocus()
        }
        post {
            val imm = inputMethodManager ?: return@post
            val shouldShowKeyboard =
                requestedFocus ||
                        !imm.isActive(this) ||
                        !imm.isAcceptingText ||
                        !isSoftwareKeyboardVisible()
            if (shouldShowKeyboard) {
                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    fun isSoftwareKeyboardVisible(): Boolean {
        return isImeVisible()
    }

    override fun onCheckIsTextEditor(): Boolean = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        val snapshot = currentEditingSnapshot()
        outAttrs.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        outAttrs.imeOptions =
            EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                    EditorInfo.IME_FLAG_NO_FULLSCREEN
        outAttrs.initialSelStart = snapshot.selection.start
        outAttrs.initialSelEnd = snapshot.selection.end

        lastPublishedSnapshot = snapshot
        return CodeEditorInputConnection()
    }

    override fun onFocusChanged(
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: android.graphics.Rect?,
    ) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused) {
            publishSnapshotToIme(restartOnTextChange = true)
        } else {
            onInterruptInputAnchor?.invoke()
            inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
            lastPublishedSnapshot = currentEditingSnapshot()
        }
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (handleHardwareKeyDown(event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleHardwareKeyDown(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (inputAnchorState?.imeFieldValue?.composition != null) return false

        val layoutSnapshot = layoutSnapshot ?: return false
        val modifierHeld = event.isCtrlPressed
        val extendSelection = selectionState.shouldExtendSelection(event.isShiftPressed)

        fun dispatch(
            nextValue: TextFieldValue,
            nextPreferredColumn: Int? = null,
        ): Boolean {
            if (fieldValue != nextValue) {
                dispatchFieldValueChange(nextValue, nextPreferredColumn)
            } else {
                updatePreferredColumn(nextPreferredColumn)
            }
            return true
        }

        return when {
            modifierHeld && event.keyCode == KeyEvent.KEYCODE_A -> {
                dispatch(fieldValue.selectAll())
            }

            modifierHeld && event.keyCode == KeyEvent.KEYCODE_C -> {
                val selectedText = fieldValue.selectedText()
                if (selectedText.isNotEmpty()) {
                    scope?.launch { clipboard?.copyText(selectedText) }
                }
                true
            }

            modifierHeld && event.keyCode == KeyEvent.KEYCODE_V -> {
                scope?.launch {
                    val pastedText = clipboard?.pasteText() ?: return@launch
                    if (pastedText.isEmpty()) return@launch
                    selectionState.clear()
                    dispatchFieldValueChange(
                        nextValue = fieldValue.replaceSelection(pastedText),
                    )
                }
                true
            }

            modifierHeld && event.keyCode == KeyEvent.KEYCODE_F -> {
                onFindRequested?.invoke(fieldValue.selectedText())
                true
            }

            event.keyCode == KeyEvent.KEYCODE_DEL -> {
                selectionState.clear()
                dispatch(fieldValue.deleteBackward())
            }

            event.keyCode == KeyEvent.KEYCODE_FORWARD_DEL -> {
                selectionState.clear()
                dispatch(fieldValue.deleteForward())
            }

            event.keyCode == KeyEvent.KEYCODE_ENTER -> {
                selectionState.clear()
                dispatch(fieldValue.replaceSelection("\n"))
            }

            event.keyCode == KeyEvent.KEYCODE_TAB -> {
                selectionState.clear()
                dispatch(fieldValue.replaceSelection("\t"))
            }

            event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT -> {
                dispatch(moveCaretHorizontally(fieldValue, -1, extendSelection))
            }

            event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT -> {
                dispatch(moveCaretHorizontally(fieldValue, 1, extendSelection))
            }

            event.keyCode == KeyEvent.KEYCODE_MOVE_HOME -> {
                val cursorOffset = fieldValue.normalizedCaretOffset()
                val cursorPosition = layoutSnapshot.offsetToPosition(cursorOffset)
                dispatch(
                    nextValue = fieldValue.moveCaretTo(
                        targetOffset = layoutSnapshot.positionToOffset(cursorPosition.lineIndex, 0),
                        extendSelection = extendSelection,
                    ),
                )
            }

            event.keyCode == KeyEvent.KEYCODE_MOVE_END -> {
                val cursorOffset = fieldValue.normalizedCaretOffset()
                val cursorPosition = layoutSnapshot.offsetToPosition(cursorOffset)
                dispatch(
                    nextValue = fieldValue.moveCaretTo(
                        targetOffset = layoutSnapshot.positionToOffset(
                            cursorPosition.lineIndex,
                            layoutSnapshot.lineLength(cursorPosition.lineIndex),
                        ),
                        extendSelection = extendSelection,
                    ),
                )
            }

            event.keyCode == KeyEvent.KEYCODE_DPAD_UP || event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
                val delta = if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) -1 else 1
                val cursorOffset = fieldValue.normalizedCaretOffset()
                val cursorPosition = layoutSnapshot.offsetToPosition(cursorOffset)
                val targetColumn = preferredColumn ?: cursorPosition.columnIndex
                val targetLine = (cursorPosition.lineIndex + delta).coerceIn(0, layoutSnapshot.lineCount - 1)
                dispatch(
                    nextValue = fieldValue.moveCaretTo(
                        targetOffset = layoutSnapshot.positionToOffset(targetLine, targetColumn),
                        extendSelection = extendSelection,
                    ),
                    nextPreferredColumn = targetColumn,
                )
            }

            !modifierHeld && !event.isAltPressed && event.unicodeChar in 0x20..0xD7FF -> {
                val typedChar = event.unicodeChar.toChar().toString()
                selectionState.clear()
                dispatch(fieldValue.replaceSelection(typedChar))
            }

            else -> false
        }
    }

    private fun updatePreferredColumn(nextPreferredColumn: Int?) {
        preferredColumn = nextPreferredColumn
        onPreferredColumnChange?.invoke(nextPreferredColumn)
    }

    private fun dispatchDocumentValueChange(nextValue: TextFieldValue) {
        fieldValue = nextValue
        onFieldValueChange?.invoke(nextValue)
        publishLocalSnapshotToIme()
    }

    private fun dispatchFieldValueChange(
        nextValue: TextFieldValue,
        nextPreferredColumn: Int? = null,
    ) {
        fieldValue = nextValue
        onFieldValueChange?.invoke(nextValue)
        updatePreferredColumn(nextPreferredColumn)
        onInterruptInputAnchor?.invoke()
        publishLocalSnapshotToIme()
    }

    private fun dispatchInputAnchorValueChange(newValue: TextFieldValue): Boolean {
        val inputAnchorState = inputAnchorState ?: return false
        if (newValue.text.isNotEmpty() || newValue.composition != null) {
            selectionState.clear()
        }
        handleInputAnchorValueChange(
            inputAnchorState = inputAnchorState,
            newValue = newValue,
            fieldValue = fieldValue,
            onPreferredColumnChange = ::updatePreferredColumn,
            onFieldValueChange = ::dispatchDocumentValueChange,
        )
        publishLocalSnapshotToIme()
        return true
    }

    private fun updateImeSelection(newSelection: TextRange): Boolean {
        val inputAnchorState = inputAnchorState ?: return false
        val imeFieldValue = inputAnchorState.imeFieldValue
        val anchorSelection = inputAnchorState.anchorSelection ?: TextRange(fieldValue.selection.normalizedStart)
        // Keep IME-local selection relative to the anchor where the preedit text was injected.
        inputAnchorState.update(
            newValue = imeFieldValue.copy(
                selection = clampTextRange(
                    range = newSelection,
                    textLength = imeFieldValue.text.length,
                ),
                composition = imeFieldValue.composition?.let { composition ->
                    clampTextRange(
                        range = composition,
                        textLength = imeFieldValue.text.length,
                    )
                },
            ),
            anchorSelection = anchorSelection,
            consumedSelectionOnCompose = inputAnchorState.consumedSelectionOnCompose,
        )
        publishLocalSnapshotToIme()
        return true
    }

    private fun currentEditingSnapshot(): AndroidEditingSnapshot {
        val inputAnchorState = inputAnchorState
        val imeFieldValue = inputAnchorState?.imeFieldValue
        val anchorSelection = inputAnchorState?.anchorSelection

        if (imeFieldValue == null || imeFieldValue.text.isEmpty()) {
            return AndroidEditingSnapshot(
                text = fieldValue.text,
                selection = clampTextRange(
                    range = fieldValue.selection,
                    textLength = fieldValue.text.length,
                ),
                composition = null,
                insertedImeRange = null,
            )
        }

        // The Android input host inserts preedit text at the normalized anchor boundary and
        // projects IME-local selection/composition back into document offsets from there.
        val anchorStart = (anchorSelection ?: fieldValue.selection).normalizedStart
            .coerceIn(0, fieldValue.text.length)
        val insertedText = imeFieldValue.text
        val effectiveText = fieldValue.text.replaceRange(anchorStart, anchorStart, insertedText)
        val insertedRange = TextRange(anchorStart, anchorStart + insertedText.length)
        val effectiveSelection = TextRange(
            start = anchorStart + imeFieldValue.selection.start.coerceIn(0, insertedText.length),
            end = anchorStart + imeFieldValue.selection.end.coerceIn(0, insertedText.length),
        )
        val effectiveComposition = imeFieldValue.composition?.let { composition ->
            TextRange(
                start = anchorStart + composition.start.coerceIn(0, insertedText.length),
                end = anchorStart + composition.end.coerceIn(0, insertedText.length),
            )
        }

        return AndroidEditingSnapshot(
            text = effectiveText,
            selection = clampTextRange(
                range = effectiveSelection,
                textLength = effectiveText.length,
            ),
            composition = effectiveComposition?.let { composition ->
                clampTextRange(
                    range = composition,
                    textLength = effectiveText.length,
                )
            },
            insertedImeRange = insertedRange,
        )
    }

    private fun publishSnapshotToIme(restartOnTextChange: Boolean) {
        val snapshot = currentEditingSnapshot()
        if (!hasFocus()) {
            lastPublishedSnapshot = snapshot
            return
        }

        val inputMethodManager = inputMethodManager ?: return
        val previousSnapshot = lastPublishedSnapshot
        val shouldRestart = restartOnTextChange && previousSnapshot != null && previousSnapshot.text != snapshot.text
        if (shouldRestart) {
            inputMethodManager.restartInput(this)
        }
        inputMethodManager.updateSelection(
            this,
            snapshot.selection.start,
            snapshot.selection.end,
            snapshot.composition?.start ?: -1,
            snapshot.composition?.end ?: -1,
        )
        lastPublishedSnapshot = snapshot
    }

    private fun publishLocalSnapshotToIme() {
        publishSnapshotToIme(restartOnTextChange = false)
    }

    private inner class CodeEditorInputConnection :
        BaseInputConnection(this@AndroidInputHostView, true) {

        override fun getTextBeforeCursor(
            n: Int,
            flags: Int,
        ): CharSequence {
            val snapshot = currentEditingSnapshot()
            val end = snapshot.selection.end.coerceIn(0, snapshot.text.length)
            val start = (end - n).coerceAtLeast(0)
            return snapshot.text.substring(start, end)
        }

        override fun getTextAfterCursor(
            n: Int,
            flags: Int,
        ): CharSequence {
            val snapshot = currentEditingSnapshot()
            val start = snapshot.selection.end.coerceIn(0, snapshot.text.length)
            val end = (start + n).coerceAtMost(snapshot.text.length)
            return snapshot.text.substring(start, end)
        }

        override fun getSelectedText(flags: Int): CharSequence {
            val snapshot = currentEditingSnapshot()
            val start = snapshot.selection.normalizedStart
            val end = snapshot.selection.normalizedEnd
            if (start >= end) return ""
            return snapshot.text.substring(start, end)
        }

        override fun getExtractedText(
            request: ExtractedTextRequest?,
            flags: Int,
        ): ExtractedText {
            val snapshot = currentEditingSnapshot()
            return ExtractedText().apply {
                text = snapshot.text
                startOffset = 0
                partialStartOffset = -1
                partialEndOffset = -1
                selectionStart = snapshot.selection.start
                selectionEnd = snapshot.selection.end
            }
        }

        override fun setComposingText(
            text: CharSequence?,
            newCursorPosition: Int,
        ): Boolean {
            val content = text?.toString().orEmpty()
            if (content.isEmpty()) {
                if (deleteSelectionIfNeeded()) {
                    return true
                }
                return dispatchInputAnchorValueChange(TextFieldValue(""))
            }
            return dispatchInputAnchorValueChange(
                TextFieldValue(
                    text = content,
                    selection = resolveInsertedTextSelection(content.length, newCursorPosition),
                    composition = TextRange(0, content.length),
                )
            )
        }

        override fun commitText(
            text: CharSequence?,
            newCursorPosition: Int,
        ): Boolean {
            val content = text?.toString().orEmpty()
            if (content.isEmpty()) {
                if (deleteSelectionIfNeeded()) {
                    return true
                }
                return dispatchInputAnchorValueChange(TextFieldValue(""))
            }
            return dispatchInputAnchorValueChange(
                TextFieldValue(
                    text = content,
                    selection = resolveInsertedTextSelection(content.length, newCursorPosition),
                )
            )
        }

        override fun finishComposingText(): Boolean {
            val imeFieldValue = inputAnchorState?.imeFieldValue ?: return true
            if (imeFieldValue.text.isEmpty()) return true
            return dispatchInputAnchorValueChange(
                imeFieldValue.copy(
                    composition = null,
                )
            )
        }

        override fun setComposingRegion(
            start: Int,
            end: Int,
        ): Boolean {
            val snapshot = currentEditingSnapshot()
            val insertedRange = snapshot.insertedImeRange ?: return false
            val composition = TextRange(
                start = (start - insertedRange.start).coerceIn(0, insertedRange.length),
                end = (end - insertedRange.start).coerceIn(0, insertedRange.length),
            )
            val imeFieldValue = inputAnchorState?.imeFieldValue ?: return false
            val anchorSelection = inputAnchorState?.anchorSelection ?: TextRange(fieldValue.selection.normalizedStart)
            inputAnchorState?.update(
                newValue = imeFieldValue.copy(
                    composition = clampTextRange(
                        range = composition,
                        textLength = imeFieldValue.text.length,
                    )
                ),
                anchorSelection = anchorSelection,
                consumedSelectionOnCompose = inputAnchorState?.consumedSelectionOnCompose == true,
            )
            publishLocalSnapshotToIme()
            return true
        }

        override fun setSelection(
            start: Int,
            end: Int,
        ): Boolean {
            val snapshot = currentEditingSnapshot()
            val safeSelection = clampTextRange(
                range = TextRange(start, end),
                textLength = snapshot.text.length,
            )
            val insertedRange = snapshot.insertedImeRange

            return if (
                insertedRange != null &&
                safeSelection.start in insertedRange.start..insertedRange.end &&
                safeSelection.end in insertedRange.start..insertedRange.end
            ) {
                updateImeSelection(
                    TextRange(
                        start = safeSelection.start - insertedRange.start,
                        end = safeSelection.end - insertedRange.start,
                    )
                )
            } else {
                val mappedStart = effectiveOffsetToDocumentOffset(safeSelection.start, insertedRange)
                val mappedEnd = effectiveOffsetToDocumentOffset(safeSelection.end, insertedRange)
                // Route all document-level selection changes through AndroidInputSelectionState so
                // IME-specific collapse quirks can be normalized in one place.
                val nextSelection = selectionState.resolveSelection(
                    fieldValue = fieldValue,
                    mappedStart = mappedStart,
                    mappedEnd = mappedEnd,
                )
                selectionState.onSelectionApplied(nextSelection)
                dispatchFieldValueChange(
                    nextValue = fieldValue.copy(
                        selection = nextSelection,
                        composition = null,
                    ),
                )
                true
            }
        }

        override fun deleteSurroundingText(
            beforeLength: Int,
            afterLength: Int,
        ): Boolean {
            if (beforeLength <= 0 && afterLength <= 0) return true

            val imeFieldValue = inputAnchorState?.imeFieldValue
            if (imeFieldValue != null && imeFieldValue.text.isNotEmpty()) {
                val nextValue = imeFieldValue
                    .deleteSurroundingText(beforeLength, afterLength)
                    .let { updated ->
                        when {
                            updated.text.isEmpty() -> updated
                            imeFieldValue.composition != null -> updated.copy(
                                composition = TextRange(0, updated.text.length),
                            )

                            else -> updated
                        }
                    }
                return dispatchInputAnchorValueChange(nextValue)
            }

            dispatchFieldValueChange(
                nextValue = fieldValue.deleteSurroundingText(beforeLength, afterLength),
            )
            return true
        }

        override fun deleteSurroundingTextInCodePoints(
            beforeLength: Int,
            afterLength: Int,
        ): Boolean {
            return deleteSurroundingText(beforeLength, afterLength)
        }

        override fun performContextMenuAction(id: Int): Boolean {
            return when (id) {
                android.R.id.startSelectingText -> {
                    selectionState.activateSelectionAction(fieldValue)
                    true
                }

                android.R.id.stopSelectingText -> {
                    val collapseOffset = selectionState.deactivateSelectionAction(fieldValue)
                    if (!fieldValue.selection.collapsed) {
                        dispatchFieldValueChange(
                            nextValue = fieldValue.copy(
                                selection = TextRange(collapseOffset),
                                composition = null,
                            ),
                        )
                    }
                    true
                }

                android.R.id.selectAll -> {
                    selectionState.clear()
                    dispatchFieldValueChange(fieldValue.selectAll())
                    true
                }

                android.R.id.cut -> {
                    val selectedText = fieldValue.selectedText()
                    if (selectedText.isNotEmpty()) {
                        selectionState.clear()
                        scope?.launch { clipboard?.copyText(selectedText, label = "code_cut") }
                        dispatchFieldValueChange(fieldValue.replaceSelection(""))
                    }
                    true
                }

                android.R.id.copy -> {
                    val selectedText = fieldValue.selectedText()
                    if (selectedText.isNotEmpty()) {
                        scope?.launch { clipboard?.copyText(selectedText, label = "code_copy") }
                    }
                    true
                }

                android.R.id.paste -> {
                    scope?.launch {
                        val pastedText = clipboard?.pasteText() ?: return@launch
                        if (pastedText.isEmpty()) return@launch
                        selectionState.clear()
                        dispatchFieldValueChange(fieldValue.replaceSelection(pastedText))
                    }
                    true
                }

                else -> super.performContextMenuAction(id)
            }
        }

        override fun sendKeyEvent(event: KeyEvent): Boolean {
            if (selectionState.handleSoftKeyboardShiftKey(event, fieldValue)) {
                return true
            }
            return handleHardwareKeyDown(event) || super.sendKeyEvent(event)
        }

    }

    private fun deleteSelectionIfNeeded(): Boolean {
        if (fieldValue.selection.collapsed) {
            return false
        }
        selectionState.clear()
        dispatchFieldValueChange(
            nextValue = fieldValue.replaceSelection(""),
        )
        return true
    }

    private fun isImeVisible(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = rootWindowInsets ?: return false
            return insets.isVisible(WindowInsets.Type.ime())
        }

        val root = rootView ?: return false
        if (root.height <= 0) return false

        val visibleFrame = Rect()
        root.getWindowVisibleDisplayFrame(visibleFrame)
        val obscuredHeight = (root.height - visibleFrame.height()).coerceAtLeast(0)
        return obscuredHeight > root.height * IME_VISIBLE_THRESHOLD_RATIO
    }
}

private const val IME_VISIBLE_THRESHOLD_RATIO: Float = 0.15f
