package io.github.shadcn.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.shadcn.ui.compose.foundation.ShadcnPopupPositionProvider
import io.github.shadcn.ui.compose.foundation.rememberShadcnIndication
import io.github.shadcn.ui.compose.icons.Check
import io.github.shadcn.ui.compose.icons.Icons
import io.github.shadcn.ui.compose.icons.KeyboardArrowDown

@Composable
fun <T> DropdownSelect(
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 192.dp,
    itemLabel: (T) -> String = { it.toString() },
    placeholder: String = "Select an option",
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var triggerWidth by remember { mutableIntStateOf(0) }

    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "Arrow Rotation"
    )
    Box(
        modifier = modifier
            .onSizeChanged { triggerWidth = it.width }
    ) {
        Row(
            modifier = Modifier
                .height(TriggerHeight)
                .width(width)
                .clip(SelectRoundedCornerShape)
                .border(
                    width = TriggerBorderWidth,
                    color = ShadcnTheme.colors.input,
                    shape = SelectRoundedCornerShape
                )
                .background(ShadcnTheme.colors.background)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    expanded = !expanded
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (selectedItem != null) itemLabel(selectedItem) else placeholder,
                color = if (selectedItem != null) ShadcnTheme.colors.foreground else ShadcnTheme.colors.mutedForeground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.Filled.KeyboardArrowDown,
                contentDescription = "Dropdown Arrow",
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(16.dp)
                    .rotate(rotation)
                    .alpha(0.5f),
                tint = ShadcnTheme.colors.foreground
            )
        }

        if (expanded) {
            val density = LocalDensity.current
            val widthDp = with(density) { triggerWidth.toDp() }
            val offsetPx = with(density) { 4.dp.roundToPx() }
            val provider = remember(offsetPx) { ShadcnPopupPositionProvider(offsetPx) }

            Popup(
                onDismissRequest = { expanded = false },
                popupPositionProvider = provider,
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    modifier = Modifier
                        .width(widthDp)
                        .heightIn(max = PopupContentHeight)
                        .shadow(PopupContentElevation, SelectRoundedCornerShape)
                        .background(ShadcnTheme.colors.popover, SelectRoundedCornerShape)
                        .border(PopupContentBorderWidth, ShadcnTheme.colors.border, SelectRoundedCornerShape)
                        .clip(SelectRoundedCornerShape)
                        .padding(PopupContentPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    items.forEach { item ->
                        val isSelected = item == selectedItem
                        DropdownItem(
                            text = itemLabel(item),
                            isSelected = isSelected,
                            onClick = {
                                onItemSelected(item)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val accentColor = ShadcnTheme.colors.accent
    val accentForegroundColor = ShadcnTheme.colors.accentForeground
    val popoverForegroundColor = ShadcnTheme.colors.popoverForeground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(SelectItemRoundedCornerShape)
            .background(if (isSelected) accentColor else Color.Transparent)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberShadcnIndication(accentColor)
            )
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isSelected) accentForegroundColor else popoverForegroundColor,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = accentForegroundColor
                )
            }
        } else {
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

private val TriggerHeight = 40.dp
private val TriggerBorderWidth = 1.dp
private val SelectRoundedCornerShape = RoundedCornerShape(6.dp)
private val PopupContentHeight = 300.dp
private val PopupContentBorderWidth = 1.dp
private val PopupContentElevation = 4.dp
private val PopupContentPadding = 4.dp
private val SelectItemRoundedCornerShape = RoundedCornerShape(4.dp)