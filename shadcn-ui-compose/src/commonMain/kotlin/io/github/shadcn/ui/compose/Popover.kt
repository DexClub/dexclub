package io.github.shadcn.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.shadcn.ui.compose.foundation.ShadcnPopupPositionProvider

@Composable
fun PopoverBox(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    popupPadding: PaddingValues = PaddingValues.Zero,
    width: Dp = PopupContentWidth,
    height: Dp = PopupContentHeight,
    trigger: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        trigger()
        if (expanded) {
            val density = LocalDensity.current
            val offsetPx = with(density) { 4.dp.roundToPx() }
            val popupPositionProvider = remember(offsetPx) {
                ShadcnPopupPositionProvider(offsetPx = offsetPx)
            }
            Popup(
                onDismissRequest = onDismissRequest,
                popupPositionProvider = popupPositionProvider,
                properties = PopupProperties(focusable = true)
            ) {
                Box(
                    modifier = Modifier
                        .padding(popupPadding)
                        .widthIn(max = width)
                        .heightIn(max = height)
                        .shadow(Elevation, RoundedCornerShape)
                        .background(ShadcnTheme.colors.popover, RoundedCornerShape)
                        .border(BorderWidth, ShadcnTheme.colors.border, RoundedCornerShape)
                        .clip(RoundedCornerShape)
                        .padding(Padding),
                ) {
                    content()
                }
            }
        }
    }
}

private val RoundedCornerShape = RoundedCornerShape(8.dp)
private val Elevation = 4.dp
private val BorderWidth = 1.dp
private val Padding = 4.dp
private val PopupContentWidth = 200.dp
private val PopupContentHeight = 300.dp
