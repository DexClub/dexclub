package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal class CodeEditorInputAnchorState {
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
    val effectiveAnchorSelection = inputAnchorState.anchorSelection ?: fieldValue.selection
    when {
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
