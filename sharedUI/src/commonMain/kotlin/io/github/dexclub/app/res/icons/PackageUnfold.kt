package io.github.dexclub.app.res.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.res.IconRes

val IconRes.PackageUnfold: ImageVector
    get() {
        if (_PackageUnfold != null) {
            return _PackageUnfold!!
        }
        _PackageUnfold = ImageVector.Builder(
            name = "PackageUnfold",
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
                moveTo(3f, 8f)
                lineTo(6.5f, 4.5f)
                lineTo(3f, 1f)
            }
        }.build()

        return _PackageUnfold!!
    }

@Suppress("ObjectPropertyName")
private var _PackageUnfold: ImageVector? = null
