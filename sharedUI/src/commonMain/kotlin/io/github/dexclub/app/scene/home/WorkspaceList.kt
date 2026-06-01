package io.github.dexclub.app.scene.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.compose.EIconButton
import io.github.dexclub.app.model.WorkspaceSummary
import io.github.dexclub.app.res.StringRes
import io.github.shadcn.ui.compose.Icon
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.foundation.rememberShadcnIndication
import io.github.shadcn.ui.compose.icons.Close
import io.github.shadcn.ui.compose.icons.Icons

@Composable
private fun WorkspaceListItem(
    item: WorkspaceSummary,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    onSelect: (WorkspaceSummary) -> Unit,
    onDelete: (WorkspaceSummary) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(shape)
            .border(1.dp, ShadcnTheme.colors.border, shape)
            .background(ShadcnTheme.colors.card, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberShadcnIndication(),
                enabled = true,
                onClick = { onSelect(item) },
            ),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 24.dp)
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = item.name,
                style = ShadcnTheme.textStyles.bodyMedium,
                modifier = Modifier,
            )
            Text(
                text = item.displayPath,
                style = ShadcnTheme.textStyles.labelMedium.copy(
                    color = ShadcnTheme.textStyles.labelMedium.color.copy(alpha = 0.4f),
                ),
                modifier = Modifier,
            )
        }

        EIconButton(
            onClick = { onDelete(item) },
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Filled.Close,
                contentDescription = StringRes.current.close,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun WorkspaceListPart(
    uiState: HomeUiState,
    onEnterWorkspace: (WorkspaceSummary) -> Unit,
    onDelete: (WorkspaceSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        uiState.workspaceItems.forEach { item ->
            WorkspaceListItem(
                item = item,
                onSelect = { summary ->
                    onEnterWorkspace(summary)
                },
                onDelete = onDelete,
            )
        }
    }
}
