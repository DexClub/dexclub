package io.github.dexclub.app.res.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.res.IconRes

val IconRes.JavaClass: ImageVector
    get() {
        if (_JavaClass != null) {
            return _JavaClass!!
        }
        _JavaClass = ImageVector.Builder(
            name = "JavaClass",
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
                moveTo(10f, 9.283f)
                curveTo(9.53f, 9.742f, 9.028f, 9.978f, 8.1f, 10f)
                curveTo(7.061f, 10.022f, 6f, 9.279f, 6f, 8f)
                curveTo(6f, 6.712f, 6.971f, 6f, 8.1f, 6f)
                curveTo(8.948f, 6f, 9.548f, 6.185f, 9.9f, 6.554f)
                lineTo(10.516f, 5.837f)
                curveTo(9.829f, 5.27f, 9.288f, 5f, 8.098f, 5f)
                curveTo(6.34f, 5f, 5f, 6.358f, 5f, 8f)
                curveTo(5f, 9.682f, 6.364f, 11f, 8.002f, 11f)
                curveTo(9.293f, 11f, 10.023f, 10.593f, 10.616f, 9.981f)
                curveTo(10.205f, 9.515f, 10f, 9.283f, 10f, 9.283f)
                close()
            }
        }.build()

        return _JavaClass!!
    }

@Suppress("ObjectPropertyName")
private var _JavaClass: ImageVector? = null