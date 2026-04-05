package io.github.dexclub.codeview.compose.internal.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import io.github.dexclub.codeview.compose.CodeContentOptions
import io.github.dexclub.codeview.compose.CodeDecorationOptions
import io.github.dexclub.codeview.compose.CodeViewDefaults
import io.github.dexclub.codeview.compose.CodeGutterOptions
import io.github.dexclub.codeview.compose.CodeViewerCursorTarget
import io.github.dexclub.codeview.compose.CodeViewerInteractionOptions
import io.github.dexclub.codeview.compose.rememberPlatformImeBottomInsetPx
import io.github.dexclub.codeview.compose.internal.editor.CodeEditorComposingOverlay
import io.github.dexclub.codeview.compose.internal.editor.normalizedStart
import io.github.dexclub.codeview.compose.internal.layout.CodeLayoutSnapshot
import io.github.dexclub.codeview.compose.internal.layout.CodeLineTextLayoutCache
import io.github.dexclub.codeview.compose.internal.viewport.CodeViewportState
import io.github.dexclub.codeview.compose.internal.viewport.CodeViewerVerticalScrollState
import io.github.dexclub.codeview.compose.internal.viewport.rememberCodeViewerVerticalScrollState
import io.github.dexclub.codeview.compose.internal.viewport.resolveCodeViewerVerticalLayout
import io.github.dexclub.codeview.compose.internal.viewport.resolveCodeViewerViewportState
import io.github.dexclub.codeview.core.annotation.CodeAnnotationHit
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.core.document.DocumentRevision
import io.github.dexclub.codeview.core.text.Cursor
import io.github.dexclub.codeview.core.text.LineSelection
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun CodeViewerCanvas(
    documentKey: DocumentId,
    documentRevision: DocumentRevision,
    layoutSnapshot: CodeLayoutSnapshot,
    modifier: Modifier,
    textStyle: TextStyle,
    initialFirstVisibleLine: Int,
    initialScrollOffsetX: Int,
    scrollPastEnd: Int,
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
    gutterOptions: CodeGutterOptions,
    contentOptions: CodeContentOptions,
    decorationOptions: CodeDecorationOptions,
    followCursorToken: Long? = null,
    enablePrimaryAnnotationClick: Boolean = true,
    enableLongPressContextMenu: Boolean = true,
    enableSecondaryClickContextMenu: Boolean = true,
    contentModifierTransform: ((Modifier, CodeViewerCanvasMetrics, CodeLineTextLayoutCache, CodeViewerScrollController) -> Modifier)? = null,
    underlayContent: (@Composable (Modifier, CodeViewerCanvasMetrics, CodeLineTextLayoutCache) -> Unit)? = null,
    floatingUnderlayContent: (@Composable (CodeViewerCanvasMetrics, CodeLineTextLayoutCache, CodeViewerViewportSnapshot) -> Unit)? = null,
    floatingContent: (@Composable (CodeViewerCanvasMetrics, CodeLineTextLayoutCache, CodeViewerViewportSnapshot, CodeViewerScrollController) -> Unit)? = null,
    overlayContent: (@Composable (Modifier, CodeViewerCanvasMetrics) -> Unit)?,
) {
    val density = LocalDensity.current
    val imeBottomInsetPx = rememberPlatformImeBottomInsetPx()
    val textMeasurer = rememberTextMeasurer()
    val effectiveTextStyle = remember(textStyle) {
        CodeViewDefaults.CodeTextStyle.merge(textStyle)
    }

    val textMetrics = remember(textMeasurer, density, effectiveTextStyle) {
        measureCodeViewerTextMetrics(
            textMeasurer = textMeasurer,
            density = density,
            textStyle = effectiveTextStyle,
        )
    }
    val charWidthPx = textMetrics.charWidthPx
    val lineHeightPx = textMetrics.profile.lineHeightPx
    val contentHeightPx = textMetrics.profile.contentHeightPx
    val contentTopPaddingPx = textMetrics.profile.contentTopPaddingPx
    val baselinePx = textMetrics.profile.baselinePx
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
    val lineLayoutCache = remember(textMeasurer, effectiveTextStyle, documentKey) {
        CodeLineTextLayoutCache(
            textMeasurer = textMeasurer,
            textStyle = effectiveTextStyle,
            layoutSnapshot = layoutSnapshot,
        )
    }
    lineLayoutCache.updateLayoutSnapshot(layoutSnapshot)
    val contentOverscrollEffect = rememberCodeViewerHandleOverscrollEffect()
    val horizontalScrollOverscrollEffect = remember(contentOverscrollEffect) {
        contentOverscrollEffect.withoutVisualEffect()
    }
    val verticalScrollOverscrollEffect = remember(contentOverscrollEffect) {
        contentOverscrollEffect.withoutVisualEffect()
    }

    BoxWithConstraints(modifier = modifier) {
        key(documentKey) {
            val viewportWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(0f)
            val viewportHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(0f)
            contentOverscrollEffect.viewportWidthPx = viewportWidthPx
            contentOverscrollEffect.viewportHeightPx = viewportHeightPx
            val horizontalScrollState = rememberScrollState()
            val verticalScrollState = rememberCodeViewerVerticalScrollState()
            var latestViewportWidthPx by remember { mutableStateOf(0f) }
            var latestContentViewportWidthPx by remember { mutableStateOf(0f) }
            var latestViewportHeightPx by remember { mutableStateOf(0f) }
            var maxObservedViewportHeightPx by remember { mutableStateOf(0f) }
            var initialScrollApplied by remember { mutableStateOf(false) }
            latestViewportWidthPx = viewportWidthPx
            latestViewportHeightPx = viewportHeightPx
            maxObservedViewportHeightPx = max(maxObservedViewportHeightPx, viewportHeightPx)
            val lineNumberOptions = gutterOptions.lineNumbers
            val lineNumberGutterStartPaddingPx = with(density) { lineNumberOptions.startPadding.toPx() }
            val lineNumberGutterEndPaddingPx = with(density) { lineNumberOptions.endPadding.toPx() }
            val contentStartPaddingPx = with(density) { contentOptions.startPadding.toPx() }
            val contentEndPaddingPx = with(density) { contentOptions.endPadding.toPx() }
            val lineNumberGutterWidthPx = remember(
                gutterOptions,
                lineLayoutCache,
                layoutSnapshot.lineCount,
                lineNumberGutterStartPaddingPx,
                lineNumberGutterEndPaddingPx,
            ) {
                if (!gutterOptions.visible || !lineNumberOptions.visible) {
                    0f
                } else {
                    resolveCodeGutterWidthPx(
                        lineLayoutCache = lineLayoutCache,
                        lineCount = layoutSnapshot.lineCount,
                        gutterOptions = gutterOptions,
                        startPaddingPx = lineNumberGutterStartPaddingPx,
                        endPaddingPx = lineNumberGutterEndPaddingPx,
                    )
                }
            }
            val contentViewportWidthPx = resolveCodeContentViewportWidthPx(
                viewportWidthPx = viewportWidthPx,
                contentLeftInsetPx = lineNumberGutterWidthPx,
            )
            latestContentViewportWidthPx = contentViewportWidthPx
            val estimatedMaxLineWidthPx = remember(layoutSnapshot, charWidthPx) {
                lineLayoutCache.estimatedMaxLineWidthPx(charWidthPx)
            }
            val textContentWidthPx = max(
                contentViewportWidthPx,
                contentStartPaddingPx + estimatedMaxLineWidthPx + charWidthPx + contentEndPaddingPx,
            )
            val maxHorizontalScrollPx = max(0f, textContentWidthPx - contentViewportWidthPx)
            val safeScrollPastEnd = scrollPastEnd.coerceAtLeast(0)

            val currentViewport = resolveCodeViewerViewportState(
                layoutSnapshot = layoutSnapshot,
                verticalScrollPx = verticalScrollState.value,
                horizontalScrollPx = horizontalScrollState.value.toFloat(),
                viewportWidthPx = contentViewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                lineHeightPx = lineHeightPx,
                maxHorizontalScrollPx = maxHorizontalScrollPx,
                extraBottomLines = safeScrollPastEnd,
            )
            val renderLineRange = currentViewport.renderLineRange(
                lineCount = layoutSnapshot.lineCount,
                extraLeadingLines = VIEWPORT_RENDER_LEADING_OVERSCAN_LINES,
                extraTrailingLines = VIEWPORT_RENDER_TRAILING_OVERSCAN_LINES,
            )
            fun currentMaxHorizontalScrollPx(): Float {
                return max(0f, textContentWidthPx - latestContentViewportWidthPx)
            }

            fun currentViewportState(): CodeViewportState {
                return resolveCodeViewerViewportState(
                    layoutSnapshot = layoutSnapshot,
                    verticalScrollPx = verticalScrollState.value,
                    horizontalScrollPx = horizontalScrollState.value.toFloat(),
                    viewportWidthPx = latestContentViewportWidthPx,
                    viewportHeightPx = latestViewportHeightPx,
                    lineHeightPx = lineHeightPx,
                    maxHorizontalScrollPx = currentMaxHorizontalScrollPx(),
                    extraBottomLines = safeScrollPastEnd,
                )
            }

            LaunchedEffect(
                layoutSnapshot.text,
                lineHeightPx,
                initialFirstVisibleLine,
                initialScrollOffsetX,
            ) {
                if (initialScrollApplied || lineHeightPx <= 0f) return@LaunchedEffect
                verticalScrollState.scrollTo(
                    initialFirstVisibleLine.coerceAtLeast(0) * lineHeightPx
                )
                horizontalScrollState.scrollTo(initialScrollOffsetX.coerceAtLeast(0))
                initialScrollApplied = true
            }

            LaunchedEffect(
                cursorTarget?.token,
                cursorTarget?.line,
                cursorTarget?.offset,
                layoutSnapshot.text,
                charWidthPx,
                lineHeightPx,
            ) {
                if (cursorTarget == null || lineHeightPx <= 0f) return@LaunchedEffect
                val revealTarget = resolveNavigationRevealTarget(
                    layoutSnapshot = layoutSnapshot,
                    lineLayoutCache = lineLayoutCache,
                    cursorTarget = cursorTarget,
                    charWidthPx = charWidthPx,
                    contentStartPaddingPx = contentStartPaddingPx,
                ) ?: return@LaunchedEffect
                revealCursorTargetIfNeeded(
                    currentViewport = currentViewportState(),
                    revealTarget = revealTarget,
                    layoutSnapshot = layoutSnapshot,
                    maxHorizontalScrollPx = currentMaxHorizontalScrollPx(),
                    extraBottomLines = safeScrollPastEnd,
                    verticalScrollState = verticalScrollState,
                    horizontalScrollState = horizontalScrollState,
                    preferredBottomReservePx = 0f,
                )
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
                charWidthPx,
                lineHeightPx,
            ) {
                if (lineHeightPx <= 0f) return@LaunchedEffect
                val revealTarget = resolveEditingRevealTarget(
                    layoutSnapshot = layoutSnapshot,
                    lineLayoutCache = lineLayoutCache,
                    safeCursor = safeCursor,
                    followCursorToken = followCursorToken,
                    composingOverlay = composingOverlay,
                    charWidthPx = charWidthPx,
                    contentStartPaddingPx = contentStartPaddingPx,
                ) ?: return@LaunchedEffect
                revealCursorTargetIfNeeded(
                    currentViewport = currentViewportState(),
                    revealTarget = revealTarget,
                    layoutSnapshot = layoutSnapshot,
                    maxHorizontalScrollPx = currentMaxHorizontalScrollPx(),
                    extraBottomLines = safeScrollPastEnd,
                    verticalScrollState = verticalScrollState,
                    horizontalScrollState = horizontalScrollState,
                    preferredBottomReservePx = 0f,
                )
            }

            LaunchedEffect(
                cursorTarget?.token,
                cursorTarget?.line,
                cursorTarget?.offset,
                followCursorToken,
                imeBottomInsetPx.roundToInt(),
                safeCursor?.line,
                safeCursor?.offset,
                composingOverlay?.anchorSelection?.start,
                composingOverlay?.anchorSelection?.end,
                composingOverlay?.imeFieldValue?.text,
                composingOverlay?.imeFieldValue?.selection?.start,
                composingOverlay?.imeFieldValue?.selection?.end,
                layoutSnapshot.text,
                charWidthPx,
                lineHeightPx,
            ) {
                if (lineHeightPx <= 0f) return@LaunchedEffect
                snapshotFlow {
                    latestViewportWidthPx.roundToInt() to latestViewportHeightPx.roundToInt()
                }.collectLatest {
                    val revealTarget = resolveNavigationRevealTarget(
                        layoutSnapshot = layoutSnapshot,
                        lineLayoutCache = lineLayoutCache,
                        cursorTarget = cursorTarget,
                        charWidthPx = charWidthPx,
                        contentStartPaddingPx = contentStartPaddingPx,
                    ) ?: resolveEditingRevealTarget(
                        layoutSnapshot = layoutSnapshot,
                        lineLayoutCache = lineLayoutCache,
                        safeCursor = safeCursor,
                        followCursorToken = followCursorToken,
                        composingOverlay = composingOverlay,
                        charWidthPx = charWidthPx,
                        contentStartPaddingPx = contentStartPaddingPx,
                    ) ?: return@collectLatest
                    revealCursorTargetIfNeeded(
                        currentViewport = currentViewportState(),
                        revealTarget = revealTarget,
                        layoutSnapshot = layoutSnapshot,
                        maxHorizontalScrollPx = currentMaxHorizontalScrollPx(),
                        extraBottomLines = safeScrollPastEnd,
                        verticalScrollState = verticalScrollState,
                        horizontalScrollState = horizontalScrollState,
                        preferredBottomReservePx = resolveViewportRevealBottomReservePx(
                            maxObservedViewportHeightPx = maxObservedViewportHeightPx,
                            currentViewportHeightPx = latestViewportHeightPx,
                            lineHeightPx = lineHeightPx,
                            imeBottomInsetPx = imeBottomInsetPx,
                        ),
                    )
                }
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

            val textContentWidthDp = with(density) {
                textContentWidthPx.toDp()
            }
            val contentViewportWidthDp = with(density) {
                contentViewportWidthPx.toDp()
            }
            val lineNumberGutterWidthDp = with(density) {
                lineNumberGutterWidthPx.toDp()
            }
            val verticalLayout = resolveCodeViewerVerticalLayout(
                lineCount = layoutSnapshot.lineCount,
                lineHeightPx = lineHeightPx,
                viewportHeightPx = viewportHeightPx,
                scrollPastEnd = safeScrollPastEnd,
            )
            verticalScrollState.updateBounds(
                maxValue = verticalLayout.maxVerticalScrollPx,
            )
            val verticalScrollableState = rememberScrollableState { delta ->
                -verticalScrollState.dispatchRawDelta(-delta)
            }
            val viewportHeightDp = with(density) {
                viewportHeightPx.toDp()
            }

            val canvasMetrics = CodeViewerCanvasMetrics(
                charWidthPx = charWidthPx,
                lineHeightPx = lineHeightPx,
                contentHeightPx = contentHeightPx,
                contentTopPaddingPx = contentTopPaddingPx,
                baselinePx = baselinePx,
                contentLeftInsetPx = lineNumberGutterWidthPx,
                contentStartPaddingPx = contentStartPaddingPx,
                contentEndPaddingPx = contentEndPaddingPx,
            )
            val scrollController = remember(horizontalScrollState, verticalScrollState, lineNumberGutterWidthPx) {
                CodeViewerScrollController(
                    horizontalScrollPxProvider = { horizontalScrollState.value.toFloat() },
                    verticalScrollPxProvider = { verticalScrollState.value },
                    viewportWidthPxProvider = { latestContentViewportWidthPx },
                    viewportHeightPxProvider = { latestViewportHeightPx },
                    contentLeftInsetPxProvider = { lineNumberGutterWidthPx },
                    contentStartPaddingPxProvider = { contentStartPaddingPx },
                    scrollByHandler = { horizontalDeltaPx, verticalDeltaPx ->
                        if (horizontalDeltaPx != 0f) {
                            horizontalScrollState.dispatchRawDelta(horizontalDeltaPx)
                        }
                        if (verticalDeltaPx != 0f) {
                            verticalScrollState.dispatchRawDelta(verticalDeltaPx)
                        }
                    },
                )
            }
            val viewportSnapshot = CodeViewerViewportSnapshot(
                verticalScrollPx = verticalScrollState.value,
                horizontalScrollPx = horizontalScrollState.value.toFloat(),
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                lineHeightPx = lineHeightPx,
                contentLeftInsetPx = lineNumberGutterWidthPx,
                contentViewportWidthPx = contentViewportWidthPx,
                contentStartPaddingPx = contentStartPaddingPx,
                contentEndPaddingPx = contentEndPaddingPx,
                horizontalContentOverscrollPx = contentOverscrollEffect.horizontalContentOffsetPx,
                verticalContentOverscrollPx = contentOverscrollEffect.verticalContentOffsetPx,
            )

            floatingUnderlayContent?.invoke(
                canvasMetrics,
                lineLayoutCache,
                viewportSnapshot,
            )
            val currentVerticalScrollPx = verticalScrollState.value
            val verticalScrollableModifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .scrollable(
                    state = verticalScrollableState,
                    orientation = Orientation.Vertical,
                    overscrollEffect = verticalScrollOverscrollEffect,
                )
            val contentViewportModifier = Modifier
                .offset {
                    IntOffset(
                        x = lineNumberGutterWidthPx.roundToInt(),
                        y = 0,
                    )
                }
                .requiredSize(
                    width = contentViewportWidthDp,
                    height = viewportHeightDp,
                )
                .clipToBounds()
                .horizontalScroll(
                    state = horizontalScrollState,
                    overscrollEffect = horizontalScrollOverscrollEffect,
                )
            val contentContextMenuHandler = onContextMenu?.let { contextMenuHandler ->
                { annotationHit: CodeAnnotationHit?, offset: Offset ->
                    contextMenuHandler(
                        annotationHit,
                        offset + Offset(lineNumberGutterWidthPx, 0f),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .overscroll(contentOverscrollEffect)
            ) {
                // Keep floating overlays inside the same overscroll visual layer as the
                // canvas content. This avoids the old "text stretches but handles stay
                // fixed" mismatch and removes the need for hand-tuned overlay chasing.
                Box(modifier = verticalScrollableModifier) {
                    if (lineNumberGutterWidthPx > 0f) {
                        Canvas(
                            modifier = Modifier.requiredSize(
                                width = lineNumberGutterWidthDp,
                                height = viewportHeightDp,
                            ),
                            onDraw = {
                                drawCodeLineNumbers(
                                    layoutSnapshot = layoutSnapshot,
                                    lineLayoutCache = lineLayoutCache,
                                    visibleLineRange = renderLineRange,
                                    lineHeightPx = lineHeightPx,
                                    verticalScrollPx = currentVerticalScrollPx,
                                    baselinePx = baselinePx,
                                    gutterWidthPx = lineNumberGutterWidthPx,
                                    cursor = safeCursor,
                                    gutterOptions = gutterOptions,
                                )
                            },
                        )
                    }

                    Box(modifier = contentViewportModifier) {
                        val codeContentModifier = Modifier
                            .requiredSize(
                                width = textContentWidthDp,
                                height = viewportHeightDp,
                            )
                            .annotationInteractionModifier(
                                layoutSnapshot = layoutSnapshot,
                                lineLayoutCache = lineLayoutCache,
                                documentKey = documentKey,
                                documentRevision = documentRevision,
                                lineHeightPx = lineHeightPx,
                                verticalScrollPx = currentVerticalScrollPx,
                                contentStartPaddingPx = contentStartPaddingPx,
                                interactionOptions = interactionOptions,
                                onAnnotationHit = onAnnotationHit,
                                onContextMenu = contentContextMenuHandler,
                                enablePrimaryClick = enablePrimaryAnnotationClick,
                                enableLongPressContextMenu = enableLongPressContextMenu,
                                enableSecondaryClickContextMenu = enableSecondaryClickContextMenu,
                            )
                        val editorAwareContentModifier = contentModifierTransform?.invoke(
                            codeContentModifier,
                            canvasMetrics,
                            lineLayoutCache,
                            scrollController,
                        ) ?: codeContentModifier

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
                                        verticalScrollPx = currentVerticalScrollPx,
                                        contentHeightPx = contentHeightPx,
                                        contentTopPaddingPx = contentTopPaddingPx,
                                        baselinePx = baselinePx,
                                        contentStartPaddingPx = contentStartPaddingPx,
                                        contentEndPaddingPx = contentEndPaddingPx,
                                        selection = safeSelection,
                                        searchHighlight = safeSearchHighlight,
                                        cursor = safeCursor,
                                        composingOverlay = composingOverlay,
                                        cursorAlpha = cursorAlpha,
                                        visibleLineRange = renderLineRange,
                                        decorationOptions = decorationOptions,
                                    )
                                }
                            )
                            overlayContent?.invoke(
                                Modifier.fillMaxSize(),
                                canvasMetrics,
                            )
                        }
                    }
                }
                floatingContent?.invoke(
                    canvasMetrics,
                    lineLayoutCache,
                    viewportSnapshot,
                    scrollController,
                )
            }
        }
    }
}

internal data class CodeViewerCanvasMetrics(
    val charWidthPx: Float,
    val lineHeightPx: Float,
    val contentHeightPx: Float,
    val contentTopPaddingPx: Float,
    val baselinePx: Float,
    val contentLeftInsetPx: Float,
    val contentStartPaddingPx: Float,
    val contentEndPaddingPx: Float,
)

private suspend fun applyRevealedViewportScrollIfNeeded(
    currentVerticalScrollPx: Float,
    targetVerticalScrollPx: Float,
    currentHorizontalScrollPx: Float,
    targetHorizontalScrollPx: Float,
    verticalScrollState: CodeViewerVerticalScrollState,
    horizontalScrollState: ScrollState,
) {
    if (abs(targetVerticalScrollPx - currentVerticalScrollPx) >= 1f) {
        verticalScrollState.scrollTo(targetVerticalScrollPx)
    }
    if (abs(targetHorizontalScrollPx - currentHorizontalScrollPx) >= 1f) {
        horizontalScrollState.scrollTo(targetHorizontalScrollPx.roundToInt())
    }
}

internal class CodeViewerScrollController(
    private val horizontalScrollPxProvider: () -> Float,
    private val verticalScrollPxProvider: () -> Float,
    private val viewportWidthPxProvider: () -> Float,
    private val viewportHeightPxProvider: () -> Float,
    private val contentLeftInsetPxProvider: () -> Float,
    private val contentStartPaddingPxProvider: () -> Float,
    private val scrollByHandler: (horizontalDeltaPx: Float, verticalDeltaPx: Float) -> Unit,
) {
    val horizontalScrollPx: Float
        get() = horizontalScrollPxProvider()

    val verticalScrollPx: Float
        get() = verticalScrollPxProvider()

    val viewportWidthPx: Float
        get() = viewportWidthPxProvider()

    val viewportHeightPx: Float
        get() = viewportHeightPxProvider()

    val contentLeftInsetPx: Float
        get() = contentLeftInsetPxProvider()

    val contentStartPaddingPx: Float
        get() = contentStartPaddingPxProvider()

    fun scrollBy(
        horizontalDeltaPx: Float = 0f,
        verticalDeltaPx: Float = 0f,
    ) {
        scrollByHandler(horizontalDeltaPx, verticalDeltaPx)
    }
}

internal data class CodeViewerViewportSnapshot(
    val verticalScrollPx: Float,
    val horizontalScrollPx: Float,
    val viewportWidthPx: Float,
    val viewportHeightPx: Float,
    val lineHeightPx: Float,
    val contentLeftInsetPx: Float,
    val contentViewportWidthPx: Float,
    val contentStartPaddingPx: Float,
    val contentEndPaddingPx: Float,
    val horizontalContentOverscrollPx: Float = 0f,
    val verticalContentOverscrollPx: Float = 0f,
)

internal val CodeViewerViewportSnapshot.contentViewportLeftPx: Float
    get() = contentLeftInsetPx

internal val CodeViewerViewportSnapshot.contentViewportRightPx: Float
    get() = contentLeftInsetPx + contentViewportWidthPx

internal fun CodeViewerViewportSnapshot.contentXToViewportX(contentXPx: Float): Float {
    return stretchViewportX(contentLeftInsetPx + contentStartPaddingPx + contentXPx - horizontalScrollPx)
}

internal fun CodeViewerViewportSnapshot.contentYToViewportY(contentYPx: Float): Float {
    return stretchViewportY(contentYPx - verticalScrollPx)
}

internal fun CodeViewerViewportSnapshot.contentXToHandleViewportX(contentXPx: Float): Float {
    // Handles now live in the same overscroll-rendered subtree as the editor content,
    // so they only need the base scroll transform here.
    return contentLeftInsetPx + contentStartPaddingPx + contentXPx - horizontalScrollPx
}

internal fun CodeViewerViewportSnapshot.contentYToHandleViewportY(contentYPx: Float): Float {
    return contentYPx - verticalScrollPx
}

private fun CodeViewerViewportSnapshot.stretchViewportX(viewportX: Float): Float {
    val scaleDelta = resolveContentStretchScaleDelta(
        overscrollPx = horizontalContentOverscrollPx,
        viewportSizePx = viewportWidthPx,
    )
    return applyStretchTransform(
        positionPx = viewportX,
        viewportSizePx = viewportWidthPx,
        scaleDelta = scaleDelta,
        overscrollPx = horizontalContentOverscrollPx,
    )
}

private fun CodeViewerViewportSnapshot.stretchViewportY(viewportY: Float): Float {
    val scaleDelta = resolveContentStretchScaleDelta(
        overscrollPx = verticalContentOverscrollPx,
        viewportSizePx = viewportHeightPx,
    )
    return applyStretchTransform(
        positionPx = viewportY,
        viewportSizePx = viewportHeightPx,
        scaleDelta = scaleDelta,
        overscrollPx = verticalContentOverscrollPx,
    )
}

internal fun resolveContentStretchScaleDelta(
    overscrollPx: Float,
    viewportSizePx: Float,
): Float {
    if (overscrollPx == 0f || viewportSizePx <= 0f) return 0f
    return (kotlin.math.abs(overscrollPx) / viewportSizePx * CONTENT_STRETCH_SCALE_FACTOR)
        .coerceAtMost(CONTENT_STRETCH_MAX_SCALE_DELTA)
}

private fun applyStretchTransform(
    positionPx: Float,
    viewportSizePx: Float,
    scaleDelta: Float,
    overscrollPx: Float,
): Float {
    if (scaleDelta == 0f) return positionPx
    val scale = 1f + scaleDelta
    return when {
        overscrollPx > 0f -> positionPx * scale
        overscrollPx < 0f -> viewportSizePx - (viewportSizePx - positionPx) * scale
        else -> positionPx
    }
}

private data class ComposingRevealTarget(
    val lineIndex: Int,
    val anchorColumn: Int,
    val xPx: Float,
    val widthPx: Float,
)

private data class CursorRevealTarget(
    val cursor: Cursor,
    val xPx: Float,
    val widthPx: Float,
)

private suspend fun revealCursorTargetIfNeeded(
    currentViewport: CodeViewportState,
    revealTarget: CursorRevealTarget,
    layoutSnapshot: CodeLayoutSnapshot,
    maxHorizontalScrollPx: Float,
    extraBottomLines: Int,
    verticalScrollState: CodeViewerVerticalScrollState,
    horizontalScrollState: ScrollState,
    preferredBottomReservePx: Float,
) {
    val targetHorizontalScrollPx = currentViewport.revealCursor(
        layout = layoutSnapshot,
        cursor = revealTarget.cursor,
        charWidthPx = 1f,
        cursorHorizontalPx = revealTarget.xPx,
        cursorWidthPx = revealTarget.widthPx,
        maxHorizontalScrollPx = maxHorizontalScrollPx,
        extraBottomLines = extraBottomLines,
    ).horizontalScrollPx
    val currentVerticalScrollPx = verticalScrollState.value
    val targetVerticalScrollPx = resolveCursorVerticalRevealTargetPx(
        currentVerticalScrollPx = currentVerticalScrollPx,
        cursorLine = revealTarget.cursor.line,
        lineHeightPx = currentViewport.lineHeightPx,
        viewportHeightPx = currentViewport.viewportHeightPx,
        preferredBottomReservePx = preferredBottomReservePx,
    )
    applyRevealedViewportScrollIfNeeded(
        currentVerticalScrollPx = currentVerticalScrollPx,
        targetVerticalScrollPx = targetVerticalScrollPx,
        currentHorizontalScrollPx = horizontalScrollState.value.toFloat(),
        targetHorizontalScrollPx = targetHorizontalScrollPx,
        verticalScrollState = verticalScrollState,
        horizontalScrollState = horizontalScrollState,
    )
}

internal fun resolveCursorVerticalRevealTargetPx(
    currentVerticalScrollPx: Float,
    cursorLine: Int,
    lineHeightPx: Float,
    viewportHeightPx: Float,
    preferredBottomReservePx: Float,
): Float {
    if (lineHeightPx <= 0f || viewportHeightPx <= 0f) return currentVerticalScrollPx

    val cursorTopPx = cursorLine * lineHeightPx
    val cursorBottomPx = cursorTopPx + lineHeightPx
    val safeBottomReservePx = preferredBottomReservePx
        .coerceIn(0f, (viewportHeightPx - 1f).coerceAtLeast(0f))
    val visibleTopPx = currentVerticalScrollPx
    val visibleBottomPx = currentVerticalScrollPx + viewportHeightPx - safeBottomReservePx

    return when {
        cursorTopPx < visibleTopPx -> cursorTopPx
        cursorBottomPx > visibleBottomPx -> {
            max(0f, cursorBottomPx - (viewportHeightPx - safeBottomReservePx))
        }

        else -> currentVerticalScrollPx
    }
}

internal fun resolveViewportRevealBottomReservePx(
    maxObservedViewportHeightPx: Float,
    currentViewportHeightPx: Float,
    lineHeightPx: Float,
    imeBottomInsetPx: Float,
): Float {
    if (lineHeightPx <= 0f || maxObservedViewportHeightPx <= 0f) return 0f
    val heightDeltaPx = (maxObservedViewportHeightPx - currentViewportHeightPx).coerceAtLeast(0f)
    val dynamicImeReservePx = (imeBottomInsetPx.coerceAtLeast(0f) * VIEWPORT_REVEAL_IME_PROGRESS_RATIO)
        .coerceAtMost(lineHeightPx * VIEWPORT_REVEAL_BOTTOM_RESERVE_LINES)
    return max(heightDeltaPx, dynamicImeReservePx)
        .coerceAtMost(lineHeightPx * VIEWPORT_REVEAL_BOTTOM_RESERVE_LINES)
}

private fun resolveNavigationRevealTarget(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    cursorTarget: CodeViewerCursorTarget?,
    charWidthPx: Float,
    contentStartPaddingPx: Float,
): CursorRevealTarget? {
    val target = cursorTarget ?: return null
    val safeTargetCursor = layoutSnapshot.clampCursor(
        Cursor(
            line = target.line,
            offset = target.offset,
        )
    ) ?: return null
    return CursorRevealTarget(
        cursor = safeTargetCursor,
        xPx = contentStartPaddingPx + lineLayoutCache.columnX(safeTargetCursor.line, safeTargetCursor.offset),
        widthPx = lineLayoutCache.cursorWidthPx(
            lineIndex = safeTargetCursor.line,
            column = safeTargetCursor.offset,
            fallbackCharWidthPx = charWidthPx,
        ),
    )
}

private fun resolveEditingRevealTarget(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    safeCursor: Cursor?,
    followCursorToken: Long?,
    composingOverlay: CodeEditorComposingOverlay?,
    charWidthPx: Float,
    contentStartPaddingPx: Float,
): CursorRevealTarget? {
    val composingRevealTarget = resolveComposingRevealTarget(
        layoutSnapshot = layoutSnapshot,
        lineLayoutCache = lineLayoutCache,
        composingOverlay = composingOverlay,
        contentStartPaddingPx = contentStartPaddingPx,
    )
    return when {
        composingRevealTarget != null -> CursorRevealTarget(
            cursor = Cursor(
                line = composingRevealTarget.lineIndex,
                offset = composingRevealTarget.anchorColumn,
            ),
            xPx = composingRevealTarget.xPx,
            widthPx = composingRevealTarget.widthPx,
        )

        followCursorToken != null && safeCursor != null -> CursorRevealTarget(
            cursor = safeCursor,
            xPx = contentStartPaddingPx + lineLayoutCache.columnX(safeCursor.line, safeCursor.offset),
            widthPx = lineLayoutCache.cursorWidthPx(
                lineIndex = safeCursor.line,
                column = safeCursor.offset,
                fallbackCharWidthPx = charWidthPx,
            ),
        )

        else -> null
    }
}

private fun resolveComposingRevealTarget(
    layoutSnapshot: CodeLayoutSnapshot,
    lineLayoutCache: CodeLineTextLayoutCache,
    composingOverlay: CodeEditorComposingOverlay?,
    contentStartPaddingPx: Float,
): ComposingRevealTarget? {
    val overlay = composingOverlay ?: return null
    val overlayText = overlay.imeFieldValue.text
    if (overlayText.isEmpty()) return null
    if (overlayText.contains('\n') || overlayText.contains('\r')) return null

    val anchorOffset = overlay.anchorSelection.normalizedStart
    val anchorPosition = layoutSnapshot.offsetToPosition(anchorOffset)
    val overlayLayout = lineLayoutCache.plainTextLayout(overlayText)
    val caretOffset = overlay.imeFieldValue.selection.end.coerceIn(0, overlayText.length)
    val anchorX = contentStartPaddingPx + lineLayoutCache.columnX(anchorPosition.lineIndex, anchorPosition.columnIndex)
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

private const val CONTENT_STRETCH_SCALE_FACTOR: Float = 0.5f
private const val CONTENT_STRETCH_MAX_SCALE_DELTA: Float = 0.03f
private const val VIEWPORT_REVEAL_BOTTOM_RESERVE_LINES: Int = 1
private const val VIEWPORT_REVEAL_IME_PROGRESS_RATIO: Float = 0.08f
private const val VIEWPORT_RENDER_LEADING_OVERSCAN_LINES: Int = 2
private const val VIEWPORT_RENDER_TRAILING_OVERSCAN_LINES: Int = 4
