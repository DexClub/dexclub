package io.github.dexclub.app.res.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.res.IconRes

val IconRes.MixedAnnotationClass: ImageVector
    get() {
        if (_MixedAnnotationClass != null) {
            return _MixedAnnotationClass!!
        }

        _MixedAnnotationClass = ImageVector.Builder(
            name = "MixedAnnotationClass",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF74B647)),
                fillAlpha = 0.8f,
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(15f, 8f)
                curveTo(15f, 11.866f, 11.866f, 15f, 8f, 15f)
                curveTo(4.134f, 15f, 1f, 11.866f, 1f, 8f)
                curveTo(1f, 4.134f, 4.134f, 1f, 8f, 1f)
                curveTo(11.866f, 1f, 15f, 4.134f, 15f, 8f)
                close()
            }

            path(
                fill = SolidColor(Color(0xFF40B6E0)),
                fillAlpha = 0.62f,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(12.9497f, 3.0503f)
                curveTo(15.6827f, 5.7833f, 15.6827f, 10.2167f, 12.9497f, 12.9497f)
                curveTo(10.2167f, 15.6827f, 5.7833f, 15.6827f, 3.0503f, 12.9497f)
                lineTo(12.9497f, 3.0503f)
                close()
            }

            group(
                translationX = -2.2f,
                translationY = -2.2f,
                scaleX = 0.58f,
                scaleY = 0.58f,
                pivotX = 8f,
                pivotY = 8f,
            ) {
                path(
                    fill = SolidColor(Color(0xFF1B1B1B)),
                    fillAlpha = 0.86f,
                    pathFillType = PathFillType.NonZero,
                ) {
                    moveTo(10.5f, 6.2f)
                    lineTo(9.4f, 6.2f)
                    curveTo(9.4f, 5.8f, 8.9f, 5.6f, 8.1f, 5.6f)
                    curveTo(7.2f, 5.6f, 6.6f, 5.9f, 6.6f, 6.5f)
                    curveTo(6.6f, 7.1f, 7.1f, 7.4f, 8.2f, 7.7f)
                    curveTo(9.5f, 8.0f, 10.6f, 8.4f, 10.6f, 9.6f)
                    curveTo(10.6f, 10.7f, 9.5f, 11.4f, 8.0f, 11.4f)
                    curveTo(6.4f, 11.4f, 5.4f, 10.8f, 5.4f, 9.8f)
                    lineTo(6.5f, 9.8f)
                    curveTo(6.5f, 10.2f, 7.1f, 10.5f, 8.0f, 10.5f)
                    curveTo(8.8f, 10.5f, 9.4f, 10.2f, 9.4f, 9.6f)
                    curveTo(9.4f, 9.0f, 8.9f, 8.7f, 7.8f, 8.4f)
                    curveTo(6.5f, 8.1f, 5.4f, 7.7f, 5.4f, 6.5f)
                    curveTo(5.4f, 5.4f, 6.5f, 4.7f, 8.1f, 4.7f)
                    curveTo(9.6f, 4.7f, 10.5f, 5.3f, 10.5f, 6.2f)
                    close()
                }
            }

            group(
                translationX = 2.15f,
                translationY = 2.15f,
                scaleX = 0.43f,
                scaleY = 0.43f,
                pivotX = 8f,
                pivotY = 8f,
            ) {
                path(
                    fill = SolidColor(Color(0xFF1B1B1B)),
                    fillAlpha = 0.82f,
                    pathFillType = PathFillType.EvenOdd,
                ) {
                    moveTo(7.628f, 6.761f)
                    curveTo(7.473f, 6.851f, 7.345f, 6.974f, 7.246f, 7.124f)
                    curveTo(7.151f, 7.272f, 7.081f, 7.441f, 7.039f, 7.626f)
                    curveTo(6.997f, 7.809f, 6.976f, 7.996f, 6.976f, 8.184f)
                    curveTo(6.976f, 8.586f, 7.049f, 8.872f, 7.197f, 9.058f)
                    curveTo(7.349f, 9.248f, 7.554f, 9.345f, 7.805f, 9.345f)
                    curveTo(7.97f, 9.345f, 8.115f, 9.307f, 8.237f, 9.231f)
                    curveTo(8.357f, 9.158f, 8.457f, 9.053f, 8.533f, 8.92f)
                    curveTo(8.606f, 8.793f, 8.663f, 8.64f, 8.701f, 8.464f)
                    curveTo(8.739f, 8.294f, 8.764f, 8.101f, 8.773f, 7.893f)
                    lineTo(8.842f, 6.699f)
                    lineTo(8.8f, 6.689f)
                    curveTo(8.685f, 6.662f, 8.598f, 6.648f, 8.5f, 6.637f)
                    curveTo(8.402f, 6.627f, 8.304f, 6.622f, 8.202f, 6.622f)
                    curveTo(7.978f, 6.622f, 7.786f, 6.668f, 7.628f, 6.761f)
                    close()
                    moveTo(4.002f, 8.001f)
                    curveTo(4.002f, 4.001f, 7.667f, 4.001f, 8.316f, 4.001f)
                    curveTo(8.651f, 4.001f, 12f, 4.086f, 12f, 7.557f)
                    curveTo(12f, 10.009f, 10.475f, 10f, 10.17f, 10f)
                    curveTo(9.673f, 9.97f, 9.314f, 9.774f, 9.093f, 9.411f)
                    curveTo(8.683f, 9.804f, 8.281f, 10f, 7.889f, 10f)
                    curveTo(7.08f, 10f, 6f, 9.921f, 6f, 8f)
                    curveTo(6f, 6.32f, 7.456f, 6f, 8f, 6f)
                    curveTo(8.153f, 6f, 9.745f, 6.055f, 9.824f, 6.079f)
                    lineTo(9.744f, 8.188f)
                    curveTo(9.636f, 8.893f, 9.799f, 9.245f, 10.233f, 9.245f)
                    curveTo(10.956f, 9.245f, 11.06f, 7.783f, 11.06f, 7.547f)
                    curveTo(11.06f, 4.907f, 8.708f, 4.745f, 8.316f, 4.745f)
                    curveTo(7.771f, 4.745f, 4.944f, 4.851f, 4.944f, 8.001f)
                    curveTo(4.944f, 8.584f, 4.944f, 11.246f, 7.889f, 11.246f)
                    curveTo(8.09f, 11.246f, 9.744f, 11.043f, 10.162f, 10.828f)
                    lineTo(10.162f, 11.578f)
                    curveTo(9.855f, 11.703f, 9.093f, 12f, 7.898f, 12f)
                    curveTo(7.156f, 12f, 4.002f, 12f, 4.002f, 8.001f)
                    close()
                }
            }
        }.build()

        return _MixedAnnotationClass!!
    }

@Suppress("ObjectPropertyName")
private var _MixedAnnotationClass: ImageVector? = null
