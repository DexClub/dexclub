package io.github.shadcn.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class TextFieldColor {
    Normal,
    Success,
    Error
}

data class TextFieldColors(
    val backgroundColor: Color,
    val textColor: Color,
    val cursorColor: Color,
    val placeholderColor: Color,
    val borderColor: Color,
    val focusRingColor: Color,
)

@Composable
fun defaultTextFieldColors(
    color: TextFieldColor = TextFieldColor.Normal,
): TextFieldColors {
    val theme = ShadcnTheme.colors
    return when (color) {
        TextFieldColor.Normal -> TextFieldColors(
            backgroundColor = theme.background,
            textColor = theme.foreground,
            cursorColor = theme.primary,
            placeholderColor = theme.mutedForeground,
            borderColor = theme.border,
            focusRingColor = theme.primary
        )

        TextFieldColor.Success -> TextFieldColors(
            backgroundColor = theme.background,
            textColor = theme.foreground,
            cursorColor = Color(0x22, 0x16, 0x34),
            placeholderColor = theme.mutedForeground,
            borderColor = Color(0x22, 0x16, 0x34),
            focusRingColor = Color(0x22, 0x16, 0x34)
        )

        TextFieldColor.Error -> TextFieldColors(
            backgroundColor = theme.background,
            textColor = theme.foreground,
            cursorColor = Color(0xEF, 0x44, 0x44),
            placeholderColor = theme.mutedForeground,
            borderColor = Color(0xEF, 0x44, 0x44),
            focusRingColor = Color(0xEF, 0x44, 0x44)
        )
    }
}

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: CharSequence = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    shape: Shape = RoundedCornerShape(6.dp),
    textStyle: TextStyle = ShadcnTheme.textStyles.bodyMedium,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(ShadcnTheme.colors.primary),
    decorationBox: (@Composable (innerTextField: @Composable () -> Unit) -> Unit)? = null,
    colors: TextFieldColors = defaultTextFieldColors(color = TextFieldColor.Normal),
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val mergedStyle = LocalTextStyle.current.merge(textStyle).copy(
        color = if (enabled) colors.textColor else colors.textColor.copy(alpha = 0.5f)
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = mergedStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox ?: { innerTextField ->
            ShadcnTextFieldDecoration(
                isFocused = isFocused,
                enabled = enabled,
                shape = shape,
                textStyle = mergedStyle,
                innerTextField = innerTextField,
                placeholder = placeholder,
                isEmpty = value.isEmpty(),
                colors = colors
            )
        }
    )
}

@Composable
fun TextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: CharSequence = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    shape: Shape = RoundedCornerShape(6.dp),
    textStyle: TextStyle = ShadcnTheme.textStyles.bodyMedium,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(ShadcnTheme.colors.primary),
    decorationBox: (@Composable (innerTextField: @Composable () -> Unit) -> Unit)? = null,
    colors: TextFieldColors = defaultTextFieldColors(color = TextFieldColor.Normal),
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val mergedStyle = LocalTextStyle.current.merge(textStyle).copy(
        color = if (enabled) colors.textColor else colors.textColor.copy(alpha = 0.5f)
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = mergedStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox ?: { innerTextField ->
            ShadcnTextFieldDecoration(
                isFocused = isFocused,
                enabled = enabled,
                shape = shape,
                textStyle = mergedStyle,
                innerTextField = innerTextField,
                placeholder = placeholder,
                isEmpty = value.text.isEmpty(),
                colors = colors
            )
        }
    )
}

@Composable
private fun ShadcnTextFieldDecoration(
    isFocused: Boolean,
    enabled: Boolean,
    shape: Shape,
    textStyle: TextStyle,
    innerTextField: @Composable () -> Unit,
    placeholder: CharSequence = "",
    isEmpty: Boolean,
    ringWidth: Dp = 3.dp,
    colors: TextFieldColors,
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.borderColor.copy(alpha = 0.5f)
            isFocused -> colors.borderColor
            else -> colors.borderColor
        },
        animationSpec = tween(150),
        label = "BorderColor"
    )
    val focusAlpha by animateFloatAsState(
        targetValue = if (isFocused && enabled) 0.2f else 0f,
        animationSpec = tween(300),
        label = "FocusRingAlpha"
    )

    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.8f)
            .padding(ringWidth)
            .shadcnFocusRing(
                focusActive = focusAlpha > 0f,
                ringColor = colors.focusRingColor.copy(alpha = focusAlpha),
                ringWidth = ringWidth,
                shape = shape
            )
            .background(colors.backgroundColor, shape)
            .border(BorderWidth, animatedBorderColor, shape)
            .padding(horizontal = HorizontalPadding, vertical = VerticalPadding),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = buildAnnotatedString { append(placeholder) },
            style = textStyle.copy(
                color = colors.placeholderColor,
            ),
            modifier = Modifier
                .alpha(if (isEmpty) 1f else 0f)
                .then(if (!isEmpty) Modifier.clearAndSetSemantics { } else Modifier)
        )
        innerTextField()
    }
}

private fun Modifier.shadcnFocusRing(
    focusActive: Boolean,
    ringColor: Color,
    shape: Shape,
    ringWidth: Dp,
): Modifier = this.drawWithCache {
    onDrawBehind {
        if (focusActive) {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                color = ringColor,
                style = Stroke(width = ringWidth.toPx() * 2f)
            )
        }
    }
}

private val BorderWidth = 1.dp
private val HorizontalPadding = 12.dp
private val VerticalPadding = 8.dp