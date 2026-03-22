package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeEditorPointerInputTest {
    @Test
    fun shouldKeepSelectionOnTap_returnsTrueWhenTapFallsInsideSelection() {
        assertTrue(shouldKeepSelectionOnTap(TextRange(3, 8), tappedOffset = 3))
        assertTrue(shouldKeepSelectionOnTap(TextRange(3, 8), tappedOffset = 5))
        assertTrue(shouldKeepSelectionOnTap(TextRange(3, 8), tappedOffset = 8))
    }

    @Test
    fun shouldKeepSelectionOnTap_returnsTrueForReversedSelection() {
        assertTrue(shouldKeepSelectionOnTap(TextRange(8, 3), tappedOffset = 5))
    }

    @Test
    fun shouldKeepSelectionOnTap_returnsFalseWhenTapFallsOutsideOrSelectionCollapsed() {
        assertFalse(shouldKeepSelectionOnTap(TextRange(3, 8), tappedOffset = 2))
        assertFalse(shouldKeepSelectionOnTap(TextRange(3, 8), tappedOffset = 9))
        assertFalse(shouldKeepSelectionOnTap(TextRange(4), tappedOffset = 4))
    }
}
