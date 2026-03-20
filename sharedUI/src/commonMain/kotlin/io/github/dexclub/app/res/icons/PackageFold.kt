package io.github.dexclub.app.res.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.res.IconRes

val IconRes.PackageFold: ImageVector
    get() {
        if (_PackageFold != null) {
            return _PackageFold!!
        }
        _PackageFold = ImageVector.Builder(
            name = "PackageFold",
            defaultWidth = 9.dp,
            defaultHeight = 9.dp,
            viewportWidth = 9f,
            viewportHeight = 9f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFFA8ADBD)),
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8f, 2.75f)
                lineTo(4.5f, 6.25f)
                lineTo(1f, 2.75f)
            }
        }.build()

        return _PackageFold!!
    }

@Suppress("ObjectPropertyName")
private var _PackageFold: ImageVector? = null
