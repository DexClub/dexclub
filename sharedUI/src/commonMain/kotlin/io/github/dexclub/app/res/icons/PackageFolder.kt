package io.github.dexclub.app.res.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.res.IconRes

val IconRes.PackageFolder: ImageVector
    get() {
        if (_PackageFolder != null) {
            return _PackageFolder!!
        }
        _PackageFolder = ImageVector.Builder(
            name = "PackageFolder",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFEBECF0)),
                stroke = SolidColor(Color(0xFF6C707E)),
                strokeLineWidth = 1f
            ) {
                moveTo(8.106f, 4.346f)
                lineTo(8.253f, 4.5f)
                horizontalLineTo(8.467f)
                horizontalLineTo(13f)
                curveTo(13.828f, 4.5f, 14.5f, 5.172f, 14.5f, 6f)
                verticalLineTo(12.133f)
                curveTo(14.5f, 12.953f, 13.932f, 13.5f, 13.367f, 13.5f)
                horizontalLineTo(2.633f)
                curveTo(2.068f, 13.5f, 1.5f, 12.953f, 1.5f, 12.133f)
                verticalLineTo(3.867f)
                curveTo(1.5f, 3.047f, 2.068f, 2.5f, 2.633f, 2.5f)
                horizontalLineTo(6.122f)
                curveTo(6.258f, 2.5f, 6.388f, 2.556f, 6.483f, 2.654f)
                lineTo(8.106f, 4.346f)
                close()
            }
        }.build()

        return _PackageFolder!!
    }

@Suppress("ObjectPropertyName")
private var _PackageFolder: ImageVector? = null
