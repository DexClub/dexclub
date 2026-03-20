package io.github.shadcn.ui.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LinearProgress(
    modifier: Modifier = Modifier,
    progress: Float? = null, // 0.0 ~ 1.0 or null
    color: Color = ShadcnTheme.colors.primary,
    trackColor: Color = ShadcnTheme.colors.muted,
    height: Dp = 8.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "IndeterminateProgress")

    val animatedProgress by animateFloatAsState(
        targetValue = progress?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LinearProgressAnimation"
    )

    val indeterminateOffsetX by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "IndeterminateOffset"
    )

    // 轨道容器
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        if (progress != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(color)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .fillMaxHeight()
                    .fillMaxWidth(0.4f)
                    .offset(x = height * 0)
                    .graphicsLayer {
                        translationX = indeterminateOffsetX * size.width / 0.4f
                    }
                    .background(color)
            )
        }
    }
}

@Composable
fun CircularProgress(
    modifier: Modifier = Modifier,
    progress: Float? = null, // 0.0 ~ 1.0 or null
    color: Color = ShadcnTheme.colors.primary,
    trackColor: Color = ShadcnTheme.colors.muted,
    strokeWidth: Dp = 4.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CircularLoading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "Rotation"
    )
    val targetProgress = progress ?: 0.25f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "Progress"
    )

    Canvas(
        modifier = modifier
            .size(40.dp)
            .padding(strokeWidth / 2)
    ) {
        val diameter = size.minDimension
        val radius = diameter / 2
        val strokePx = strokeWidth.toPx()

        // 1. 绘制轨道 (Track)
        drawCircle(
            color = trackColor,
            style = Stroke(width = strokePx),
            radius = radius,
            center = center
        )

        // 2. 绘制进度 (Indicator)
        val sweepAngle = if (progress == null) {
            // Loading 模式：固定长度的弧线
            90f
        } else {
            // 进度模式：根据数值计算角度
            animatedProgress * 360f
        }

        val startAngle = if (progress == null) {
            // Loading 模式：随时间旋转
            rotation - 90f // -90 是为了从顶部开始
        } else {
            // 进度模式：固定从顶部开始 (-90度)
            -90f
        }

        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
            size = Size(diameter, diameter),
            topLeft = Offset(center.x - radius, center.y - radius)
        )
    }
}