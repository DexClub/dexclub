package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboard
import io.github.dexclub.app.model.OpenTabUiModel
import io.github.dexclub.codeview.compose.CodeContentOptions
import io.github.dexclub.codeview.compose.CodeDecorationOptions
import io.github.dexclub.codeview.compose.CodeEditor
import io.github.dexclub.codeview.compose.CodeGutterOptions
import io.github.dexclub.codeview.compose.CodeViewerCursorTarget
import io.github.dexclub.codeview.compose.CodeViewerInteractionOptions
import io.github.dexclub.codeview.compose.rememberCodeAddons
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection
import io.github.dexclub.codeview.treesitter.java.install.treeSitterJavaLanguage
import io.github.dexclub.codeview.treesitter.smali.install.treeSitterSmaliLanguage
import io.github.dexclub.core.editor.EDITOR_SESSION_KIND_JAVA
import io.github.dexclub.core.editor.EditorInPageSearchSource
import io.github.dexclub.core.editor.EditorInPageSearchState
import io.github.dexclub.core.navigation.NavigateRequestContext
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.copyText
import kotlinx.coroutines.launch

private val WORKSPACE_CODE_GUTTER_OPTIONS: CodeGutterOptions = CodeGutterOptions()
private val WORKSPACE_CODE_CONTENT_OPTIONS: CodeContentOptions = CodeContentOptions()
private val WORKSPACE_CODE_DECORATION_OPTIONS: CodeDecorationOptions = CodeDecorationOptions()

private data class PendingSelectionCursorSync(
    val selection: LineSelection?,
    val cursor: Cursor?,
)

private data class SearchRevealStateKey(
    val matchQuery: String,
    val activeMatchIndex: Int,
    val cursorLine: Int,
    val cursorOffset: Int,
    val isVisible: Boolean,
)

@Composable
internal fun CodeViewPane(
    tab: OpenTabUiModel,
    paneState: WorkspaceCodePaneUiState,
    callbacks: WorkspaceCodePaneCallbacks,
    paneIndex: Int,
    kind: String,
    isSelectedTab: Boolean,
    navigationRevealTarget: NavigationRevealTarget?,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues.Zero,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val editorState = paneState.editorState
    val inPageSearchState = editorState.inPageSearchState
    val addons = rememberCodeAddons {
        install(treeSitterJavaLanguage())
        install(treeSitterSmaliLanguage())
    }
    val languageId = remember(kind) {
        CodeLanguageId(if (kind == EDITOR_SESSION_KIND_JAVA) "java" else "smali")
    }
    val document = remember(paneState.contentKey, languageId) {
        CodeDocument.create(languageId, paneState.text)
    }
    var currentText by remember(paneState.contentKey) { mutableStateOf(paneState.text) }

    LaunchedEffect(paneState.text) {
        if (paneState.text != currentText) {
            currentText = paneState.text
        }
        document.update(paneState.text)
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var menuPos by remember { mutableStateOf(Offset.Zero) }
    var menuNavigateContext by remember { mutableStateOf<NavigateRequestContext?>(null) }
    var pendingSearchRevealTarget by remember(tab.tabId, kind) { mutableStateOf<CodeViewerCursorTarget?>(null) }
    var searchRevealToken by remember(tab.tabId, kind) { mutableStateOf(0L) }
    var lastAppliedSearchRevealKey by remember(tab.tabId, kind) { mutableStateOf<SearchRevealStateKey?>(null) }
    var pendingSelectionCursorSync by remember(tab.tabId, kind) { mutableStateOf<PendingSelectionCursorSync?>(null) }

    val navigationCursorTarget = remember(navigationRevealTarget, editorState.cursorLine, editorState.cursorOffset) {
        if (navigationRevealTarget == null) return@remember null
        if (navigationRevealTarget.tabId != tab.tabId) return@remember null
        if (navigationRevealTarget.kind != null && navigationRevealTarget.kind != kind) return@remember null
        val cursorLine = editorState.cursorLine
        if (cursorLine < 0) return@remember null
        CodeViewerCursorTarget(
            line = cursorLine,
            offset = editorState.cursorOffset,
            token = navigationRevealTarget.token,
        )
    }
    val externalCursor = remember(editorState.cursorLine, editorState.cursorOffset) {
        if (editorState.cursorLine < 0 || editorState.cursorOffset < 0) {
            null
        } else {
            Cursor(
                line = editorState.cursorLine,
                offset = editorState.cursorOffset,
            )
        }
    }
    var latestSelection by remember(tab.tabId, kind) { mutableStateOf(editorState.selection) }
    var latestCursor by remember(tab.tabId, kind) { mutableStateOf(externalCursor) }
    val effectiveSelection = pendingSelectionCursorSync?.selection ?: latestSelection
    val effectiveCursor = pendingSelectionCursorSync?.cursor ?: latestCursor
    val selectedText = remember(currentText, effectiveSelection) {
        extractSelectedText(
            text = currentText,
            selection = effectiveSelection,
        )
    }
    val inPageSearchMatches = remember(currentText, inPageSearchState.matchQuery) {
        resolveInPageSearchMatches(
            text = currentText,
            query = inPageSearchState.matchQuery,
        )
    }
    val activeSearchMatchIndex = remember(inPageSearchState.activeMatchIndex, inPageSearchMatches) {
        when {
            inPageSearchMatches.isEmpty() -> 0
            else -> inPageSearchState.activeMatchIndex.coerceIn(0, inPageSearchMatches.lastIndex)
        }
    }
    val activeSearchMatch = remember(inPageSearchState.isVisible, activeSearchMatchIndex, inPageSearchMatches) {
        if (!inPageSearchState.isVisible || inPageSearchMatches.isEmpty()) {
            null
        } else {
            inPageSearchMatches[activeSearchMatchIndex]
        }
    }
    val searchHighlight = remember(activeSearchMatch, editorState.searchHighlight, inPageSearchState.isVisible) {
        if (inPageSearchState.isVisible) {
            activeSearchMatch?.selection
        } else {
            editorState.searchHighlight
        }
    }
    val inactiveSearchHighlights = remember(
        inPageSearchState.isVisible,
        inPageSearchMatches,
        activeSearchMatchIndex,
    ) {
        if (!inPageSearchState.isVisible || inPageSearchMatches.isEmpty()) {
            emptyList()
        } else {
            inPageSearchMatches.mapIndexedNotNull { index, match ->
                if (index == activeSearchMatchIndex) null else match.selection
            }
        }
    }
    val effectiveCursorTarget = navigationCursorTarget ?: pendingSearchRevealTarget

    fun requestSearchReveal(
        matchQuery: String,
        activeMatchIndex: Int,
        cursor: Cursor,
        isVisible: Boolean,
    ) {
        val revealKey = SearchRevealStateKey(
            matchQuery = matchQuery,
            activeMatchIndex = activeMatchIndex,
            cursorLine = cursor.line,
            cursorOffset = cursor.offset,
            isVisible = isVisible,
        )
        if (lastAppliedSearchRevealKey == revealKey) return
        searchRevealToken += 1L
        pendingSearchRevealTarget = CodeViewerCursorTarget(
            line = cursor.line,
            offset = cursor.offset,
            token = searchRevealToken,
        )
        lastAppliedSearchRevealKey = revealKey
    }

    LaunchedEffect(editorState.selection, externalCursor) {
        val pendingSync = pendingSelectionCursorSync
        if (pendingSync != null) {
            if (
                editorState.selection == pendingSync.selection &&
                externalCursor == pendingSync.cursor
            ) {
                latestSelection = editorState.selection
                latestCursor = externalCursor
                pendingSelectionCursorSync = null
            }
            return@LaunchedEffect
        }
        latestSelection = editorState.selection
        latestCursor = externalCursor
    }

    LaunchedEffect(inPageSearchState.activeMatchIndex, activeSearchMatchIndex, inPageSearchMatches.size) {
        if (inPageSearchState.activeMatchIndex != activeSearchMatchIndex) {
            callbacks.onUpdateInPageSearchState(
                tab.tabId,
                kind,
                inPageSearchState.copy(activeMatchIndex = activeSearchMatchIndex),
            )
        }
    }

    LaunchedEffect(
        isSelectedTab,
        inPageSearchState.matchQuery,
        inPageSearchState.isVisible,
        activeSearchMatchIndex,
        activeSearchMatch?.cursor?.line,
        activeSearchMatch?.cursor?.offset,
    ) {
        if (!isSelectedTab) return@LaunchedEffect
        val match = activeSearchMatch ?: run {
            lastAppliedSearchRevealKey = null
            pendingSearchRevealTarget = null
            return@LaunchedEffect
        }
        requestSearchReveal(
            matchQuery = inPageSearchState.matchQuery,
            activeMatchIndex = activeSearchMatchIndex,
            cursor = match.cursor,
            isVisible = inPageSearchState.isVisible,
        )
    }

    LaunchedEffect(isSelectedTab, navigationRevealTarget, navigationCursorTarget) {
        if (!isSelectedTab) return@LaunchedEffect
        val target = navigationRevealTarget ?: return@LaunchedEffect
        if (navigationCursorTarget == null) return@LaunchedEffect

        // 等当前组合把 reveal target 传给 CodeEditor 后再清空外层待消费状态，避免后续重组重复触发。
        withFrameNanos { }
        callbacks.onConsumeNavigationRevealTarget(target)
    }

    LaunchedEffect(pendingSearchRevealTarget?.token) {
        if (pendingSearchRevealTarget == null) return@LaunchedEffect

        withFrameNanos { }
        pendingSearchRevealTarget = null
    }

    fun pushInPageSearchState(
        nextState: EditorInPageSearchState,
    ) {
        callbacks.onUpdateInPageSearchState(
            tab.tabId,
            kind,
            nextState,
        )
    }

    fun updateCursorSelection(
        selection: LineSelection?,
        nextCursor: Cursor?,
    ) {
        pendingSelectionCursorSync = PendingSelectionCursorSync(
            selection = selection,
            cursor = nextCursor,
        )
        callbacks.onUpdateCursorSelection(
            tab.tabId,
            kind,
            nextCursor?.line ?: -1,
            nextCursor?.offset ?: -1,
            selection,
        )
    }

    fun updateLocalCursorSelection(
        selection: LineSelection?,
        cursor: Cursor?,
    ) {
        latestSelection = selection
        latestCursor = cursor
    }

    fun applySearchMatch(
        match: WorkspaceInPageSearchMatch,
        matchQuery: String,
        matchIndex: Int,
    ) {
        updateLocalCursorSelection(
            selection = match.selection,
            cursor = match.cursor,
        )
        requestSearchReveal(
            matchQuery = matchQuery,
            activeMatchIndex = matchIndex,
            cursor = match.cursor,
            isVisible = true,
        )
        updateCursorSelection(
            selection = latestSelection,
            nextCursor = latestCursor,
        )
    }

    fun clearSearchSelection() {
        latestSelection = null
        updateCursorSelection(
            selection = null,
            nextCursor = effectiveCursor,
        )
    }

    fun activateSearchMatch(
        index: Int,
    ) {
        if (inPageSearchMatches.isEmpty()) return
        val normalizedIndex = index.coerceIn(0, inPageSearchMatches.lastIndex)
        val match = inPageSearchMatches[normalizedIndex]
        pushInPageSearchState(
            inPageSearchState.copy(
                activeMatchIndex = normalizedIndex,
                isVisible = true,
            ),
        )
        applySearchMatch(
            match = match,
            matchQuery = inPageSearchState.matchQuery,
            matchIndex = normalizedIndex,
        )
    }

    fun navigateSearchMatch(
        delta: Int,
    ) {
        if (inPageSearchMatches.isEmpty()) return
        val count = inPageSearchMatches.size
        val nextIndex = (activeSearchMatchIndex + delta + count) % count
        activateSearchMatch(nextIndex)
    }

    fun updateSearchQuery(
        query: String,
    ) {
        val matches = resolveInPageSearchMatches(
            text = currentText,
            query = query,
        )
        pushInPageSearchState(
            inPageSearchState.copy(
                queryText = query,
                matchQuery = query,
                source = EditorInPageSearchSource.Manual,
                activeMatchIndex = 0,
                isVisible = true,
            ),
        )
        if (matches.isNotEmpty()) {
            applySearchMatch(
                match = matches.first(),
                matchQuery = query,
                matchIndex = 0,
            )
        } else {
            clearSearchSelection()
        }
    }

    fun openSearchBar(selectedQueryCandidate: String? = selectedText) {
        callbacks.onActivatePane(tab, paneIndex, kind)
        val selectedQuery = selectedQueryCandidate?.takeIf { text ->
            text.isNotBlank() && !text.contains('\n') && !text.contains('\r')
        }
        if (selectedQuery != null) {
            val matches = resolveInPageSearchMatches(
                text = currentText,
                query = selectedQuery,
            )
            val activeMatchIndex = effectiveSelection?.let { selection ->
                findInPageSearchMatchIndex(
                    matches = matches,
                    selection = selection,
                )
            } ?: 0

            pushInPageSearchState(
                inPageSearchState.copy(
                    queryText = selectedQuery,
                    matchQuery = selectedQuery,
                    source = EditorInPageSearchSource.Manual,
                    activeMatchIndex = activeMatchIndex.coerceAtLeast(0),
                    isVisible = true,
                    requestFocusToken = inPageSearchState.requestFocusToken + 1,
                ),
            )
            return
        }
        pushInPageSearchState(
            inPageSearchState.copy(
                isVisible = true,
                source = if (inPageSearchState.queryText.isEmpty() && inPageSearchState.matchQuery.isEmpty()) {
                    EditorInPageSearchSource.Manual
                } else {
                    inPageSearchState.source
                },
                requestFocusToken = inPageSearchState.requestFocusToken + 1,
            ),
        )
    }

    fun closeSearchBar() {
        pendingSearchRevealTarget = null
        pushInPageSearchState(
            inPageSearchState.copy(isVisible = false),
        )
    }

    fun handleSearchShortcut(
        keyEvent: KeyEvent,
    ): Boolean {
        if (!isSelectedTab || keyEvent.type != KeyEventType.KeyDown) {
            return false
        }
        if (isCodeCopyShortcut(keyEvent) && selectedText.isNotEmpty()) {
            scope.launch { clipboard.copyText(selectedText, label = "code_selection") }
            return true
        }
        if (isInPageSearchShortcut(keyEvent)) {
            openSearchBar(selectedText)
            return true
        }
        return false
    }

    Box(
        modifier = modifier.onPreviewKeyEvent(::handleSearchShortcut),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (inPageSearchState.isVisible) {
                WorkspaceInPageSearchBar(
                    queryText = inPageSearchState.queryText,
                    activeMatchIndex = activeSearchMatchIndex,
                    matchCount = inPageSearchMatches.size,
                    requestFocusToken = inPageSearchState.requestFocusToken,
                    onQueryChange = ::updateSearchQuery,
                    onPreviousMatch = { navigateSearchMatch(delta = -1) },
                    onNextMatch = { navigateSearchMatch(delta = 1) },
                    onClose = ::closeSearchBar,
                )
                HorizontalDivider(
                    color = ShadcnTheme.colors.border.copy(alpha = 0.5f),
                )
            }

            CodeEditor(
                document = document,
                addons = addons,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .onPreviewKeyEvent(::handleSearchShortcut),
                initialFirstVisibleLine = editorState.scrollOffsetY,
                initialScrollOffsetX = editorState.scrollOffsetX,
                scrollPastEnd = paneState.scrollPastEnd,
                selection = effectiveSelection,
                cursor = effectiveCursor,
                searchHighlight = searchHighlight,
                inactiveSearchHighlights = inactiveSearchHighlights,
                cursorTarget = effectiveCursorTarget,
                interactionOptions = CodeViewerInteractionOptions(
                    annotationTag = NODE_ANNOTATION_TAG,
                ),
                gutterOptions = WORKSPACE_CODE_GUTTER_OPTIONS,
                contentOptions = WORKSPACE_CODE_CONTENT_OPTIONS,
                decorationOptions = WORKSPACE_CODE_DECORATION_OPTIONS,
                onTextChange = { newText ->
                    currentText = newText
                },
                onScrollChange = { firstVisibleLine, scrollOffsetX ->
                    if (isSelectedTab) {
                        callbacks.onUpdateScrollOffset(tab.tabId, kind, firstVisibleLine, scrollOffsetX)
                    }
                },
                onViewportChange = { firstVisibleLine, lastVisibleLine ->
                    if (isSelectedTab) {
                        callbacks.onCodeViewportChanged(tab.tabId, kind, firstVisibleLine, lastVisibleLine)
                    }
                },
                onSelectionChange = { selection ->
                    if (!isSelectedTab) return@CodeEditor
                    pendingSearchRevealTarget = null
                    latestSelection = selection
                },
                onCursorChange = { nextCursor ->
                    if (!isSelectedTab) return@CodeEditor
                    pendingSearchRevealTarget = null
                    latestCursor = nextCursor
                    updateCursorSelection(
                        selection = latestSelection,
                        nextCursor = nextCursor,
                    )
                },
                onAnnotationHit = { hit ->
                    if (!isSelectedTab) return@CodeEditor
                    val request = toNavigateRequestContext(
                        annotationHit = hit,
                        tabId = tab.tabId,
                        paneIndex = paneIndex,
                        activeKind = kind,
                    ) ?: return@CodeEditor
                    callbacks.onNavigateToDefinition(request)
                },
                onFindRequested = { selectedText ->
                    if (!isSelectedTab) return@CodeEditor
                    openSearchBar(selectedText)
                },
                onContextMenu = { hit, offset ->
                    if (!isSelectedTab) return@CodeEditor
                    callbacks.onActivatePane(tab, paneIndex, kind)
                    menuPos = offset
                    menuNavigateContext = hit?.let {
                        toNavigateRequestContext(
                            annotationHit = it,
                            tabId = tab.tabId,
                            paneIndex = paneIndex,
                            activeKind = kind,
                        )
                    }
                    menuExpanded = true
                },
            )
        }

        if (isSelectedTab) {
            CodeContextMenu(
                selectedText = selectedText,
                onSelectAll = {
                    callbacks.onActivatePane(tab, paneIndex, kind)
                    val selection = resolveSelectAllSelection(currentText)
                    latestSelection = selection
                    latestCursor = selection?.let {
                        Cursor(
                            line = it.endLine,
                            offset = it.endOffset,
                        )
                    }
                    updateCursorSelection(
                        selection = latestSelection,
                        nextCursor = latestCursor,
                    )
                },
                expanded = menuExpanded,
                position = menuPos,
                navigateContext = menuNavigateContext,
                onNavigateToDefinition = callbacks.onNavigateToDefinition,
                onDismissRequest = { menuExpanded = false },
            )
        }
    }
}
