package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal class CodeEditorInputAnchorState {
    // imeFieldValue mirrors the platform preedit session only. The real document text stays in
    // CodeEditor state and is updated separately when composition commits.
    var imeFieldValue by mutableStateOf(TextFieldValue(""))
        private set
    var anchorSelection by mutableStateOf<TextRange?>(null)
        private set
    var consumedSelectionOnCompose by mutableStateOf(false)
        private set

    fun update(
        newValue: TextFieldValue,
        anchorSelection: TextRange,
        consumedSelectionOnCompose: Boolean,
    ) {
        imeFieldValue = newValue
        this.anchorSelection = anchorSelection
        this.consumedSelectionOnCompose = consumedSelectionOnCompose
    }

    fun clear() {
        if (
            imeFieldValue.text.isEmpty() &&
            imeFieldValue.composition == null &&
            imeFieldValue.selection == TextRange.Zero &&
            anchorSelection == null
        ) {
            return
        }
        imeFieldValue = TextFieldValue("")
        anchorSelection = null
        consumedSelectionOnCompose = false
    }
}

@Composable
internal fun rememberCodeEditorInputAnchorState(documentKey: Any): CodeEditorInputAnchorState {
    return remember(documentKey) { CodeEditorInputAnchorState() }
}

internal fun handleInputAnchorValueChange(
    inputAnchorState: CodeEditorInputAnchorState,
    newValue: TextFieldValue,
    fieldValue: TextFieldValue,
    onPreferredColumnChange: (Int?) -> Unit,
    onFieldValueChange: (TextFieldValue) -> Unit,
) {
    val previousImeFieldValue = inputAnchorState.imeFieldValue
    val effectiveAnchorSelection = inputAnchorState.anchorSelection ?: fieldValue.selection
    when {
        previousImeFieldValue.text.isNotEmpty() &&
                inputAnchorState.anchorSelection != null &&
                previousImeFieldValue.text == newValue.text &&
                previousImeFieldValue.selection != newValue.selection -> {
            // Some Android IMEs update only the selection inside an existing preedit session.
            // Preserve the full delta instead of assuming a single-character caret move.
            val delta = newValue.selection.end - previousImeFieldValue.selection.end
            if (delta != 0) {
                onPreferredColumnChange(null)
                inputAnchorState.clear()
                onFieldValueChange(
                    moveCaretHorizontally(
                        fieldValue = fieldValue,
                        delta = delta,
                        extendSelection = false,
                    )
                )
            } else {
                inputAnchorState.update(
                    newValue = newValue,
                    anchorSelection = inputAnchorState.anchorSelection ?: effectiveAnchorSelection,
                    consumedSelectionOnCompose = inputAnchorState.consumedSelectionOnCompose,
                )
            }
        }

        newValue.text.isEmpty() -> {
            inputAnchorState.clear()
        }

        newValue.composition != null -> {
            val shouldConsumeSelection = !effectiveAnchorSelection.collapsed && inputAnchorState.anchorSelection == null
            if (shouldConsumeSelection) {
                onFieldValueChange(
                    fieldValue.replaceRange(
                        range = effectiveAnchorSelection,
                        replacement = "",
                    )
                )
            }
            // Once composing starts, keep the anchor collapsed at the insertion boundary so the
            // IME-local text can be projected back into the document deterministically.
            val collapsedAnchorSelection = TextRange(effectiveAnchorSelection.normalizedStart)
            inputAnchorState.update(
                newValue = newValue,
                anchorSelection = collapsedAnchorSelection,
                consumedSelectionOnCompose = shouldConsumeSelection || inputAnchorState.consumedSelectionOnCompose,
            )
        }

        else -> {
            onPreferredColumnChange(null)
            onFieldValueChange(
                fieldValue.replaceRange(
                    range = inputAnchorState.anchorSelection ?: effectiveAnchorSelection,
                    replacement = newValue.text,
                )
            )
            inputAnchorState.clear()
        }
    }
}
