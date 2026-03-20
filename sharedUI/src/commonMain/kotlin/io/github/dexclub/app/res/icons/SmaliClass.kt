package io.github.dexclub.app.res.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.res.IconRes

val IconRes.SmaliClass: ImageVector
    get() {
        if (_Smali != null) {
            return _Smali!!
        }
        _Smali = ImageVector.Builder(
            name = "Smali",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            // 背景圆圈 - Smali 绿色
            path(
                fill = SolidColor(Color(0xFF74B647)),
                fillAlpha = 0.8f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(15f, 8f)
                curveTo(15f, 11.866f, 11.866f, 15f, 8f, 15f)
                curveTo(4.134f, 15f, 1f, 11.866f, 1f, 8f)
                curveTo(1f, 4.134f, 4.134f, 1f, 8f, 1f)
                curveTo(11.866f, 1f, 15f, 4.134f, 15f, 8f)
            }
            // 中间的字母 "S" - 重新设计的正向路径
            path(
                fill = SolidColor(Color(0xFF231F20)),
                fillAlpha = 0.8f,
                pathFillType = PathFillType.NonZero
            ) {
                // S 的上弧线
                moveTo(10.5f, 6.2f)
                lineTo(9.4f, 6.2f)
                curveTo(9.4f, 5.8f, 8.9f, 5.6f, 8.1f, 5.6f)
                curveTo(7.2f, 5.6f, 6.6f, 5.9f, 6.6f, 6.5f)
                curveTo(6.6f, 7.1f, 7.1f, 7.4f, 8.2f, 7.7f)
                // S 的中段转折
                curveTo(9.5f, 8.0f, 10.6f, 8.4f, 10.6f, 9.6f)
                // S 的下弧线
                curveTo(10.6f, 10.7f, 9.5f, 11.4f, 8.0f, 11.4f)
                curveTo(6.4f, 11.4f, 5.4f, 10.8f, 5.4f, 9.8f)
                lineTo(6.5f, 9.8f)
                curveTo(6.5f, 10.2f, 7.1f, 10.5f, 8.0f, 10.5f)
                curveTo(8.8f, 10.5f, 9.4f, 10.2f, 9.4f, 9.6f)
                curveTo(9.4f, 9.0f, 8.9f, 8.7f, 7.8f, 8.4f)
                // 回到上段
                curveTo(6.5f, 8.1f, 5.4f, 7.7f, 5.4f, 6.5f)
                curveTo(5.4f, 5.4f, 6.5f, 4.7f, 8.1f, 4.7f)
                curveTo(9.6f, 4.7f, 10.5f, 5.3f, 10.5f, 6.2f)
                close()
            }
        }.build()

        return _Smali!!
    }

@Suppress("ObjectPropertyName")
private var _Smali: ImageVector? = null
