package io.github.dexclub.codeview.compose

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.core.text.CodeTextValue
import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshotFactory
import io.github.dexclub.codeview.compose.internal.viewer.CodeViewerCanvas
import io.github.dexclub.codeview.language.addon.CodeAddons
import io.github.dexclub.codeview.runtime.CodeRuntime

@CodeViewApi
@Composable
public fun CodeViewer(
    text: String,
    languageId: CodeLanguageId,
    addons: CodeAddons,
    modifier: Modifier = Modifier,
    runtime: CodeRuntime = remember { CodeRuntime() },
    initialFirstVisibleLine: Int = 0,
    initialScrollOffsetX: Int = 0,
    scrollPastEnd: Int = CodeViewDefaults.ScrollPastEnd,
    selection: LineSelection? = null,
    cursor: Cursor? = null,
    searchHighlight: LineSelection? = null,
    inactiveSearchHighlights: List<LineSelection> = emptyList(),
    cursorTarget: CodeViewerCursorTarget? = null,
    interactionOptions: CodeViewerInteractionOptions = CodeViewerInteractionOptions(),
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)? = null,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)? = null,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)? = null,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)? = null,
    textStyle: TextStyle = CodeViewDefaults.CodeTextStyle,
    gutterOptions: CodeGutterOptions = CodeViewDefaults.GutterOptions,
    contentOptions: CodeContentOptions = CodeViewDefaults.ContentOptions,
    decorationOptions: CodeDecorationOptions = CodeViewDefaults.DecorationOptions,
) {
    val document = remember(text, languageId) {
        CodeDocument.create(languageId, text)
    }
    CodeViewer(
        document = document,
        addons = addons,
        modifier = modifier,
        runtime = runtime,
        textStyle = textStyle,
        initialFirstVisibleLine = initialFirstVisibleLine,
        initialScrollOffsetX = initialScrollOffsetX,
        scrollPastEnd = scrollPastEnd,
        selection = selection,
        cursor = cursor,
        searchHighlight = searchHighlight,
        inactiveSearchHighlights = inactiveSearchHighlights,
        cursorTarget = cursorTarget,
        interactionOptions = interactionOptions,
        onScrollChange = onScrollChange,
        onViewportChange = onViewportChange,
        onAnnotationHit = onAnnotationHit,
        onContextMenu = onContextMenu,
        gutterOptions = gutterOptions,
        contentOptions = contentOptions,
        decorationOptions = decorationOptions,
    )
}

@CodeViewApi
@Composable
public fun CodeViewer(
    value: CodeTextValue,
    addons: CodeAddons,
    modifier: Modifier = Modifier,
    runtime: CodeRuntime = remember { CodeRuntime() },
    initialFirstVisibleLine: Int = 0,
    initialScrollOffsetX: Int = 0,
    scrollPastEnd: Int = CodeViewDefaults.ScrollPastEnd,
    selection: LineSelection? = null,
    cursor: Cursor? = null,
    searchHighlight: LineSelection? = null,
    inactiveSearchHighlights: List<LineSelection> = emptyList(),
    cursorTarget: CodeViewerCursorTarget? = null,
    interactionOptions: CodeViewerInteractionOptions = CodeViewerInteractionOptions(),
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)? = null,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)? = null,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)? = null,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)? = null,
    textStyle: TextStyle = CodeViewDefaults.CodeTextStyle,
    gutterOptions: CodeGutterOptions = CodeViewDefaults.GutterOptions,
    contentOptions: CodeContentOptions = CodeViewDefaults.ContentOptions,
    decorationOptions: CodeDecorationOptions = CodeViewDefaults.DecorationOptions,
) {
    val document = remember(value) {
        CodeDocument.create(
            languageId = value.language ?: CodeLanguageId("plaintext"),
            initialText = value.text,
        )
    }
    CodeViewer(
        document = document,
        addons = addons,
        modifier = modifier,
        runtime = runtime,
        textStyle = textStyle,
        initialFirstVisibleLine = initialFirstVisibleLine,
        initialScrollOffsetX = initialScrollOffsetX,
        scrollPastEnd = scrollPastEnd,
        selection = selection,
        cursor = cursor,
        searchHighlight = searchHighlight,
        inactiveSearchHighlights = inactiveSearchHighlights,
        cursorTarget = cursorTarget,
        interactionOptions = interactionOptions,
        onScrollChange = onScrollChange,
        onViewportChange = onViewportChange,
        onAnnotationHit = onAnnotationHit,
        onContextMenu = onContextMenu,
        gutterOptions = gutterOptions,
        contentOptions = contentOptions,
        decorationOptions = decorationOptions,
    )
}

/**
 * 带完整状态控制的 CodeViewer，供工作区主路径使用。
 * 简单调用（无滚动/选区/交互需求）可省略所有可选参数。
 */
@CodeViewApi
@Composable
public fun CodeViewer(
    document: CodeDocument,
    addons: CodeAddons,
    modifier: Modifier = Modifier,
    runtime: CodeRuntime = remember { CodeRuntime() },
    initialFirstVisibleLine: Int = 0,
    initialScrollOffsetX: Int = 0,
    scrollPastEnd: Int = CodeViewDefaults.ScrollPastEnd,
    selection: LineSelection? = null,
    cursor: Cursor? = null,
    searchHighlight: LineSelection? = null,
    inactiveSearchHighlights: List<LineSelection> = emptyList(),
    cursorTarget: CodeViewerCursorTarget? = null,
    interactionOptions: CodeViewerInteractionOptions = CodeViewerInteractionOptions(),
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)? = null,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)? = null,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)? = null,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)? = null,
    textStyle: TextStyle = CodeViewDefaults.CodeTextStyle,
    gutterOptions: CodeGutterOptions = CodeViewDefaults.GutterOptions,
    contentOptions: CodeContentOptions = CodeViewDefaults.ContentOptions,
    decorationOptions: CodeDecorationOptions = CodeViewDefaults.DecorationOptions,
) {
    val controller = remember(document, addons) {
        runtime.getSurfaceController(document, addons)
    }

    DisposableEffect(document.documentId) {
        onDispose { runtime.releaseDocument(document.documentId) }
    }

    val snapshot by document.snapshots.collectAsState()
    val tokens by controller.tokens.collectAsState()
    val annotations by controller.annotations.collectAsState()

    LaunchedEffect(snapshot.revision) {
        controller.refresh()
    }

    val textLayoutSnapshot = remember(snapshot.text) {
        CodeLayoutSnapshotFactory.create(snapshot.text)
    }
    val layoutSnapshot = remember(textLayoutSnapshot, tokens, annotations) {
        CodeLayoutSnapshotFactory.withDecorations(
            base = textLayoutSnapshot,
            tokens = tokens,
            annotations = annotations,
        )
    }

    CodeViewerCanvas(
        documentKey = document.documentId,
        documentRevision = snapshot.revision,
        layoutSnapshot = layoutSnapshot,
        modifier = modifier,
        textStyle = textStyle,
        initialFirstVisibleLine = initialFirstVisibleLine,
        initialScrollOffsetX = initialScrollOffsetX,
        scrollPastEnd = scrollPastEnd,
        selection = selection,
        cursor = cursor,
        searchHighlight = searchHighlight,
        inactiveSearchHighlights = inactiveSearchHighlights,
        cursorTarget = cursorTarget,
        interactionOptions = interactionOptions,
        onAnnotationHit = onAnnotationHit,
        onContextMenu = onContextMenu,
        onScrollChange = onScrollChange,
        onViewportChange = onViewportChange,
        gutterOptions = gutterOptions,
        contentOptions = contentOptions,
        decorationOptions = decorationOptions,
        followCursorToken = null,
        overlayContent = null,
    )
}
