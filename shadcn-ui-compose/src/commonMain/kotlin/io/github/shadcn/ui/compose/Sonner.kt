package io.github.shadcn.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.shadcn.ui.compose.graphics.setPlatformBlur
import io.github.shadcn.ui.compose.icons.Close
import io.github.shadcn.ui.compose.icons.Icons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

// Define LocalSonner for easy access
val LocalSonner = staticCompositionLocalOf<SonnerState> {
    error("No SonnerState provided")
}

// -----------------------------------------------------------------------------
// Data Models
// -----------------------------------------------------------------------------
data class SonnerData(
    val id: String,
    val message: String,
    val description: String? = null,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val type: SonnerType = SonnerType.Normal,
    val duration: Long = 4000L,
    val dismissible: Boolean = true,
)

enum class SonnerType {
    Normal, Success, Error
}

enum class SonnerAlignment {
    Top, Bottom
}

// -----------------------------------------------------------------------------
// State Management
// -----------------------------------------------------------------------------
@Stable
class SonnerState(
    val maxVisibleSonners: Int = 3,
    val alignment: SonnerAlignment = SonnerAlignment.Top,
) {
    // Keeping the newest at index 0
    private val _sonners = mutableStateListOf<SonnerData>()
    val sonners: List<SonnerData> get() = _sonners

    fun sonner(
        message: String,
        id: String = UUID.randomUUID().toString(),
        description: String? = null,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        type: SonnerType = SonnerType.Normal,
        duration: Long = 4000L,
    ) {
        val newSonner = SonnerData(
            id = id,
            message = message,
            description = description,
            actionLabel = actionLabel,
            onAction = onAction,
            type = type,
            duration = duration
        )
        val index = _sonners.indexOfFirst { it.id == id }
        if (index != -1) {
            _sonners[index] = newSonner
        } else {
            _sonners.add(0, newSonner)
        }
    }

    fun dismiss(id: String) {
        _sonners.removeAll { it.id == id }
    }

    fun dismissAll() {
        _sonners.clear()
    }

    fun addAll(sonners: List<SonnerData>) {
        _sonners.addAll(sonners)
    }
}

@Composable
fun rememberSonnerState(
    maxVisibleSonners: Int = 3,
    alignment: SonnerAlignment = SonnerAlignment.Top,
) = remember(maxVisibleSonners, alignment) {
    SonnerState(
        maxVisibleSonners = maxVisibleSonners,
        alignment = alignment,
    )
}

// -----------------------------------------------------------------------------
// The Main Component (Sonner)
// -----------------------------------------------------------------------------
/**
 * Place this component in the root layout of the app (such as outside of Scaffold or at the top level of Box)
 */
@Composable
fun SonnerBox(
    modifier: Modifier = Modifier,
    state: SonnerState = rememberSonnerState(),
    content: @Composable BoxScope.() -> Unit,
) {
    val visibleSonners by remember(state.sonners) {
        derivedStateOf { state.sonners.take(state.maxVisibleSonners) }
    }

    CompositionLocalProvider(LocalSonner provides state) {
        val contentAlignment = remember(state.alignment) {
            when (state.alignment) {
                SonnerAlignment.Top -> Alignment.TopCenter
                SonnerAlignment.Bottom -> Alignment.BottomCenter
            }
        }
        Box(modifier = modifier.fillMaxSize()) {
            content()

            Box(
                contentAlignment = contentAlignment,
                modifier = Modifier
                    .matchParentSize()
                    .padding(16.dp),
            ) {
                visibleSonners.forEachIndexed { index, sonner ->
                    key(sonner.id) {
                        SonnerItem(
                            sonner = sonner,
                            maxVisibleSonners = state.maxVisibleSonners,
                            alignment = state.alignment,
                            index = index,
                            totalCount = visibleSonners.size,
                            onDismiss = { state.dismiss(sonner.id) },
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Individual Sonner UI with Stacking Logic
// -----------------------------------------------------------------------------
@Composable
private fun SonnerItem(
    sonner: SonnerData,
    maxVisibleSonners: Int,
    alignment: SonnerAlignment,
    index: Int,
    totalCount: Int,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(sonner.id, sonner.duration) {
        if (sonner.duration > 0) {
            delay(sonner.duration)
            onDismiss()
        }
    }

    val targetScale = 1f - (index * 0.05f)
    val scale by animateFloatAsState(targetValue = targetScale, label = "scale")
    val targetAlpha = if (index < maxVisibleSonners) 1f else 0f
    val alpha by animateFloatAsState(targetValue = targetAlpha, label = "alpha")

    val isTop = alignment == SonnerAlignment.Top
    val offsetMultiplier = if (isTop) 1 else -1
    val gap = 14.dp
    val shadowPadding = 16.dp
    val targetTranslationY = (index * gap.value).dp * offsetMultiplier
    val translationY by animateDpAsState(targetValue = targetTranslationY, label = "translationY")

    if (index < maxVisibleSonners + 1 || alpha > 0.01f) {
        Box(
            modifier = Modifier
                .zIndex((totalCount - index).toFloat())
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.translationY = translationY.toPx()
                    this.alpha = alpha
                    this.cameraDistance = 8 * density
                    this.clip = false
                }
                .padding(bottom = if (!isTop) 24.dp else 0.dp, top = if (isTop) 24.dp else 0.dp)) {
            Box(modifier = Modifier.padding(all = shadowPadding)) {
                SonnerCard(
                    sonner = sonner,
                    onDismiss = onDismiss,
                    swipeEnabled = index == 0
                )
            }
        }
    }
}

@Composable
private fun SonnerCard(
    sonner: SonnerData,
    onDismiss: () -> Unit,
    swipeEnabled: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val swipeAlpha = 1f - (abs(offsetX.value) / 600f).coerceIn(0f, 0.8f)

    // Determine colors based on type (Optional customization)
    val colors = ShadcnTheme.colors
    val borderColor = if (sonner.type == SonnerType.Error) colors.destructive else colors.border
    val iconColor = if (sonner.type == SonnerType.Error) colors.destructive else colors.foreground

    BoxWithConstraints {
        val cardMaxWidth = remember(maxWidth) {
            resolveSonnerMaxWidth(maxWidth).coerceAtMost(maxWidth)
        }
        val cardMinWidth = remember(cardMaxWidth) {
            300.dp.coerceAtMost(cardMaxWidth)
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.background,
            contentColor = colors.foreground,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .widthIn(min = cardMinWidth, max = cardMaxWidth)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .drawBehind {
                    if (swipeAlpha < 0.01f) return@drawBehind
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = Color.Black.copy(alpha = 0.15f * swipeAlpha)
                            setPlatformBlur(20.dp.toPx())
                        }

                        val shadowOffsetY = 4.dp.toPx()
                        canvas.drawRoundRect(
                            left = 0f,
                            top = shadowOffsetY,
                            right = size.width,
                            bottom = size.height + shadowOffsetY,
                            radiusX = 12.dp.toPx(),
                            radiusY = 12.dp.toPx(),
                            paint = paint,
                        )
                    }
                }
                .graphicsLayer {
                    alpha = swipeAlpha
                    clip = false // 必须设置为 false，防止阴影被当前层裁剪
                }
                .pointerInput(swipeEnabled) {
                    if (!swipeEnabled) return@pointerInput
                    detectHorizontalDragGestures(onDragEnd = {
                        if (abs(offsetX.value) > size.width / 4) {
                            scope.launch {
                                val target = if (offsetX.value > 0) {
                                    size.width.toFloat() * 1.5f
                                } else {
                                    -size.width.toFloat() * 1.5f
                                }
                                offsetX.animateTo(target, tween(300))
                                onDismiss()
                            }
                        } else {
                            scope.launch {
                                offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                    }, onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount)
                        }
                    })
                },
        ) {
            Row(
                modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                // Optional Icon (Simple example)
                if (sonner.type == SonnerType.Error) {
                    // Replace with your icon library (e.g. Lucide)
                    Text(
                        "!",
                        color = iconColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = sonner.message,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.foreground,
                    )
                    if (sonner.description != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sonner.description,
                            fontSize = 12.sp,
                            color = colors.mutedForeground,
                            lineHeight = 16.sp,
                        )
                    }
                }

                // Action Button
                if (sonner.actionLabel != null && sonner.onAction != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        colors = ShadcnButtonDefaults.primaryColors(),
                        onClick = {
                            sonner.onAction.invoke()
                            onDismiss()
                        },
                    ) {
                        Text(
                            text = sonner.actionLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Close Button (Optional, mimics Shadcn "close" prop)
                if (sonner.dismissible && sonner.actionLabel == null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.Filled.Close,
                        contentDescription = "Close",
                        tint = colors.mutedForeground,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(
                                indication = null,
                                interactionSource = null,
                                onClick = onDismiss,
                            ),
                    )
                }
            }
        }
    }
}

private fun resolveSonnerMaxWidth(containerWidth: Dp): Dp {
    return when {
        containerWidth < 360.dp -> containerWidth
        containerWidth < 840.dp -> 356.dp
        containerWidth < 1280.dp -> 420.dp
        else -> 480.dp
    }
}
