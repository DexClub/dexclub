package io.github.dexclub.codeview.compose

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshotFactory
import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.core.document.DocumentRevision
import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.core.text.CodeSelection
import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection
import io.github.dexclub.codeview.language.addon.CodeAddons
import io.github.dexclub.codeview.runtime.CodeRuntime

@CodeViewApi
@Composable
public fun CodeEditor(
    initialText: String,
    languageId: CodeLanguageId,
    addons: CodeAddons,
    modifier: Modifier = Modifier,
    onTextChange: ((String) -> Unit)? = null,
    runtime: CodeRuntime = remember { CodeRuntime() },
    readOnly: Boolean = false,
    selection: LineSelection? = null,
    cursor: Cursor? = null,
    searchHighlight: LineSelection? = null,
    initialFirstVisibleLine: Int = 0,
    initialScrollOffsetX: Int = 0,
    cursorTarget: CodeViewerCursorTarget? = null,
    interactionOptions: CodeViewerInteractionOptions = CodeViewerInteractionOptions(),
    onSelectionChange: ((LineSelection?) -> Unit)? = null,
    onCursorChange: ((Cursor?) -> Unit)? = null,
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)? = null,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)? = null,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)? = null,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)? = null,
    textStyle: TextStyle = CodeViewDefaults.CodeTextStyle,
) {
    var text by remember { mutableStateOf(initialText) }

    val document = remember(languageId) {
        CodeDocument.create(languageId, initialText)
    }

    LaunchedEffect(text) {
        if (document.snapshots.value.text != text) {
            document.update(text)
        }
    }

    CodeEditor(
        document = document,
        addons = addons,
        modifier = modifier,
        runtime = runtime,
        textStyle = textStyle,
        readOnly = readOnly,
        selection = selection,
        cursor = cursor,
        searchHighlight = searchHighlight,
        initialFirstVisibleLine = initialFirstVisibleLine,
        initialScrollOffsetX = initialScrollOffsetX,
        cursorTarget = cursorTarget,
        interactionOptions = interactionOptions,
        onTextChange = { newText ->
            text = newText
            onTextChange?.invoke(newText)
        },
        onSelectionChange = onSelectionChange,
        onCursorChange = onCursorChange,
        onScrollChange = onScrollChange,
        onViewportChange = onViewportChange,
        onAnnotationHit = onAnnotationHit,
        onContextMenu = onContextMenu,
    )
}

@CodeViewApi
@Composable
public fun CodeEditor(
    text: String,
    onTextChange: (String) -> Unit,
    languageId: CodeLanguageId,
    addons: CodeAddons,
    modifier: Modifier = Modifier,
    runtime: CodeRuntime = remember { CodeRuntime() },
    readOnly: Boolean = false,
    selection: LineSelection? = null,
    cursor: Cursor? = null,
    searchHighlight: LineSelection? = null,
    initialFirstVisibleLine: Int = 0,
    initialScrollOffsetX: Int = 0,
    cursorTarget: CodeViewerCursorTarget? = null,
    interactionOptions: CodeViewerInteractionOptions = CodeViewerInteractionOptions(),
    onSelectionChange: ((LineSelection?) -> Unit)? = null,
    onCursorChange: ((Cursor?) -> Unit)? = null,
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)? = null,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)? = null,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)? = null,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)? = null,
    textStyle: TextStyle = CodeViewDefaults.CodeTextStyle,
) {
    val document = remember(languageId) {
        CodeDocument.create(languageId, text)
    }

    LaunchedEffect(text) {
        if (document.snapshots.value.text != text) {
            document.update(text)
        }
    }

    CodeEditor(
        document = document,
        addons = addons,
        modifier = modifier,
        runtime = runtime,
        textStyle = textStyle,
        readOnly = readOnly,
        selection = selection,
        cursor = cursor,
        searchHighlight = searchHighlight,
        initialFirstVisibleLine = initialFirstVisibleLine,
        initialScrollOffsetX = initialScrollOffsetX,
        cursorTarget = cursorTarget,
        interactionOptions = interactionOptions,
        onTextChange = onTextChange,
        onSelectionChange = onSelectionChange,
        onCursorChange = onCursorChange,
        onScrollChange = onScrollChange,
        onViewportChange = onViewportChange,
        onAnnotationHit = onAnnotationHit,
        onContextMenu = onContextMenu,
    )
}

@CodeViewApi
@Composable
public fun CodeEditor(
    document: CodeDocument,
    addons: CodeAddons,
    modifier: Modifier = Modifier,
    runtime: CodeRuntime = remember { CodeRuntime() },
    readOnly: Boolean = false,
    selection: LineSelection? = null,
    cursor: Cursor? = null,
    searchHighlight: LineSelection? = null,
    initialFirstVisibleLine: Int = 0,
    initialScrollOffsetX: Int = 0,
    cursorTarget: CodeViewerCursorTarget? = null,
    interactionOptions: CodeViewerInteractionOptions = CodeViewerInteractionOptions(),
    onTextChange: ((String) -> Unit)? = null,
    onSelectionChange: ((LineSelection?) -> Unit)? = null,
    onCursorChange: ((Cursor?) -> Unit)? = null,
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)? = null,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)? = null,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)? = null,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)? = null,
    textStyle: TextStyle = CodeViewDefaults.CodeTextStyle,
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

    val documentTextLayoutSnapshot = remember(snapshot.text) {
        CodeLayoutSnapshotFactory.create(snapshot.text)
    }
    val documentLayoutSnapshot = remember(documentTextLayoutSnapshot, tokens, annotations) {
        CodeLayoutSnapshotFactory.withDecorations(
            base = documentTextLayoutSnapshot,
            tokens = tokens,
            annotations = annotations,
        )
    }
    val externalSelection = remember(documentLayoutSnapshot, selection, cursor) {
        resolveExternalSelection(
            layoutSnapshot = documentLayoutSnapshot,
            selection = selection,
            cursor = cursor,
        )
    }
    var fieldValue by remember(document.documentId) {
        mutableStateOf(
            TextFieldValue(
                text = snapshot.text,
                selection = externalSelection?.toTextRange() ?: TextRange(snapshot.text.length),
            )
        )
    }
    var followCursorToken by remember(document.documentId) {
        mutableStateOf(0L)
    }
    val editorTextLayoutSnapshot = remember(fieldValue.text) {
        CodeLayoutSnapshotFactory.create(fieldValue.text)
    }
    val editorLayoutSnapshot = remember(
        readOnly,
        editorTextLayoutSnapshot,
        snapshot.text,
        tokens,
        annotations,
    ) {
        when {
            readOnly -> documentLayoutSnapshot
            fieldValue.text == snapshot.text -> CodeLayoutSnapshotFactory.withDecorations(
                base = editorTextLayoutSnapshot,
                tokens = tokens,
                annotations = annotations,
            )

            else -> editorTextLayoutSnapshot
        }
    }

    LaunchedEffect(snapshot.text, externalSelection?.anchorOffset, externalSelection?.caretOffset) {
        val nextValue = when {
            externalSelection != null -> fieldValue.copy(
                text = snapshot.text,
                selection = externalSelection.toTextRange(),
                composition = clampTextRangeOrNull(
                    range = fieldValue.composition,
                    textLength = snapshot.text.length,
                ),
            )

            fieldValue.text == snapshot.text -> fieldValue.copy(
                selection = clampTextRange(
                    range = fieldValue.selection,
                    textLength = snapshot.text.length,
                ),
                composition = clampTextRangeOrNull(
                    range = fieldValue.composition,
                    textLength = snapshot.text.length,
                ),
            )

            else -> TextFieldValue(
                text = snapshot.text,
                selection = TextRange(snapshot.text.length),
            )
        }
        if (fieldValue != nextValue) {
            fieldValue = nextValue
        }
    }

    val effectiveSelection = remember(editorLayoutSnapshot, fieldValue.selection) {
        editorLayoutSnapshot.codeSelectionToLineSelection(fieldValue.selection.toCodeSelection())
    }
    val effectiveCursor = remember(editorLayoutSnapshot, fieldValue.selection, readOnly, cursor) {
        when {
            readOnly -> cursor
            else -> editorLayoutSnapshot.cursorFromSelection(fieldValue.selection.toCodeSelection())
        }
    }

    CodeEditorContent(
        documentId = document.documentId,
        documentRevision = snapshot.revision,
        layoutSnapshot = editorLayoutSnapshot,
        modifier = modifier,
        textStyle = textStyle,
        readOnly = readOnly,
        searchHighlight = searchHighlight,
        initialFirstVisibleLine = initialFirstVisibleLine,
        initialScrollOffsetX = initialScrollOffsetX,
        selection = if (readOnly) selection else effectiveSelection,
        cursor = if (readOnly) cursor else effectiveCursor,
        cursorTarget = cursorTarget,
        interactionOptions = interactionOptions,
        onScrollChange = onScrollChange,
        onViewportChange = onViewportChange,
        onAnnotationHit = onAnnotationHit,
        onContextMenu = onContextMenu,
        followCursorToken = if (readOnly || followCursorToken == 0L) null else followCursorToken,
        fieldValue = fieldValue,
        onFieldValueChange = { newValue ->
            fieldValue = newValue
            followCursorToken += 1L
            val textChanged = snapshot.text != newValue.text
            if (textChanged) {
                document.update(newValue.text)
            }
            if (textChanged) {
                onTextChange?.invoke(newValue.text)
            }

            val selectionLayoutSnapshot = when {
                newValue.text == editorLayoutSnapshot.text -> editorLayoutSnapshot
                else -> CodeLayoutSnapshotFactory.create(newValue.text)
            }
            val codeSelection = newValue.selection.toCodeSelection()
            onSelectionChange?.invoke(selectionLayoutSnapshot.codeSelectionToLineSelection(codeSelection))
            onCursorChange?.invoke(selectionLayoutSnapshot.cursorFromSelection(codeSelection))
        },
    )
}

@Composable
private fun CodeEditorContent(
    documentId: DocumentId,
    documentRevision: DocumentRevision,
    layoutSnapshot: CodeLayoutSnapshot,
    modifier: Modifier,
    textStyle: TextStyle,
    readOnly: Boolean,
    searchHighlight: LineSelection?,
    initialFirstVisibleLine: Int,
    initialScrollOffsetX: Int,
    selection: LineSelection?,
    cursor: Cursor?,
    cursorTarget: CodeViewerCursorTarget?,
    interactionOptions: CodeViewerInteractionOptions,
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)?,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)?,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)?,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)?,
    followCursorToken: Long?,
    fieldValue: TextFieldValue,
    onFieldValueChange: (TextFieldValue) -> Unit,
) {
    CodeViewerCanvas(
        documentKey = documentId,
        documentRevision = documentRevision,
        layoutSnapshot = layoutSnapshot,
        modifier = modifier,
        textStyle = textStyle,
        initialFirstVisibleLine = initialFirstVisibleLine,
        initialScrollOffsetX = initialScrollOffsetX,
        selection = selection,
        cursor = cursor,
        searchHighlight = searchHighlight,
        cursorTarget = cursorTarget,
        interactionOptions = interactionOptions,
        onAnnotationHit = onAnnotationHit,
        onContextMenu = onContextMenu,
        onScrollChange = onScrollChange,
        onViewportChange = onViewportChange,
        followCursorToken = followCursorToken,
        overlayContent = if (readOnly) {
            null
        } else {
            { overlayModifier, canvasMetrics ->
                val selectionColors = remember {
                    TextSelectionColors(
                        handleColor = Color(0xFF4096FF),
                        backgroundColor = Color.Transparent,
                    )
                }
                CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                    BasicTextField(
                        value = fieldValue,
                        onValueChange = onFieldValueChange,
                        modifier = overlayModifier.annotationInteractionModifier(
                            layoutSnapshot = layoutSnapshot,
                            documentKey = documentId,
                            documentRevision = documentRevision,
                            charWidthPx = canvasMetrics.charWidthPx,
                            lineHeightPx = canvasMetrics.lineHeightPx,
                            interactionOptions = interactionOptions,
                            onAnnotationHit = onAnnotationHit,
                            onContextMenu = onContextMenu,
                            enablePrimaryClick = false,
                            enableLongPressContextMenu = false,
                            enableSecondaryClickContextMenu = true,
                        ),
                        readOnly = false,
                        singleLine = false,
                        minLines = layoutSnapshot.lineCount.coerceAtLeast(1),
                        maxLines = Int.MAX_VALUE,
                        textStyle = CodeViewDefaults.CodeTextStyle
                            .merge(textStyle)
                            .copy(color = Color.Transparent),
                        cursorBrush = SolidColor(Color.Transparent),
                    )
                }
            }
        },
    )
}

private fun resolveExternalSelection(
    layoutSnapshot: CodeLayoutSnapshot,
    selection: LineSelection?,
    cursor: Cursor?,
): CodeSelection? {
    val safeSelection = layoutSnapshot.clampSelection(selection)
    val safeCursor = layoutSnapshot.clampCursor(cursor)

    if (safeSelection == null) {
        return safeCursor?.let { safe -> CodeSelection.collapsed(layoutSnapshot.cursorToOffset(safe)) }
    }

    val normalizedSelection = safeSelection.normalized()
    val startOffset = layoutSnapshot.positionToOffset(
        normalizedSelection.startLine,
        normalizedSelection.startOffset,
    )
    val endOffset = layoutSnapshot.positionToOffset(
        normalizedSelection.endLine,
        normalizedSelection.endOffset,
    )
    val cursorOffset = safeCursor?.let(layoutSnapshot::cursorToOffset)

    return when {
        cursorOffset == null -> CodeSelection(
            anchorOffset = startOffset,
            caretOffset = endOffset,
        )

        cursorOffset == startOffset && startOffset != endOffset -> CodeSelection(
            anchorOffset = endOffset,
            caretOffset = startOffset,
        )

        else -> CodeSelection(
            anchorOffset = startOffset,
            caretOffset = endOffset,
        )
    }
}

private fun TextRange.toCodeSelection(): CodeSelection =
    CodeSelection(
        anchorOffset = start,
        caretOffset = end,
    )

private fun CodeSelection.toTextRange(): TextRange = TextRange(
    start = anchorOffset,
    end = caretOffset,
)

private fun clampTextRange(
    range: TextRange,
    textLength: Int,
): TextRange {
    return TextRange(
        start = range.start.coerceIn(0, textLength),
        end = range.end.coerceIn(0, textLength),
    )
}

private fun clampTextRangeOrNull(
    range: TextRange?,
    textLength: Int,
): TextRange? = range?.let {
    clampTextRange(
        range = it,
        textLength = textLength,
    )
}
