package io.github.dexclub.codeview.compose.internal.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import kotlin.math.max
import kotlin.math.roundToInt
import io.github.dexclub.codeview.compose.CodeViewDefaults
import io.github.dexclub.codeview.compose.CodeViewerCursorTarget
import io.github.dexclub.codeview.compose.CodeViewerInteractionOptions
import io.github.dexclub.codeview.compose.internal.editor.CodeEditorComposingOverlay
import io.github.dexclub.codeview.compose.internal.editor.normalizedStart
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.compose.internal.viewport.CodeViewportState
import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.core.document.DocumentRevision
import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection

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
    composingOverlay: CodeEditorComposingOverlay? = null,
    cursorTarget: CodeViewerCursorTarget?,
    interactionOptions: CodeViewerInteractionOptions,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)?,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)?,
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)?,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)?,
    followCursorToken: Long? = null,
    enablePrimaryAnnotationClick: Boolean = true,
    enableLongPressContextMenu: Boolean = true,
    enableSecondaryClickContextMenu: Boolean = true,
    contentModifierTransform: ((Modifier, CodeViewerCanvasMetrics, CodeLineTextLayoutCache) -> Modifier)? = null,
    underlayContent: (@Composable (Modifier, CodeViewerCanvasMetrics, CodeLineTextLayoutCache) -> Unit)? = null,
    floatingUnderlayContent: (@Composable (CodeViewerCanvasMetrics, CodeLineTextLayoutCache, CodeViewerViewportSnapshot) -> Unit)? = null,
    floatingContent: (@Composable (CodeViewerCanvasMetrics, CodeLineTextLayoutCache, CodeViewerViewportSnapshot) -> Unit)? = null,
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
    val cursorAlpha = rememberCodeViewerCursorAlpha(
        cursorVisible = safeCursor != null,
        resetKey = listOf(
            safeCursor?.line,
            safeCursor?.offset,
            followCursorToken,
            layoutSnapshot.text.length,
        ),
    )
    val lineLayoutCache = remember(textMeasurer, effectiveTextStyle, layoutSnapshot) {
        CodeLineTextLayoutCache(
            textMeasurer = textMeasurer,
            textStyle = effectiveTextStyle,
            layoutSnapshot = layoutSnapshot,
        )
    }

    BoxWithConstraints(modifier = modifier) {
        key(documentKey) {
            val viewportWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(0f)
            val viewportHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(0f)
            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()
            var initialScrollApplied by remember { mutableStateOf(false) }
            val maxHorizontalScrollPx = max(0f, lineLayoutCache.maxLineWidthPx - viewportWidthPx)

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
                    cursorHorizontalPx = lineLayoutCache.columnX(safeTargetCursor.line, safeTargetCursor.offset),
                    cursorWidthPx = lineLayoutCache.cursorWidthPx(
                        lineIndex = safeTargetCursor.line,
                        column = safeTargetCursor.offset,
                        fallbackCharWidthPx = charWidthPx,
                    ),
                    maxHorizontalScrollPx = maxHorizontalScrollPx,
                )
                verticalScrollState.scrollTo((targetViewport.firstVisibleLine * lineHeightPx).roundToInt())
                horizontalScrollState.scrollTo(targetViewport.horizontalScrollPx.roundToInt())
            }

            LaunchedEffect(
                followCursorToken,
                safeCursor?.line,
                safeCursor?.offset,
                composingOverlay?.anchorSelection?.start,
                composingOverlay?.anchorSelection?.end,
                composingOverlay?.imeFieldValue?.text,
                composingOverlay?.imeFieldValue?.selection?.start,
                composingOverlay?.imeFieldValue?.selection?.end,
                layoutSnapshot.text,
                viewportWidthPx,
                viewportHeightPx,
                charWidthPx,
                lineHeightPx,
            ) {
                if (lineHeightPx <= 0f) return@LaunchedEffect
                val composingRevealTarget = resolveComposingRevealTarget(
                    layoutSnapshot = layoutSnapshot,
                    lineLayoutCache = lineLayoutCache,
                    composingOverlay = composingOverlay,
                )
                val revealCursor = when {
                    composingRevealTarget != null -> Cursor(
                        line = composingRevealTarget.lineIndex,
                        offset = composingRevealTarget.anchorColumn,
                    )
                    followCursorToken != null && safeCursor != null -> safeCursor
                    else -> null
                } ?: return@LaunchedEffect
                val targetViewport = currentViewport.revealCursor(
                    layout = layoutSnapshot,
                    cursor = revealCursor,
                    charWidthPx = charWidthPx,
                    cursorHorizontalPx = composingRevealTarget?.xPx ?: lineLayoutCache.columnX(revealCursor.line, revealCursor.offset),
                    cursorWidthPx = composingRevealTarget?.widthPx ?: lineLayoutCache.cursorWidthPx(
                        lineIndex = revealCursor.line,
                        column = revealCursor.offset,
                        fallbackCharWidthPx = charWidthPx,
                    ),
                    maxHorizontalScrollPx = maxHorizontalScrollPx,
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
                max(viewportWidthPx, lineLayoutCache.maxLineWidthPx + charWidthPx).toDp()
            }
            val contentHeightDp = with(density) {
                max(viewportHeightPx, layoutSnapshot.lineCount * lineHeightPx + lineHeightPx).toDp()
            }

            val canvasMetrics = CodeViewerCanvasMetrics(
                charWidthPx = charWidthPx,
                lineHeightPx = lineHeightPx,
            )
            val viewportSnapshot = CodeViewerViewportSnapshot(
                verticalScrollPx = verticalScrollState.value.toFloat(),
                horizontalScrollPx = horizontalScrollState.value.toFloat(),
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                lineHeightPx = lineHeightPx,
            )

            floatingUnderlayContent?.invoke(
                canvasMetrics,
                lineLayoutCache,
                viewportSnapshot,
            )

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
                        lineLayoutCache = lineLayoutCache,
                        documentKey = documentKey,
                        documentRevision = documentRevision,
                        lineHeightPx = lineHeightPx,
                        interactionOptions = interactionOptions,
                        onAnnotationHit = onAnnotationHit,
                        onContextMenu = onContextMenu,
                        enablePrimaryClick = enablePrimaryAnnotationClick,
                        enableLongPressContextMenu = enableLongPressContextMenu,
                        enableSecondaryClickContextMenu = enableSecondaryClickContextMenu,
                    )
                val editorAwareContentModifier = contentModifierTransform?.invoke(
                    contentModifier,
                    canvasMetrics,
                    lineLayoutCache,
                ) ?: contentModifier

                Box(modifier = editorAwareContentModifier) {
                    underlayContent?.invoke(
                        Modifier.fillMaxSize(),
                        canvasMetrics,
                        lineLayoutCache,
                    )
                    Canvas(
                        modifier = Modifier.fillMaxSize(),
                        onDraw = {
                            drawCodeViewerContent(
                                layoutSnapshot = layoutSnapshot,
                                lineLayoutCache = lineLayoutCache,
                                lineHeightPx = lineHeightPx,
                                selection = safeSelection,
                                searchHighlight = safeSearchHighlight,
                                cursor = safeCursor,
                                composingOverlay = composingOverlay,
                                cursorAlpha = cursorAlpha,
                                visibleLineRange = currentViewport.visibleLineRange(layoutSnapshot.lineCount),
                            )
                        }
                    )
                    overlayContent?.invoke(
                        Modifier.fillMaxSize(),
                        canvasMetrics,
                    )
                }
            }

            floatingContent?.invoke(
                canvasMetrics,
                lineLayoutCache,
                viewportSnapshot,
            )
        }
    }
}

internal data class CodeViewerCanvasMetrics(
    val charWidthPx: Float,
    val lineHeightPx: Float,
)

internal data class CodeViewerViewportSnapshot(
    val verticalScrollPx: Float,
    val horizontalScrollPx: Float,
    val viewportWidthPx: Float,
    val viewportHeightPx: Float,
    val lineHeightPx: Float,
)

private data class ComposingRevealTarget(
    val lineIndex: Int,
    val anchorColumn: Int,
    val xPx: Float,
    val widthPx: Float,
)

private fun resolveComposingRevealTarget(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    composingOverlay: CodeEditorComposingOverlay?,
): ComposingRevealTarget? {
    val overlay = composingOverlay ?: return null
    val overlayText = overlay.imeFieldValue.text
    if (overlayText.isEmpty()) return null
    if (overlayText.contains('\n') || overlayText.contains('\r')) return null

    val anchorOffset = overlay.anchorSelection.normalizedStart
    val anchorPosition = layoutSnapshot.offsetToPosition(anchorOffset)
    val overlayLayout = lineLayoutCache.plainTextLayout(overlayText)
    val caretOffset = overlay.imeFieldValue.selection.end.coerceIn(0, overlayText.length)
    val anchorX = lineLayoutCache.columnX(anchorPosition.lineIndex, anchorPosition.columnIndex)
    val caretX = anchorX + overlayLayout.getCursorRect(caretOffset).left
    return ComposingRevealTarget(
        lineIndex = anchorPosition.lineIndex,
        anchorColumn = anchorPosition.columnIndex,
        xPx = caretX,
        widthPx = lineLayoutCache.cursorWidthPx(
            lineIndex = anchorPosition.lineIndex,
            column = anchorPosition.columnIndex,
            fallbackCharWidthPx = 1f,
        ),
    )
}

private fun measureAverageCharacterWidthPx(
    textMeasurer: TextMeasurer,
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
    textMeasurer: TextMeasurer,
    density: Density,
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
