package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.OutlineButton
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.TextField
import io.github.shadcn.ui.compose.TextFieldColor
import io.github.shadcn.ui.compose.defaultTextFieldColors

@Composable
internal fun WorkspaceInPageSearchBar(
    queryText: String,
    activeMatchIndex: Int,
    matchCount: Int,
    requestFocusToken: Long,
    onQueryChange: (String) -> Unit,
    onPreviousMatch: () -> Unit,
    onNextMatch: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = queryText,
                selection = TextRange(queryText.length),
            )
        )
    }
    val countText = when {
        queryText.isEmpty() -> ""
        matchCount == 0 -> "0 / 0"
        else -> "${activeMatchIndex + 1} / $matchCount"
    }

    fun moveCaretToEnd() {
        textFieldValue = textFieldValue.copy(
            selection = TextRange(textFieldValue.text.length),
            composition = null,
        )
    }

    LaunchedEffect(queryText) {
        if (textFieldValue.text == queryText) {
            return@LaunchedEffect
        }
        textFieldValue = TextFieldValue(
            text = queryText,
            selection = TextRange(queryText.length),
        )
    }

    LaunchedEffect(requestFocusToken) {
        if (requestFocusToken > 0L) {
            moveCaretToEnd()
            focusRequester.requestFocus()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        TextField(
            value = textFieldValue,
            onValueChange = { nextValue ->
                textFieldValue = nextValue
                if (nextValue.text != queryText) {
                    onQueryChange(nextValue.text)
                }
            },
            placeholder = "页内搜索",
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = { onNextMatch() },
            ),
            colors = defaultTextFieldColors(
                color = if (queryText.isNotEmpty() && matchCount == 0) {
                    TextFieldColor.Error
                } else {
                    TextFieldColor.Normal
                },
            ),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }

                    when {
                        keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter -> {
                            if (keyEvent.isShiftPressed) {
                                onPreviousMatch()
                            } else {
                                onNextMatch()
                            }
                            true
                        }

                        keyEvent.key == Key.Escape -> {
                            onClose()
                            true
                        }

                        else -> false
                    }
                },
        )

        Text(
            text = countText,
            style = ShadcnTheme.textStyles.bodySmall.copy(
                color = ShadcnTheme.colors.mutedForeground.copy(alpha = 0.92f),
            ),
            modifier = Modifier.widthIn(min = 48.dp),
        )

        OutlineButton(
            onClick = onPreviousMatch,
            enabled = matchCount > 0,
            modifier = Modifier.heightIn(min = 36.dp),
        ) {
            Text("上")
        }

        OutlineButton(
            onClick = onNextMatch,
            enabled = matchCount > 0,
            modifier = Modifier.heightIn(min = 36.dp),
        ) {
            Text("下")
        }

        OutlineButton(
            onClick = onClose,
            modifier = Modifier.heightIn(min = 36.dp),
        ) {
            Text("关闭")
        }
    }
}
