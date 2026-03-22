package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import io.github.dexclub.app.model.OpenTabUiModel
import io.github.dexclub.codeview.compose.CodeEditor
import io.github.dexclub.codeview.compose.CodeViewerCursorTarget
import io.github.dexclub.codeview.compose.CodeViewerInteractionOptions
import io.github.dexclub.codeview.compose.rememberCodeAddons
import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.treesitter.java.install.treeSitterJavaLanguage
import io.github.dexclub.codeview.treesitter.smali.install.treeSitterSmaliLanguage
import io.github.dexclub.core.editor.EDITOR_SESSION_KIND_JAVA
import io.github.dexclub.core.navigation.NavigateRequestContext

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
    val editorState = paneState.editorState
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

    LaunchedEffect(paneState.text) {
        document.update(paneState.text)
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var menuPos by remember { mutableStateOf(Offset.Zero) }
    var menuNavigateContext by remember { mutableStateOf<NavigateRequestContext?>(null) }

    val cursorTarget = remember(navigationRevealTarget) {
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
    val cursor = remember(editorState.cursorLine, editorState.cursorOffset) {
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
    var latestCursor by remember(tab.tabId, kind) { mutableStateOf(cursor) }
    val selectedText = remember(paneState.text, latestSelection) {
        extractSelectedText(
            text = paneState.text,
            selection = latestSelection,
        )
    }

    LaunchedEffect(editorState.selection) {
        latestSelection = editorState.selection
    }

    LaunchedEffect(cursor) {
        latestCursor = cursor
    }

    fun updateCursorSelection(
        selection: LineSelection?,
        nextCursor: Cursor?,
    ) {
        callbacks.onUpdateCursorSelection(
            tab.tabId,
            kind,
            nextCursor?.line ?: -1,
            nextCursor?.offset ?: -1,
            selection,
        )
    }

    Box(modifier = modifier) {
        CodeEditor(
            document = document,
            addons = addons,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            initialFirstVisibleLine = editorState.scrollOffsetY,
            initialScrollOffsetX = editorState.scrollOffsetX,
            scrollPastEnd = paneState.scrollPastEnd,
            selection = editorState.selection,
            cursor = cursor,
            searchHighlight = editorState.searchHighlight,
            cursorTarget = cursorTarget,
            interactionOptions = CodeViewerInteractionOptions(
                annotationTag = NODE_ANNOTATION_TAG,
            ),
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
                latestSelection = selection
            },
            onCursorChange = { nextCursor ->
                if (!isSelectedTab) return@CodeEditor
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

        if (isSelectedTab) {
            CodeContextMenu(
                selectedText = selectedText,
                onSelectAll = {
                    callbacks.onActivatePane(tab, paneIndex, kind)
                    val selection = resolveSelectAllSelection(paneState.text)
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
