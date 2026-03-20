package io.github.dexclub.codeview.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt
import io.github.dexclub.codeview.core.annotation.CodeInteractionTrigger
import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.core.document.DocumentRevision
import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.core.text.CodeTextValue
import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection
import io.github.dexclub.codeview.core.token.CodeTokenKind
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshotFactory
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTokenSpan
import io.github.dexclub.codeview.compose.internal.interaction.resolveTextOffsetForPosition
import io.github.dexclub.codeview.compose.internal.viewport.CodeViewportState
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
    selection: LineSelection? = null,
    cursor: Cursor? = null,
    searchHighlight: LineSelection? = null,
    cursorTarget: CodeViewerCursorTarget? = null,
    interactionOptions: CodeViewerInteractionOptions = CodeViewerInteractionOptions(),
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)? = null,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)? = null,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)? = null,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)? = null,
    textStyle: TextStyle = CodeViewDefaults.CodeTextStyle,
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
        selection = selection,
        cursor = cursor,
        searchHighlight = searchHighlight,
        cursorTarget = cursorTarget,
        interactionOptions = interactionOptions,
        onScrollChange = onScrollChange,
        onViewportChange = onViewportChange,
        onAnnotationHit = onAnnotationHit,
        onContextMenu = onContextMenu,
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
    selection: LineSelection? = null,
    cursor: Cursor? = null,
    searchHighlight: LineSelection? = null,
    cursorTarget: CodeViewerCursorTarget? = null,
    interactionOptions: CodeViewerInteractionOptions = CodeViewerInteractionOptions(),
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)? = null,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)? = null,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)? = null,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)? = null,
    textStyle: TextStyle = CodeViewDefaults.CodeTextStyle,
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
        selection = selection,
        cursor = cursor,
        searchHighlight = searchHighlight,
        cursorTarget = cursorTarget,
        interactionOptions = interactionOptions,
        onScrollChange = onScrollChange,
        onViewportChange = onViewportChange,
        onAnnotationHit = onAnnotationHit,
        onContextMenu = onContextMenu,
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
    selection: LineSelection? = null,
    cursor: Cursor? = null,
    searchHighlight: LineSelection? = null,
    cursorTarget: CodeViewerCursorTarget? = null,
    interactionOptions: CodeViewerInteractionOptions = CodeViewerInteractionOptions(),
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
        selection = selection,
        cursor = cursor,
        searchHighlight = searchHighlight,
        cursorTarget = cursorTarget,
        interactionOptions = interactionOptions,
        onAnnotationHit = onAnnotationHit,
        onContextMenu = onContextMenu,
        onScrollChange = onScrollChange,
        onViewportChange = onViewportChange,
        followCursorToken = null,
        overlayContent = null,
    )
}

@Composable
internal fun CodeViewerCanvas(
    documentKey: DocumentId,
    documentRevision: DocumentRevision,
    layoutSnapshot: CodeLayoutSnapshot,
    modifier: Modifier,
    textStyle: TextStyle,
    initialFirstVisibleLine: Int,
    initialScrollOffsetX: Int,
    selection: LineSelection?,
    cursor: Cursor?,
    searchHighlight: LineSelection?,
    cursorTarget: CodeViewerCursorTarget?,
    interactionOptions: CodeViewerInteractionOptions,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)?,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)?,
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)?,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)?,
    followCursorToken: Long? = null,
    overlayContent: (@Composable (Modifier, CodeViewerCanvasMetrics) -> Unit)?,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val effectiveTextStyle = remember(textStyle) {
        CodeViewDefaults.CodeTextStyle.merge(textStyle)
    }

    val charWidthPx = remember(textMeasurer, effectiveTextStyle) {
        measureAverageCharacterWidthPx(
            textMeasurer = textMeasurer,
            textStyle = effectiveTextStyle,
        )
    }
    val lineHeightPx = remember(textMeasurer, density, effectiveTextStyle) {
        measureAverageLineHeightPx(
            textMeasurer = textMeasurer,
            density = density,
            textStyle = effectiveTextStyle,
        )
    }
    val safeSelection = remember(layoutSnapshot, selection) {
        layoutSnapshot.clampSelection(selection)?.normalized()
    }
    val safeSearchHighlight = remember(layoutSnapshot, searchHighlight) {
        layoutSnapshot.clampSelection(searchHighlight)?.normalized()
    }
    val safeCursor = remember(layoutSnapshot, cursor) {
        layoutSnapshot.clampCursor(cursor)
    }
    val lineMetricsCache = remember(textMeasurer, effectiveTextStyle, layoutSnapshot.text) {
        CodeLineMetricsCache(
            textMeasurer = textMeasurer,
            textStyle = effectiveTextStyle,
            lineContents = layoutSnapshot.lines.map { line -> line.content },
        )
    }

    BoxWithConstraints(modifier = modifier) {
        key(documentKey) {
            val viewportWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(0f)
            val viewportHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(0f)
            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()
            var initialScrollApplied by remember { mutableStateOf(false) }
            val maxHorizontalScrollPx = max(0f, lineMetricsCache.maxLineWidthPx - viewportWidthPx)

            val currentViewport = CodeViewportState(
                firstVisibleLine = if (lineHeightPx > 0f) {
                    (verticalScrollState.value.toFloat() / lineHeightPx).toInt().coerceAtLeast(0)
                } else {
                    0
                },
                horizontalScrollPx = horizontalScrollState.value.toFloat(),
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                lineHeightPx = lineHeightPx,
            ).clamp(layoutSnapshot).clampHorizontalScroll(maxHorizontalScrollPx = maxHorizontalScrollPx)

            LaunchedEffect(
                layoutSnapshot.text,
                lineHeightPx,
                initialFirstVisibleLine,
                initialScrollOffsetX,
            ) {
                if (initialScrollApplied || lineHeightPx <= 0f) return@LaunchedEffect
                verticalScrollState.scrollTo(
                    (initialFirstVisibleLine.coerceAtLeast(0) * lineHeightPx).roundToInt()
                )
                horizontalScrollState.scrollTo(initialScrollOffsetX.coerceAtLeast(0))
                initialScrollApplied = true
            }

            LaunchedEffect(
                cursorTarget?.token,
                cursorTarget?.line,
                cursorTarget?.offset,
                layoutSnapshot.text,
                viewportWidthPx,
                viewportHeightPx,
                charWidthPx,
                lineHeightPx,
            ) {
                if (cursorTarget == null || lineHeightPx <= 0f) return@LaunchedEffect
                val safeTargetCursor = layoutSnapshot.clampCursor(
                    Cursor(
                        line = cursorTarget.line,
                        offset = cursorTarget.offset,
                    )
                ) ?: return@LaunchedEffect
                val targetViewport = currentViewport.revealCursor(
                    layout = layoutSnapshot,
                    cursor = safeTargetCursor,
                    charWidthPx = charWidthPx,
                    cursorHorizontalPx = lineMetricsCache.columnX(safeTargetCursor.line, safeTargetCursor.offset),
                    cursorWidthPx = lineMetricsCache.cursorWidthPx(
                        lineIndex = safeTargetCursor.line,
                        column = safeTargetCursor.offset,
                        fallbackCharWidthPx = charWidthPx,
                    ),
                )
                verticalScrollState.scrollTo((targetViewport.firstVisibleLine * lineHeightPx).roundToInt())
                horizontalScrollState.scrollTo(targetViewport.horizontalScrollPx.roundToInt())
            }

            LaunchedEffect(
                followCursorToken,
                safeCursor?.line,
                safeCursor?.offset,
                layoutSnapshot.text,
                viewportWidthPx,
                viewportHeightPx,
                charWidthPx,
                lineHeightPx,
            ) {
                if (followCursorToken == null || safeCursor == null || lineHeightPx <= 0f) return@LaunchedEffect
                val targetViewport = currentViewport.revealCursor(
                    layout = layoutSnapshot,
                    cursor = safeCursor,
                    charWidthPx = charWidthPx,
                    cursorHorizontalPx = lineMetricsCache.columnX(safeCursor.line, safeCursor.offset),
                    cursorWidthPx = lineMetricsCache.cursorWidthPx(
                        lineIndex = safeCursor.line,
                        column = safeCursor.offset,
                        fallbackCharWidthPx = charWidthPx,
                    ),
                )
                verticalScrollState.scrollTo((targetViewport.firstVisibleLine * lineHeightPx).roundToInt())
                horizontalScrollState.scrollTo(targetViewport.horizontalScrollPx.roundToInt())
            }

            LaunchedEffect(currentViewport.firstVisibleLine, horizontalScrollState.value) {
                onScrollChange?.invoke(
                    currentViewport.firstVisibleLine,
                    horizontalScrollState.value,
                )
            }

            LaunchedEffect(currentViewport.firstVisibleLine, currentViewport.lastVisibleLine(layoutSnapshot.lineCount)) {
                onViewportChange?.invoke(
                    currentViewport.firstVisibleLine,
                    currentViewport.lastVisibleLine(layoutSnapshot.lineCount),
                )
            }

            val contentWidthDp = with(density) {
                max(viewportWidthPx, lineMetricsCache.maxLineWidthPx + charWidthPx).toDp()
            }
            val contentHeightDp = with(density) {
                max(viewportHeightPx, layoutSnapshot.lineCount * lineHeightPx + lineHeightPx).toDp()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
                    .verticalScroll(verticalScrollState),
            ) {
                val contentModifier = Modifier
                    .requiredSize(
                        width = contentWidthDp,
                        height = contentHeightDp,
                    )
                    .annotationInteractionModifier(
                        layoutSnapshot = layoutSnapshot,
                        documentKey = documentKey,
                        documentRevision = documentRevision,
                        charWidthPx = charWidthPx,
                        lineHeightPx = lineHeightPx,
                        interactionOptions = interactionOptions,
                        onAnnotationHit = onAnnotationHit,
                        onContextMenu = onContextMenu,
                    )

                Box(modifier = contentModifier) {
                    Canvas(
                        modifier = Modifier.fillMaxSize(),
                        onDraw = {
                            drawCodeViewerContent(
                                layoutSnapshot = layoutSnapshot,
                                lineMetricsCache = lineMetricsCache,
                                textMeasurer = textMeasurer,
                                textStyle = effectiveTextStyle,
                                lineHeightPx = lineHeightPx,
                                selection = safeSelection,
                                searchHighlight = safeSearchHighlight,
                                cursor = safeCursor,
                                visibleLineRange = currentViewport.visibleLineRange(layoutSnapshot.lineCount),
                            )
                        }
                    )
                    overlayContent?.invoke(
                        Modifier.fillMaxSize(),
                        CodeViewerCanvasMetrics(
                            charWidthPx = charWidthPx,
                            lineHeightPx = lineHeightPx,
                        ),
                    )
                }
            }
        }
    }
}

internal data class CodeViewerCanvasMetrics(
    val charWidthPx: Float,
    val lineHeightPx: Float,
)

private class CodeLineMetricsCache(
    private val textMeasurer: androidx.compose.ui.text.TextMeasurer,
    private val textStyle: TextStyle,
    lineContents: List<String>,
) {
    private val lines: List<String> = lineContents.toList()
    private val lineWidthCache: MutableMap<Int, Float> = mutableMapOf()
    private val columnWidthCache: MutableMap<Int, FloatArray> = mutableMapOf()

    val maxLineWidthPx: Float by lazy(LazyThreadSafetyMode.NONE) {
        lines.indices.maxOfOrNull(::lineWidthPx) ?: 0f
    }

    fun lineWidthPx(lineIndex: Int): Float {
        require(lineIndex in lines.indices) { "lineIndex 超出范围: $lineIndex" }
        return lineWidthCache.getOrPut(lineIndex) {
            columnX(lineIndex, lines[lineIndex].length)
        }
    }

    fun columnX(lineIndex: Int, column: Int): Float {
        require(lineIndex in lines.indices) { "lineIndex 超出范围: $lineIndex" }
        val lineContent = lines[lineIndex]
        val safeColumn = column.coerceIn(0, lineContent.length)
        val cache = columnWidthCache.getOrPut(lineIndex) {
            FloatArray(lineContent.length + 1) { Float.NaN }.also { widths ->
                widths[0] = 0f
            }
        }

        val cachedWidth = cache[safeColumn]
        if (!cachedWidth.isNaN()) {
            return cachedWidth
        }

        var measuredColumn = safeColumn
        while (measuredColumn > 0 && cache[measuredColumn].isNaN()) {
            measuredColumn -= 1
        }

        var widthPx = cache[measuredColumn]
        if (widthPx.isNaN()) {
            widthPx = 0f
            cache[measuredColumn] = widthPx
        }

        for (index in measuredColumn until safeColumn) {
            widthPx += measureTextWidth(lineContent[index].toString())
            cache[index + 1] = widthPx
        }

        return widthPx
    }

    fun cursorWidthPx(
        lineIndex: Int,
        column: Int,
        fallbackCharWidthPx: Float,
    ): Float {
        require(lineIndex in lines.indices) { "lineIndex 超出范围: $lineIndex" }
        val lineContent = lines[lineIndex]
        if (column >= lineContent.length) {
            return fallbackCharWidthPx.coerceAtLeast(1f)
        }
        val currentX = columnX(lineIndex, column)
        val nextX = columnX(lineIndex, column + 1)
        return (nextX - currentX).coerceAtLeast(1f)
    }

    private fun measureTextWidth(text: String): Float {
        if (text.isEmpty()) return 0f
        return textMeasurer.measure(
            text = AnnotatedString(text),
            style = textStyle,
        ).size.width.toFloat()
    }
}

private fun measureAverageCharacterWidthPx(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
): Float {
    val sampleText = "M".repeat(TEXT_METRICS_CHARACTER_SAMPLE_COUNT)
    val measuredWidthPx = textMeasurer.measure(
        text = AnnotatedString(sampleText),
        style = textStyle,
    ).size.width.toFloat()
    return (measuredWidthPx / TEXT_METRICS_CHARACTER_SAMPLE_COUNT).coerceAtLeast(1f)
}

private fun measureAverageLineHeightPx(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    density: androidx.compose.ui.unit.Density,
    textStyle: TextStyle,
): Float {
    val latinSampleText = buildString {
        repeat(TEXT_METRICS_LINE_SAMPLE_COUNT) { index ->
            if (index > 0) append('\n')
            append('M')
        }
    }
    val cjkSampleText = buildString {
        repeat(TEXT_METRICS_LINE_SAMPLE_COUNT) { index ->
            if (index > 0) append('\n')
            append(CJK_LINE_HEIGHT_SAMPLE_CHAR)
        }
    }
    val latinMeasuredHeightPx = textMeasurer.measure(
        text = AnnotatedString(latinSampleText),
        style = textStyle,
    ).size.height.toFloat()
    val cjkMeasuredHeightPx = textMeasurer.measure(
        text = AnnotatedString(cjkSampleText),
        style = textStyle,
    ).size.height.toFloat()
    val averageMeasuredLineHeightPx = max(
        latinMeasuredHeightPx / TEXT_METRICS_LINE_SAMPLE_COUNT,
        cjkMeasuredHeightPx / TEXT_METRICS_LINE_SAMPLE_COUNT,
    )
    val fallbackLineHeightPx = with(density) {
        textStyle.lineHeight.toPx()
    }
    return max(averageMeasuredLineHeightPx, fallbackLineHeightPx).coerceAtLeast(1f)
}

private const val TEXT_METRICS_CHARACTER_SAMPLE_COUNT: Int = 64
private const val TEXT_METRICS_LINE_SAMPLE_COUNT: Int = 32
private const val CJK_LINE_HEIGHT_SAMPLE_CHAR: Char = '国'

internal fun Modifier.annotationInteractionModifier(
    layoutSnapshot: CodeLayoutSnapshot,
    documentKey: DocumentId,
    documentRevision: DocumentRevision,
    charWidthPx: Float,
    lineHeightPx: Float,
    interactionOptions: CodeViewerInteractionOptions,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)?,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)?,
    enablePrimaryClick: Boolean = true,
    enableLongPressContextMenu: Boolean = true,
    enableSecondaryClickContextMenu: Boolean = true,
): Modifier {
    var modifier = this

    if (enableSecondaryClickContextMenu) {
        modifier = modifier.pointerInput(
            layoutSnapshot,
            documentKey,
            documentRevision,
            interactionOptions.annotationTag,
        ) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type != PointerEventType.Press || !event.buttons.isSecondaryPressed) {
                        continue
                    }

                    val position = event.changes.firstOrNull()?.position ?: continue
                    val hit = buildAnnotationHit(
                        layoutSnapshot = layoutSnapshot,
                        documentKey = documentKey,
                        documentRevision = documentRevision,
                        position = position,
                        charWidthPx = charWidthPx,
                        lineHeightPx = lineHeightPx,
                        trigger = CodeInteractionTrigger.ContextMenu,
                    )
                    onContextMenu?.invoke(hit, position)
                }
            }
        }
    }

    if (enablePrimaryClick || enableLongPressContextMenu) {
        modifier = modifier.pointerInput(
            layoutSnapshot,
            documentKey,
            documentRevision,
            interactionOptions.annotationTag,
        ) {
            detectTapGestures(
                onTap = if (enablePrimaryClick) {
                    { offset ->
                        val hit = buildAnnotationHit(
                            layoutSnapshot = layoutSnapshot,
                            documentKey = documentKey,
                            documentRevision = documentRevision,
                            position = offset,
                            charWidthPx = charWidthPx,
                            lineHeightPx = lineHeightPx,
                            trigger = CodeInteractionTrigger.PrimaryClick,
                        )
                        if (hit != null) {
                            onAnnotationHit?.invoke(hit)
                        }
                    }
                } else {
                    null
                },
                onLongPress = if (enableLongPressContextMenu) {
                    { offset ->
                        val hit = buildAnnotationHit(
                            layoutSnapshot = layoutSnapshot,
                            documentKey = documentKey,
                            documentRevision = documentRevision,
                            position = offset,
                            charWidthPx = charWidthPx,
                            lineHeightPx = lineHeightPx,
                            trigger = CodeInteractionTrigger.ContextMenu,
                        )
                        onContextMenu?.invoke(hit, offset)
                    }
                } else {
                    null
                },
            )
        }
    }

    return modifier
}

private fun DrawScope.drawCodeViewerContent(
    layoutSnapshot: CodeLayoutSnapshot,
    lineMetricsCache: CodeLineMetricsCache,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
    lineHeightPx: Float,
    selection: LineSelection?,
    searchHighlight: LineSelection?,
    cursor: Cursor?,
    visibleLineRange: IntRange,
) {
    val selectionColor = Color(0x334096FF)
    val searchHighlightColor = Color(0x40F4D03F)
    val cursorColor = Color(0xFF1F2328)

    for (lineIndex in visibleLineRange) {
        val line = layoutSnapshot.lineAt(lineIndex)
        val lineTop = lineIndex * lineHeightPx

        drawSelectionRange(
            lineIndex = lineIndex,
            lineLength = line.length,
            selection = searchHighlight,
            color = searchHighlightColor,
            lineMetricsCache = lineMetricsCache,
            lineTop = lineTop,
            lineHeightPx = lineHeightPx,
        )
        drawSelectionRange(
            lineIndex = lineIndex,
            lineLength = line.length,
            selection = selection,
            color = selectionColor,
            lineMetricsCache = lineMetricsCache,
            lineTop = lineTop,
            lineHeightPx = lineHeightPx,
        )

        drawCodeLineText(
            lineIndex = lineIndex,
            lineContent = line.content,
            lineTokens = layoutSnapshot.tokensForLine(lineIndex),
            lineMetricsCache = lineMetricsCache,
            textMeasurer = textMeasurer,
            textStyle = textStyle,
            lineTop = lineTop,
        )

        if (cursor != null && cursor.line == lineIndex) {
            val cursorX = lineMetricsCache.columnX(lineIndex, cursor.offset)
            drawLine(
                color = cursorColor,
                start = Offset(cursorX, lineTop + 2.dp.toPx()),
                end = Offset(cursorX, lineTop + lineHeightPx - 2.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}

private fun DrawScope.drawSelectionRange(
    lineIndex: Int,
    lineLength: Int,
    selection: LineSelection?,
    color: Color,
    lineMetricsCache: CodeLineMetricsCache,
    lineTop: Float,
    lineHeightPx: Float,
) {
    if (selection == null) return
    if (selection.isCollapsed) return
    if (lineIndex < selection.startLine || lineIndex > selection.endLine) return

    val startColumn = when {
        lineIndex == selection.startLine -> selection.startOffset
        else -> 0
    }.coerceIn(0, lineLength)
    val endColumn = when {
        lineIndex == selection.endLine -> selection.endOffset
        else -> lineLength
    }.coerceIn(startColumn, lineLength)

    if (startColumn == endColumn) return

    val left = lineMetricsCache.columnX(lineIndex, startColumn)
    val right = lineMetricsCache.columnX(lineIndex, endColumn)
    val width = (right - left).coerceAtLeast(0f)
    drawRoundRect(
        color = color,
        topLeft = Offset(left, lineTop + 1.dp.toPx()),
        size = Size(width, lineHeightPx - 2.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
    )
}

private fun DrawScope.drawCodeLineText(
    lineIndex: Int,
    lineContent: String,
    lineTokens: List<CodeLineTokenSpan>,
    lineMetricsCache: CodeLineMetricsCache,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
    lineTop: Float,
) {
    if (lineContent.isEmpty()) return

    var currentColumn = 0

    lineTokens.forEach { token ->
        if (token.startColumn > currentColumn) {
            drawTextSegment(
                text = lineContent.substring(currentColumn, token.startColumn),
                column = currentColumn,
                color = textStyle.color,
                lineIndex = lineIndex,
                lineMetricsCache = lineMetricsCache,
                textMeasurer = textMeasurer,
                textStyle = textStyle,
                lineTop = lineTop,
            )
        }

        drawTextSegment(
            text = lineContent.substring(token.startColumn, token.endColumn),
            column = token.startColumn,
            color = tokenColor(token.kind),
            lineIndex = lineIndex,
            lineMetricsCache = lineMetricsCache,
            textMeasurer = textMeasurer,
            textStyle = textStyle,
            lineTop = lineTop,
        )
        currentColumn = token.endColumn
    }

    if (currentColumn < lineContent.length) {
        drawTextSegment(
            text = lineContent.substring(currentColumn),
            column = currentColumn,
            color = textStyle.color,
            lineIndex = lineIndex,
            lineMetricsCache = lineMetricsCache,
            textMeasurer = textMeasurer,
            textStyle = textStyle,
            lineTop = lineTop,
        )
    }
}

private fun DrawScope.drawTextSegment(
    text: String,
    column: Int,
    color: Color,
    lineIndex: Int,
    lineMetricsCache: CodeLineMetricsCache,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
    lineTop: Float,
) {
    if (text.isEmpty()) return
    drawText(
        textMeasurer = textMeasurer,
        text = AnnotatedString(text),
        style = textStyle.copy(color = color),
        topLeft = Offset(
            x = lineMetricsCache.columnX(lineIndex, column),
            y = lineTop,
        ),
    )
}

private fun tokenColor(kind: CodeTokenKind): Color {
    return when (kind) {
        CodeTokenKind.Keyword,
        CodeTokenKind.KeywordModifier,
        CodeTokenKind.KeywordType -> Color(0xFF7C3AED)

        CodeTokenKind.StringLiteral,
        CodeTokenKind.EscapeSequence,
        CodeTokenKind.Interpolation -> Color(0xFF0A7F5A)

        CodeTokenKind.NumberLiteral,
        CodeTokenKind.BooleanLiteral,
        CodeTokenKind.NullLiteral -> Color(0xFF0550AE)

        CodeTokenKind.Comment -> Color(0xFF6E7781)
        CodeTokenKind.TypeName,
        CodeTokenKind.Annotation -> Color(0xFFB35900)

        CodeTokenKind.FunctionName,
        CodeTokenKind.VariableName,
        CodeTokenKind.PropertyName,
        CodeTokenKind.ParameterName,
        CodeTokenKind.ConstantName,
        CodeTokenKind.LabelName,
        CodeTokenKind.Namespace,
        CodeTokenKind.Builtin -> Color(0xFF1F2328)

        CodeTokenKind.Operator,
        CodeTokenKind.Punctuation -> Color(0xFF57606A)

        CodeTokenKind.Invalid -> Color(0xFFCF222E)
        CodeTokenKind.PlainText -> Color(0xFF1F2328)
    }
}

private fun buildAnnotationHit(
    layoutSnapshot: CodeLayoutSnapshot,
    documentKey: DocumentId,
    documentRevision: DocumentRevision,
    position: Offset,
    charWidthPx: Float,
    lineHeightPx: Float,
    trigger: CodeInteractionTrigger,
): CodeAnnotationHit? {
    val offset = resolveTextOffsetForPosition(
        layoutSnapshot = layoutSnapshot,
        position = position,
        charWidthPx = charWidthPx,
        lineHeightPx = lineHeightPx,
        clampToLineEnd = false,
    ) ?: return null
    val annotation = layoutSnapshot.findAnnotationAtOffset(offset) ?: return null

    return CodeAnnotationHit(
        annotation = annotation,
        range = annotation.range,
        trigger = trigger,
        documentId = documentKey,
        revision = documentRevision,
    )
}
