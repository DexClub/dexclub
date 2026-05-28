package io.github.dexclub.codeview.compose

import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dexclub.codeview.compose.internal.editor.CodeEditorInputAnchorState
import io.github.dexclub.codeview.compose.internal.editor.deleteBackward
import io.github.dexclub.codeview.compose.internal.editor.deleteForward
import io.github.dexclub.codeview.compose.internal.editor.handleInputAnchorValueChange
import io.github.dexclub.codeview.compose.internal.editor.moveCaretHorizontally
import io.github.dexclub.codeview.compose.internal.editor.moveCaretTo
import io.github.dexclub.codeview.compose.internal.editor.normalizedCaretOffset
import io.github.dexclub.codeview.compose.internal.editor.normalizedStart
import io.github.dexclub.codeview.compose.internal.editor.replaceRange
import io.github.dexclub.codeview.compose.internal.editor.replaceSelection
import io.github.dexclub.codeview.compose.internal.editor.selectedText
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.InputMethodEvent
import java.awt.event.InputMethodListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.font.TextHitInfo
import java.awt.im.InputMethodRequests
import java.text.AttributedCharacterIterator
import java.text.AttributedString
import java.text.CharacterIterator
import javax.swing.JComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class DesktopInputHostComponent :
    JComponent(),
    InputMethodRequests,
    InputMethodListener {

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
    private var anchorBoundsInWindow: Rect = Rect.Zero

    init {
        // Keep the host as a real AWT input target for IME/caret events, but never let the
        // component itself become visually noticeable. Candidate positioning comes from
        // getTextLocation() rather than the component bounds.
        isFocusable = true
        isOpaque = false
        enableInputMethods(true)
        background = java.awt.Color(0, 0, 0, 0)
        preferredSize = Dimension(1, 1)
        minimumSize = preferredSize
        maximumSize = preferredSize
        setSize(preferredSize)
        setBounds(0, 0, 1, 1)

        addInputMethodListener(this)
        addKeyListener(DesktopInputHostKeyAdapter())
        addFocusListener(
            object : FocusAdapter() {
                override fun focusLost(e: FocusEvent) {
                    onInterruptInputAnchor?.invoke()
                }
            }
        )
    }

    override fun getInputMethodRequests(): InputMethodRequests = this

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
    }

    fun updateWindowBounds(boundsInWindow: Rect) {
        anchorBoundsInWindow = boundsInWindow
        if (x != 0 || y != 0 || width != 1 || height != 1) {
            setBounds(0, 0, 1, 1)
        }
    }

    override fun paintComponent(g: Graphics) {
    }

    override fun paintBorder(g: Graphics) {
    }

    override fun inputMethodTextChanged(event: InputMethodEvent) {
        val inputAnchorState = inputAnchorState ?: return
        val committedText = event.committedText
        val composingText = event.composingText
        val fullText = event.fullText

        when {
            fullText.isNotEmpty() && composingText.isNotEmpty() -> {
                updateComposingState(
                    inputAnchorState = inputAnchorState,
                    event = event,
                    fullText = fullText,
                )
            }

            committedText.isNotEmpty() -> {
                dispatchInputAnchorValueChange(
                    inputAnchorState = inputAnchorState,
                    newValue = TextFieldValue(
                        text = committedText,
                        selection = TextRange(committedText.length),
                    ),
                )
            }

            else -> {
                inputAnchorState.clear()
            }
        }

        event.consume()
    }

    override fun caretPositionChanged(event: InputMethodEvent) {
        val inputAnchorState = inputAnchorState ?: return
        val anchorSelection = inputAnchorState.anchorSelection ?: return
        val imeFieldValue = inputAnchorState.imeFieldValue
        if (imeFieldValue.composition == null || imeFieldValue.text.isEmpty()) {
            return
        }
        inputAnchorState.update(
            newValue = imeFieldValue.copy(
                selection = TextRange(
                    event.fullTextCaretInsertionIndex(
                        fullTextLength = imeFieldValue.text.length,
                    )
                ),
            ),
            anchorSelection = anchorSelection,
            consumedSelectionOnCompose = inputAnchorState.consumedSelectionOnCompose,
        )
        event.consume()
    }

    override fun getTextLocation(offset: TextHitInfo?): Rectangle {
        val safeBounds = anchorBoundsInWindow
        return try {
            val screenLocation = locationOnScreen
            Rectangle(
                screenLocation.x + safeBounds.left.toInt(),
                screenLocation.y + safeBounds.top.toInt(),
                safeBounds.width.toInt().coerceAtLeast(1),
                safeBounds.height.toInt().coerceAtLeast(1),
            )
        } catch (_: Exception) {
            Rectangle(
                safeBounds.left.toInt(),
                safeBounds.top.toInt(),
                safeBounds.width.toInt().coerceAtLeast(1),
                safeBounds.height.toInt().coerceAtLeast(1),
            )
        }
    }

    override fun getLocationOffset(x: Int, y: Int): TextHitInfo {
        return TextHitInfo.leading(currentSelectionOffset())
    }

    override fun getInsertPositionOffset(): Int {
        return currentSelectionOffset()
    }

    override fun getCommittedTextLength(): Int = 0

    override fun getCommittedText(
        beginIndex: Int,
        endIndex: Int,
        attributes: Array<AttributedCharacterIterator.Attribute>?,
    ): AttributedCharacterIterator {
        return AttributedString("").iterator
    }

    override fun cancelLatestCommittedText(
        attributes: Array<AttributedCharacterIterator.Attribute>?,
    ): AttributedCharacterIterator? = null

    override fun getSelectedText(
        attributes: Array<AttributedCharacterIterator.Attribute>?,
    ): AttributedCharacterIterator {
        val imeFieldValue = inputAnchorState?.imeFieldValue ?: TextFieldValue("")
        return AttributedString(imeFieldValue.selectedText()).iterator
    }

    private fun currentSelectionOffset(): Int {
        val imeFieldValue = inputAnchorState?.imeFieldValue ?: TextFieldValue("")
        return imeFieldValue.selection.end.coerceIn(0, imeFieldValue.text.length)
    }

    private fun updateComposingState(
        inputAnchorState: CodeEditorInputAnchorState,
        event: InputMethodEvent,
        fullText: String,
    ) {
        val effectiveAnchorSelection = inputAnchorState.anchorSelection ?: fieldValue.selection
        val shouldConsumeSelection =
            !effectiveAnchorSelection.collapsed && inputAnchorState.anchorSelection == null
        val anchorSelection = TextRange(effectiveAnchorSelection.normalizedStart)

        if (shouldConsumeSelection) {
            val nextValue = fieldValue.replaceRange(
                range = effectiveAnchorSelection,
                replacement = "",
            )
            dispatchDocumentValueChange(nextValue)
        }

        // Keep the full preedit payload in the overlay until the IME ends composition.
        inputAnchorState.update(
            newValue = TextFieldValue(
                text = fullText,
                selection = TextRange(
                    event.fullTextCaretInsertionIndex(
                        fullTextLength = fullText.length,
                    )
                ),
                composition = TextRange(
                    start = event.committedCharacterCount.coerceIn(0, fullText.length),
                    end = fullText.length,
                ),
            ),
            anchorSelection = anchorSelection,
            consumedSelectionOnCompose = shouldConsumeSelection || inputAnchorState.consumedSelectionOnCompose,
        )
    }

    private fun dispatchInputAnchorValueChange(
        inputAnchorState: CodeEditorInputAnchorState,
        newValue: TextFieldValue,
    ) {
        handleInputAnchorValueChange(
            inputAnchorState = inputAnchorState,
            newValue = newValue,
            fieldValue = fieldValue,
            onPreferredColumnChange = { onPreferredColumnChange?.invoke(it) },
            onFieldValueChange = { dispatchDocumentValueChange(it) },
        )
    }

    private fun dispatchDocumentValueChange(nextValue: TextFieldValue) {
        fieldValue = nextValue
        onFieldValueChange?.invoke(nextValue)
    }

    private fun dispatchFieldValueChange(
        nextValue: TextFieldValue,
        nextPreferredColumn: Int? = null,
    ) {
        dispatchDocumentValueChange(nextValue)
        onPreferredColumnChange?.invoke(nextPreferredColumn)
        onInterruptInputAnchor?.invoke()
    }

    private inner class DesktopInputHostKeyAdapter : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            val inputAnchorState = inputAnchorState ?: return
            val layoutSnapshot = layoutSnapshot ?: return
            val clipboard = clipboard ?: return
            val scope = scope ?: return

            if (inputAnchorState.imeFieldValue.composition != null) {
                return
            }

            val modifierHeld = isDesktopCommandModifierHeld(event)
            val extendSelection = event.isShiftDown

            when {
                modifierHeld && event.keyCode == KeyEvent.VK_A -> {
                    dispatchFieldValueChange(
                        nextValue = fieldValue.copy(
                            selection = TextRange(0, fieldValue.text.length),
                            composition = null,
                        ),
                    )
                    event.consume()
                }

                modifierHeld && event.keyCode == KeyEvent.VK_C -> {
                    val selectedText = fieldValue.selectedText()
                    if (selectedText.isNotEmpty()) {
                        scope.launch { clipboard.copyText(selectedText) }
                    }
                    event.consume()
                }

                modifierHeld && event.keyCode == KeyEvent.VK_V -> {
                    scope.launch {
                        val pastedText = clipboard.pasteText() ?: return@launch
                        if (pastedText.isEmpty()) return@launch
                        dispatchFieldValueChange(
                            nextValue = fieldValue.replaceSelection(pastedText),
                        )
                    }
                    event.consume()
                }

                modifierHeld && event.keyCode == KeyEvent.VK_F -> {
                    onFindRequested?.invoke(fieldValue.selectedText())
                    event.consume()
                }

                event.keyCode == KeyEvent.VK_BACK_SPACE -> {
                    dispatchFieldValueChange(fieldValue.deleteBackward())
                    event.consume()
                }

                event.keyCode == KeyEvent.VK_DELETE -> {
                    dispatchFieldValueChange(fieldValue.deleteForward())
                    event.consume()
                }

                event.keyCode == KeyEvent.VK_ENTER -> {
                    dispatchFieldValueChange(fieldValue.replaceSelection("\n"))
                    event.consume()
                }

                event.keyCode == KeyEvent.VK_TAB -> {
                    dispatchFieldValueChange(fieldValue.replaceSelection("\t"))
                    event.consume()
                }

                event.keyCode == KeyEvent.VK_ESCAPE && !fieldValue.selection.collapsed -> {
                    dispatchFieldValueChange(
                        nextValue = fieldValue.moveCaretTo(
                            targetOffset = fieldValue.normalizedCaretOffset(),
                            extendSelection = false,
                        ),
                    )
                    event.consume()
                }

                event.keyCode == KeyEvent.VK_LEFT -> {
                    dispatchFieldValueChange(
                        nextValue = moveCaretHorizontally(
                            fieldValue = fieldValue,
                            delta = -1,
                            extendSelection = extendSelection,
                        ),
                    )
                    event.consume()
                }

                event.keyCode == KeyEvent.VK_RIGHT -> {
                    dispatchFieldValueChange(
                        nextValue = moveCaretHorizontally(
                            fieldValue = fieldValue,
                            delta = 1,
                            extendSelection = extendSelection,
                        ),
                    )
                    event.consume()
                }

                event.keyCode == KeyEvent.VK_HOME -> {
                    val cursorPosition = layoutSnapshot.offsetToPosition(fieldValue.normalizedCaretOffset())
                    dispatchFieldValueChange(
                        nextValue = fieldValue.moveCaretTo(
                            targetOffset = layoutSnapshot.positionToOffset(cursorPosition.lineIndex, 0),
                            extendSelection = extendSelection,
                        ),
                    )
                    event.consume()
                }

                event.keyCode == KeyEvent.VK_END -> {
                    val cursorPosition = layoutSnapshot.offsetToPosition(fieldValue.normalizedCaretOffset())
                    dispatchFieldValueChange(
                        nextValue = fieldValue.moveCaretTo(
                            targetOffset = layoutSnapshot.positionToOffset(
                                cursorPosition.lineIndex,
                                layoutSnapshot.lineLength(cursorPosition.lineIndex),
                            ),
                            extendSelection = extendSelection,
                        ),
                    )
                    event.consume()
                }

                event.keyCode == KeyEvent.VK_UP || event.keyCode == KeyEvent.VK_DOWN -> {
                    val delta = if (event.keyCode == KeyEvent.VK_UP) -1 else 1
                    val cursorPosition = layoutSnapshot.offsetToPosition(fieldValue.normalizedCaretOffset())
                    val targetColumn = preferredColumn ?: cursorPosition.columnIndex
                    val targetLine = (cursorPosition.lineIndex + delta).coerceIn(0, layoutSnapshot.lineCount - 1)
                    dispatchFieldValueChange(
                        nextValue = fieldValue.moveCaretTo(
                            targetOffset = layoutSnapshot.positionToOffset(targetLine, targetColumn),
                            extendSelection = extendSelection,
                        ),
                        nextPreferredColumn = targetColumn,
                    )
                    event.consume()
                }
            }
        }

        override fun keyTyped(event: KeyEvent) {
            val inputAnchorState = inputAnchorState ?: return
            if (inputAnchorState.imeFieldValue.composition != null) {
                return
            }

            if (isDesktopCommandModifierHeld(event) || event.isAltDown) {
                return
            }

            val keyChar = event.keyChar
            if (keyChar == KeyEvent.CHAR_UNDEFINED || Character.isISOControl(keyChar)) {
                return
            }

            dispatchFieldValueChange(
                nextValue = fieldValue.replaceSelection(keyChar.toString()),
            )
            event.consume()
        }
    }
}

private fun InputMethodEvent.fullTextCaretInsertionIndex(fullTextLength: Int): Int {
    val compositionStart = committedCharacterCount.coerceIn(0, fullTextLength)
    val composedLength = (fullTextLength - compositionStart).coerceAtLeast(0)
    val caretInComposedText = caret?.insertionIndex?.coerceIn(0, composedLength) ?: composedLength
    return (compositionStart + caretInComposedText).coerceIn(0, fullTextLength)
}

private val InputMethodEvent.committedText: String
    get() = text.substringOrEmpty(0, committedCharacterCount)

private val InputMethodEvent.composingText: String
    get() = text.substringOrEmpty(committedCharacterCount, null)

private val InputMethodEvent.fullText: String
    get() = text.substringOrEmpty(0, null)

private fun AttributedCharacterIterator?.substringOrEmpty(
    start: Int,
    end: Int?,
): String {
    if (this == null) return ""
    return buildString {
        index = start
        var current = current()
        while (current != CharacterIterator.DONE && (end == null || index < end)) {
            append(current)
            current = next()
        }
    }
}

private val isMacOs: Boolean = System.getProperty("os.name")?.contains("Mac", ignoreCase = true) == true

private fun isDesktopCommandModifierHeld(event: KeyEvent): Boolean {
    return if (isMacOs) event.isMetaDown else event.isControlDown
}
