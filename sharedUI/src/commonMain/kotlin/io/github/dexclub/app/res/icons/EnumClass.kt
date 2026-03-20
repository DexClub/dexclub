package io.github.dexclub.app.res.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.res.IconRes

val IconRes.EnumClass: ImageVector
    get() {
        if (_EnumClass != null) {
            return _EnumClass!!
        }
        _EnumClass = ImageVector.Builder(
            name = "EnumClass",
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
                moveTo(10f, 11f)
                lineToRelative(-4f, 0f)
                lineToRelative(0f, -6f)
                lineToRelative(4f, 0f)
                lineToRelative(0f, 1f)
                lineToRelative(-3f, 0f)
                lineToRelative(0f, 1f)
                lineToRelative(2.5f, 0f)
                lineToRelative(0f, 1f)
                lineToRelative(-2.5f, 0f)
                lineToRelative(0f, 2f)
                lineToRelative(3f, 0f)
                close()
            }
        }.build()

        return _EnumClass!!
    }

@Suppress("ObjectPropertyName")
private var _EnumClass: ImageVector? = null
