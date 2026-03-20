package io.github.shadcn.ui.compose.foundation

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import io.github.shadcn.ui.compose.DropdownMenuAlignment

class ShadcnPopupPositionProvider(
    private val offsetPx: Int,
    private val alignment: DropdownMenuAlignment = DropdownMenuAlignment.Start,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val rawX = when (alignment) {
            DropdownMenuAlignment.Start -> {
                if (layoutDirection == LayoutDirection.Ltr) {
                    anchorBounds.left
                } else {
                    anchorBounds.right - popupContentSize.width
                }
            }

            DropdownMenuAlignment.End -> {
                if (layoutDirection == LayoutDirection.Ltr) {
                    anchorBounds.right - popupContentSize.width
                } else {
                    anchorBounds.left
                }
            }
        }
        val x = rawX.coerceIn(0, maxX)

        val yBottom = anchorBounds.bottom + offsetPx
        val fitsBelow = yBottom + popupContentSize.height <= windowSize.height

        val yTop = anchorBounds.top - popupContentSize.height - offsetPx
        val fitsAbove = yTop >= 0

        val finalY = if (fitsBelow) {
            yBottom
        } else if (fitsAbove) {
            yTop
        } else {
            yBottom
        }

        return IntOffset(x, finalY)
    }
}
