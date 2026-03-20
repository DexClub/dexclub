package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dexclub.app.res.resolveClassIcon
import io.github.shadcn.ui.compose.Card
import io.github.shadcn.ui.compose.Icon
import io.github.shadcn.ui.compose.ShadcnTheme

@Composable
internal fun WorkspaceClassSearchResultCard(
    result: WorkspaceClassSearchResult,
    onClick: () -> Unit,
) {
    Card(
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .sidePanelManualClickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = resolveClassIcon(result.classVisualKind),
                contentDescription = null,
                tint = null,
                modifier = Modifier.size(16.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = result.className,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ShadcnTheme.textStyles.bodyMedium,
                )

                if (result.descriptor.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = result.descriptor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ShadcnTheme.textStyles.bodySmall.copy(
                            color = ShadcnTheme.colors.mutedForeground,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
internal fun WorkspaceStringSearchResultCard(
    result: WorkspaceStringSearchResult,
    onClick: () -> Unit,
) {
    Card(
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .sidePanelManualClickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = resolveClassIcon(result.classVisualKind),
                contentDescription = null,
                tint = null,
                modifier = Modifier.size(18.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = result.className,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ShadcnTheme.textStyles.bodySmall,
                )

                Text(
                    text = result.methodDisplaySignature,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ShadcnTheme.textStyles.labelSmall.copy(
                        color = ShadcnTheme.colors.mutedForeground,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun WorkspaceSearchStateText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = ShadcnTheme.textStyles.bodySmall.copy(
                color = ShadcnTheme.colors.primary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            ),
        )
    }
}
