package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal data class CodeEditorComposingOverlay(
    val anchorSelection: TextRange,
    val imeFieldValue: TextFieldValue,
)

internal fun CodeEditorInputAnchorState.toComposingOverlayOrNull(): CodeEditorComposingOverlay? {
    val composition = imeFieldValue.composition ?: return null
    if (imeFieldValue.text.isEmpty()) return null
    val anchorSelection = anchorSelection ?: return null
    return CodeEditorComposingOverlay(
        anchorSelection = anchorSelection,
        imeFieldValue = imeFieldValue.copy(
            composition = composition,
        ),
    )
}
