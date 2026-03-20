package io.github.dexclub.core.editor

import io.github.dexclub.codeview.core.text.LineSelection
import io.github.dexclub.app.model.OpenTabContentSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

typealias EditorStateWarnHandler = (String, Throwable?) -> Unit

data class EditorContentStateSnapshot(
    val scrollOffsetY: Int = 0,
    val scrollOffsetX: Int = 0,
    val cursorLine: Int = -1,
    val cursorOffset: Int = 0,
    val selection: LineSelection? = null,
    val searchHighlight: LineSelection? = null,
)

class EditorStateRepository(
    private val editorSessionRepository: EditorSessionRepository,
    private val scope: CoroutineScope,
    private val onWarn: EditorStateWarnHandler = { _, _ -> },
    private val scrollSaveDebounceMs: Long = DEFAULT_SCROLL_SAVE_DEBOUNCE_MS,
    private val cursorSaveDebounceMs: Long = DEFAULT_CURSOR_SAVE_DEBOUNCE_MS,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
) {
    private val stateLock = Any()
    private val scrollStatesY = mutableMapOf<String, Int>()
    private val scrollStatesX = mutableMapOf<String, Int>()
    private val cursorStates = mutableMapOf<String, EditorCursorState>()
    private val searchHighlightStates = mutableMapOf<String, LineSelection?>()
    private val scrollSaveJobs = mutableMapOf<String, Job>()
    private val cursorSaveJobs = mutableMapOf<String, Job>()
    private val _searchHighlightRevision = MutableStateFlow(0L)

    val searchHighlightRevision: StateFlow<Long> = _searchHighlightRevision

    fun hydrateContents(
        tabId: String,
        contents: Map<String, OpenTabContentSnapshot>,
    ) {
        synchronized(stateLock) {
            contents.forEach { (kind, content) ->
                val key = buildEditorContentKey(tabId, kind)
                scrollStatesY.putIfAbsent(key, content.scrollOffsetY)
                scrollStatesX.putIfAbsent(key, content.scrollOffsetX)

                if (cursorStates.containsKey(key)) return@forEach

                val selection = content.selection
                if (content.cursorLine >= 0 || selection != null) {
                    cursorStates[key] = EditorCursorState(
                        cursorLine = content.cursorLine,
                        cursorOffset = content.cursorOffset.coerceAtLeast(0),
                        selection = selection,
                    )
                }
            }
        }
    }

    fun updateScrollOffset(
        tabId: String,
        kind: String,
        offsetY: Int,
        offsetX: Int,
    ) {
        val key = buildEditorContentKey(tabId, kind)
        val previousJob = synchronized(stateLock) {
            val currentY = scrollStatesY[key]
            val currentX = scrollStatesX[key]
            if (currentY == offsetY && currentX == offsetX) {
                return
            }

            scrollStatesY[key] = offsetY
            scrollStatesX[key] = offsetX
            scrollSaveJobs.remove(key)
        }
        previousJob?.cancel()

        val newJob = scope.launch {
            delay(scrollSaveDebounceMs)
            try {
                editorSessionRepository.updateContentScrollOffset(
                    tabId = tabId,
                    kind = kind,
                    offsetY = offsetY,
                    offsetX = offsetX,
                    updatedAt = nowProvider(),
                )
            } catch (throwable: Throwable) {
                warn(
                    text = "保存滚动位置失败",
                    throwable = throwable,
                )
            }
        }
        synchronized(stateLock) {
            scrollSaveJobs[key] = newJob
        }
    }

    fun getScrollOffsetY(
        tabId: String,
        kind: String,
        fallback: Int,
    ): Int {
        synchronized(stateLock) {
            return scrollStatesY[buildEditorContentKey(tabId, kind)] ?: fallback
        }
    }

    fun getScrollOffsetX(
        tabId: String,
        kind: String,
        fallback: Int,
    ): Int {
        synchronized(stateLock) {
            return scrollStatesX[buildEditorContentKey(tabId, kind)] ?: fallback
        }
    }

    fun updateCursorSelection(
        tabId: String,
        kind: String,
        cursorLine: Int,
        cursorOffset: Int,
        selection: LineSelection?,
    ) {
        val key = buildEditorContentKey(tabId, kind)
        val snapshot = EditorCursorState(
            cursorLine = cursorLine,
            cursorOffset = cursorOffset,
            selection = selection,
        )

        val previousJob = synchronized(stateLock) {
            if (cursorStates[key] == snapshot) {
                return
            }

            cursorStates[key] = snapshot
            cursorSaveJobs.remove(key)
        }
        previousJob?.cancel()

        val newJob = scope.launch {
            delay(cursorSaveDebounceMs)
            val latestSnapshot = synchronized(stateLock) { cursorStates[key] } ?: return@launch
            val latestSelection = latestSnapshot.selection
            try {
                editorSessionRepository.updateContentCursorSelection(
                    tabId = tabId,
                    kind = kind,
                    cursorLine = latestSnapshot.cursorLine,
                    cursorOffset = latestSnapshot.cursorOffset,
                    selectionStartLine = latestSelection?.startLine ?: -1,
                    selectionStartOffset = latestSelection?.startOffset ?: -1,
                    selectionEndLine = latestSelection?.endLine ?: -1,
                    selectionEndOffset = latestSelection?.endOffset ?: -1,
                    updatedAt = nowProvider(),
                )
            } catch (throwable: Throwable) {
                warn(
                    text = "保存光标/选区失败",
                    throwable = throwable,
                )
            }
        }
        synchronized(stateLock) {
            cursorSaveJobs[key] = newJob
        }
    }

    fun getCursorLine(
        tabId: String,
        kind: String,
        fallback: Int,
    ): Int {
        synchronized(stateLock) {
            return cursorStates[buildEditorContentKey(tabId, kind)]?.cursorLine ?: fallback
        }
    }

    fun getCursorOffset(
        tabId: String,
        kind: String,
        fallback: Int,
    ): Int {
        synchronized(stateLock) {
            return cursorStates[buildEditorContentKey(tabId, kind)]?.cursorOffset ?: fallback
        }
    }

    fun getSelection(
        tabId: String,
        kind: String,
        fallback: LineSelection?,
    ): LineSelection? {
        synchronized(stateLock) {
            return cursorStates[buildEditorContentKey(tabId, kind)]?.selection ?: fallback
        }
    }

    fun updateSearchHighlight(
        tabId: String,
        kind: String,
        highlight: LineSelection?,
    ) {
        synchronized(stateLock) {
            val key = buildEditorContentKey(tabId, kind)
            if (searchHighlightStates[key] == highlight) return
            searchHighlightStates[key] = highlight
            _searchHighlightRevision.value += 1
        }
    }

    fun getSearchHighlight(
        tabId: String,
        kind: String,
        fallback: LineSelection? = null,
    ): LineSelection? {
        synchronized(stateLock) {
            return searchHighlightStates[buildEditorContentKey(tabId, kind)] ?: fallback
        }
    }

    fun getContentStateSnapshot(
        tabId: String,
        kind: String,
        fallbackContent: OpenTabContentSnapshot? = null,
    ): EditorContentStateSnapshot {
        synchronized(stateLock) {
            val key = buildEditorContentKey(tabId, kind)
            val cursorState = cursorStates[key]
            return EditorContentStateSnapshot(
                scrollOffsetY = scrollStatesY[key] ?: fallbackContent?.scrollOffsetY ?: 0,
                scrollOffsetX = scrollStatesX[key] ?: fallbackContent?.scrollOffsetX ?: 0,
                cursorLine = cursorState?.cursorLine ?: fallbackContent?.cursorLine ?: -1,
                cursorOffset = cursorState?.cursorOffset
                    ?: fallbackContent?.cursorOffset?.coerceAtLeast(0)
                    ?: 0,
                selection = cursorState?.selection ?: fallbackContent?.selection,
                searchHighlight = searchHighlightStates[key],
            )
        }
    }

    fun clearSearchHighlightsForTab(tabId: String) {
        synchronized(stateLock) {
            val keys = searchHighlightStates.keys
                .filter { key -> key.startsWith("$tabId#") }
            if (keys.isEmpty()) return
            keys.forEach(searchHighlightStates::remove)
            _searchHighlightRevision.value += 1
        }
    }

    fun clearTabStates(tabIds: Set<String>) {
        if (tabIds.isEmpty()) return

        val jobsToCancel = synchronized(stateLock) {
            val jobs = mutableListOf<Job>()
            var removedSearchHighlights = false
            scrollStatesY.keys.filter { key -> isStateKeyForTabs(key, tabIds) }.forEach(scrollStatesY::remove)
            scrollStatesX.keys.filter { key -> isStateKeyForTabs(key, tabIds) }.forEach(scrollStatesX::remove)
            cursorStates.keys.filter { key -> isStateKeyForTabs(key, tabIds) }.forEach(cursorStates::remove)
            searchHighlightStates.keys.filter { key -> isStateKeyForTabs(key, tabIds) }.forEach { key ->
                searchHighlightStates.remove(key)
                removedSearchHighlights = true
            }
            if (removedSearchHighlights) {
                _searchHighlightRevision.value += 1
            }
            scrollSaveJobs.keys.filter { key -> isStateKeyForTabs(key, tabIds) }.forEach { key ->
                scrollSaveJobs.remove(key)?.let(jobs::add)
            }
            cursorSaveJobs.keys.filter { key -> isStateKeyForTabs(key, tabIds) }.forEach { key ->
                cursorSaveJobs.remove(key)?.let(jobs::add)
            }
            jobs
        }
        jobsToCancel.forEach(Job::cancel)
    }

    fun close() {
        val jobsToCancel = synchronized(stateLock) {
            (scrollSaveJobs.values + cursorSaveJobs.values).toList().also {
                scrollSaveJobs.clear()
                cursorSaveJobs.clear()
            }
        }
        jobsToCancel.forEach(Job::cancel)
    }

    private fun isStateKeyForTabs(
        key: String,
        tabIds: Set<String>,
    ): Boolean {
        return tabIds.any { tabId -> key.startsWith("$tabId#") }
    }

    private fun warn(
        text: String,
        throwable: Throwable? = null,
    ) {
        onWarn(text, throwable)
    }

    private data class EditorCursorState(
        val cursorLine: Int,
        val cursorOffset: Int,
        val selection: LineSelection?,
    )

    companion object {
        private const val DEFAULT_SCROLL_SAVE_DEBOUNCE_MS = 400L
        private const val DEFAULT_CURSOR_SAVE_DEBOUNCE_MS = 300L
    }
}
