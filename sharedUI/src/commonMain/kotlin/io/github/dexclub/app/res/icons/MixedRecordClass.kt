package io.github.dexclub.app.res.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.res.IconRes

val IconRes.MixedRecordClass: ImageVector
    get() {
        if (_MixedRecordClass != null) {
            return _MixedRecordClass!!
        }

        _MixedRecordClass = ImageVector.Builder(
            name = "MixedRecordClass",
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
                translationX = 2.2f,
                translationY = 2.2f,
                scaleX = 0.58f,
                scaleY = 0.58f,
                pivotX = 8f,
                pivotY = 8f,
            ) {
                path(
                    fill = SolidColor(Color(0xFF1B1B1B)),
                    fillAlpha = 0.82f,
                    pathFillType = PathFillType.EvenOdd,
                ) {
                    moveTo(7f, 8f)
                    verticalLineTo(6f)
                    horizontalLineTo(8.537f)
                    curveTo(9.303f, 6f, 10f, 6.313f, 10f, 6.954f)
                    curveTo(10f, 7.595f, 9.285f, 8f, 8.547f, 8f)
                    horizontalLineTo(7f)
                    close()
                    moveTo(9.36f, 8.828f)
                    curveTo(10.175f, 8.59f, 11f, 8.109f, 11f, 6.913f)
                    curveTo(11f, 5.716f, 9.949f, 5f, 8.622f, 5f)
                    horizontalLineTo(6f)
                    verticalLineTo(11f)
                    horizontalLineTo(7f)
                    verticalLineTo(9f)
                    horizontalLineTo(7.5f)
                    horizontalLineTo(8.328f)
                    lineTo(9.773f, 11f)
                    horizontalLineTo(11f)
                    lineTo(9.36f, 8.828f)
                    close()
                }
            }
        }.build()

        return _MixedRecordClass!!
    }

@Suppress("ObjectPropertyName")
private var _MixedRecordClass: ImageVector? = null
