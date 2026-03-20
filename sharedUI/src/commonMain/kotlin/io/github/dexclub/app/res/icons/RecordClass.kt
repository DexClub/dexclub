package io.github.dexclub.app.res.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.res.IconRes

val IconRes.RecordClass: ImageVector
    get() {
        if (_RecordClass != null) {
            return _RecordClass!!
        }
        _RecordClass = ImageVector.Builder(
            name = "RecordClass",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF40B6E0)),
                fillAlpha = 0.6f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(15f, 8f)
                curveTo(15f, 11.866f, 11.866f, 15f, 8f, 15f)
                curveTo(4.134f, 15f, 1f, 11.866f, 1f, 8f)
                curveTo(1f, 4.134f, 4.134f, 1f, 8f, 1f)
                curveTo(11.866f, 1f, 15f, 4.134f, 15f, 8f)
            }
            path(
                fill = SolidColor(Color(0xFF231F20)),
                fillAlpha = 0.7f,
                pathFillType = PathFillType.EvenOdd
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
        }.build()

        return _RecordClass!!
    }

@Suppress("ObjectPropertyName")
private var _RecordClass: ImageVector? = null
