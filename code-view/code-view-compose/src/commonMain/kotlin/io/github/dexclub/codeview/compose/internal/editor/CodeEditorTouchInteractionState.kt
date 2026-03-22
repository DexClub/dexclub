package io.github.dexclub.codeview.compose.internal.editor

internal sealed interface TouchSelectionInteractionState {
    val showSelectionToolbar: Boolean
    val showSelectionHandles: Boolean
    val autoScrollSession: TouchSelectionAutoScrollSession?

    data object Idle : TouchSelectionInteractionState {
        override val showSelectionToolbar: Boolean = true
        override val showSelectionHandles: Boolean = true
        override val autoScrollSession: TouchSelectionAutoScrollSession? = null
    }

    data class LongPressSelecting(
        override val autoScrollSession: LongPressTouchSelectionAutoScrollSession,
    ) : TouchSelectionInteractionState {
        override val showSelectionToolbar: Boolean = false
        override val showSelectionHandles: Boolean = false
    }

    data class DraggingHandle(
        override val autoScrollSession: HandleTouchSelectionAutoScrollSession? = null,
    ) : TouchSelectionInteractionState {
        override val showSelectionToolbar: Boolean = false
        override val showSelectionHandles: Boolean = true
    }
}
