package io.github.dexclub.codeview.compose.internal.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshotFactory
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerScrollController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeEditorPointerInputTest {
    @Test
    fun resolveDesktopMultiClickCount_upgradesToDoubleAndTripleWithinThreshold() {
        val doubleClick = resolveDesktopMultiClickCount(
            clickUptimeMillis = 140L,
            clickPosition = Offset(12f, 18f),
            previousClickUptimeMillis = 100L,
            previousClickPosition = Offset(10f, 20f),
            previousClickCount = 1,
            multiClickTimeoutMillis = 300L,
            multiClickSlopPx = 8f,
        )
        val tripleClick = resolveDesktopMultiClickCount(
            clickUptimeMillis = 180L,
            clickPosition = Offset(11f, 19f),
            previousClickUptimeMillis = 140L,
            previousClickPosition = Offset(12f, 18f),
            previousClickCount = 2,
            multiClickTimeoutMillis = 300L,
            multiClickSlopPx = 8f,
        )

        assertEquals(2, doubleClick)
        assertEquals(3, tripleClick)
    }

    @Test
    fun resolveDesktopMultiClickCount_resetsWhenTimeoutOrDistanceExceeded() {
        assertEquals(
            1,
            resolveDesktopMultiClickCount(
                clickUptimeMillis = 500L,
                clickPosition = Offset(10f, 20f),
                previousClickUptimeMillis = 100L,
                previousClickPosition = Offset(10f, 20f),
                previousClickCount = 1,
                multiClickTimeoutMillis = 300L,
                multiClickSlopPx = 8f,
            )
        )
        assertEquals(
            1,
            resolveDesktopMultiClickCount(
                clickUptimeMillis = 140L,
                clickPosition = Offset(30f, 20f),
                previousClickUptimeMillis = 100L,
                previousClickPosition = Offset(10f, 20f),
                previousClickCount = 1,
                multiClickTimeoutMillis = 300L,
                multiClickSlopPx = 8f,
            )
        )
    }

    @Test
    fun resolveDesktopClickSelection_selectsWordOnDoubleClick() {
        val snapshot = CodeLayoutSnapshotFactory.create("foo bar+baz")

        val selection = resolveDesktopClickSelection(
            layoutSnapshot = snapshot,
            clickCount = 2,
            anchorOffset = 5,
        )

        assertEquals(TextRange(4, 7), selection)
    }

    @Test
    fun resolveDesktopClickSelection_selectsLineOnTripleClickIncludingTrailingNewline() {
        val snapshot = CodeLayoutSnapshotFactory.create("first\nsecond\nthird")

        val selection = resolveDesktopClickSelection(
            layoutSnapshot = snapshot,
            clickCount = 3,
            anchorOffset = 8,
        )

        assertEquals(TextRange(6, 13), selection)
    }

    @Test
    fun resolveSelectionLineRange_usesLineEndOnLastLineWithoutTrailingNewline() {
        val snapshot = CodeLayoutSnapshotFactory.create("first\nsecond")

        val selection = resolveSelectionLineRange(
            layoutSnapshot = snapshot,
            rawOffset = snapshot.text.length,
        )

        assertEquals(TextRange(6, 12), selection)
    }

    @Test
    fun resolveSelectionTapAction_keepsSelectionWhenTapFallsInsideAndKeyboardHidden() {
        assertEquals(
            SelectionTapAction.KeepSelection,
            resolveSelectionTapAction(
                selection = TextRange(3, 8),
                tappedOffset = 3,
                isSoftwareKeyboardVisible = false,
            )
        )
        assertEquals(
            SelectionTapAction.KeepSelection,
            resolveSelectionTapAction(
                selection = TextRange(3, 8),
                tappedOffset = 5,
                isSoftwareKeyboardVisible = false,
            )
        )
        assertEquals(
            SelectionTapAction.KeepSelection,
            resolveSelectionTapAction(
                selection = TextRange(3, 8),
                tappedOffset = 8,
                isSoftwareKeyboardVisible = false,
            )
        )
    }

    @Test
    fun resolveSelectionTapAction_handlesReversedSelection() {
        assertEquals(
            SelectionTapAction.KeepSelection,
            resolveSelectionTapAction(
                selection = TextRange(8, 3),
                tappedOffset = 5,
                isSoftwareKeyboardVisible = false,
            )
        )
    }

    @Test
    fun resolveSelectionTapAction_collapsesWhenTapFallsOutsideSelectionOrSelectionCollapsed() {
        assertEquals(
            SelectionTapAction.CollapseSelection,
            resolveSelectionTapAction(
                selection = TextRange(3, 8),
                tappedOffset = 2,
                isSoftwareKeyboardVisible = false,
            )
        )
        assertEquals(
            SelectionTapAction.CollapseSelection,
            resolveSelectionTapAction(
                selection = TextRange(3, 8),
                tappedOffset = 9,
                isSoftwareKeyboardVisible = false,
            )
        )
        assertEquals(
            SelectionTapAction.CollapseSelection,
            resolveSelectionTapAction(
                selection = TextRange(4),
                tappedOffset = 4,
                isSoftwareKeyboardVisible = false,
            )
        )
    }

    @Test
    fun resolveSelectionTapAction_collapsesInsideSelectionWhenKeyboardAlreadyVisible() {
        assertEquals(
            SelectionTapAction.CollapseSelection,
            resolveSelectionTapAction(
                selection = TextRange(3, 8),
                tappedOffset = 5,
                isSoftwareKeyboardVisible = true,
            )
        )
    }

    @Test
    fun resolveLongPressDragSelection_keepsOriginalWordWhilePointerStaysInsideWord() {
        val selection = resolveLongPressDragSelection(
            initialSelection = TextRange(4, 8),
            draggedTextOffset = 6,
        )

        assertEquals(TextRange(4, 8), selection)
    }

    @Test
    fun resolveLongPressDragSelection_expandsFromWordStartWhenDraggingRight() {
        val selection = resolveLongPressDragSelection(
            initialSelection = TextRange(4, 8),
            draggedTextOffset = 12,
        )

        assertEquals(TextRange(4, 12), selection)
    }

    @Test
    fun resolveLongPressDragSelection_expandsFromWordEndWhenDraggingLeft() {
        val selection = resolveLongPressDragSelection(
            initialSelection = TextRange(4, 8),
            draggedTextOffset = 1,
        )

        assertEquals(TextRange(8, 1), selection)
    }

    @Test
    fun resolveLongPressDragSelection_usesCollapsedOffsetAsAnchorWhenInitialSelectionCollapsed() {
        val selection = resolveLongPressDragSelection(
            initialSelection = TextRange(5),
            draggedTextOffset = 9,
        )

        assertEquals(TextRange(5, 9), selection)
    }

    @Test
    fun resolveTouchAutoScrollDelta_scalesWithOverflowAndFrameDuration() {
        val scrollController = CodeViewerScrollController(
            horizontalScrollPxProvider = { 0f },
            verticalScrollPxProvider = { 0f },
            viewportWidthPxProvider = { 100f },
            viewportHeightPxProvider = { 80f },
            contentLeftInsetPxProvider = { 0f },
            contentStartPaddingPxProvider = { 4f },
            scrollByHandler = { _, _ -> },
        )

        val slightOverflowDelta = resolveTouchAutoScrollDelta(
            viewportPosition = Offset(-8f, 40f),
            scrollController = scrollController,
            frameDurationNanos = DEFAULT_TOUCH_AUTO_SCROLL_FRAME_DURATION_NANOS,
        )
        val deepOverflowDelta = resolveTouchAutoScrollDelta(
            viewportPosition = Offset(172f, 152f),
            scrollController = scrollController,
            frameDurationNanos = DEFAULT_TOUCH_AUTO_SCROLL_FRAME_DURATION_NANOS,
        )
        val longFrameDelta = resolveTouchAutoScrollDelta(
            viewportPosition = Offset(172f, 40f),
            scrollController = scrollController,
            frameDurationNanos = DEFAULT_TOUCH_AUTO_SCROLL_FRAME_DURATION_NANOS * 2,
        )

        assertEquals(Offset.Zero, resolveTouchAutoScrollDelta(
            viewportPosition = Offset(32f, 24f),
            scrollController = scrollController,
        ))
        assertTrue(kotlin.math.abs(slightOverflowDelta.x) < kotlin.math.abs(deepOverflowDelta.x))
        assertEquals(0f, slightOverflowDelta.y)
        assertTrue(deepOverflowDelta.x > 0f)
        assertTrue(deepOverflowDelta.y > 0f)
        assertTrue(longFrameDelta.x > deepOverflowDelta.x)
    }
}
