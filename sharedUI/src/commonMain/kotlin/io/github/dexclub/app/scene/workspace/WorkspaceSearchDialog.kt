package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.shadcn.ui.compose.Card
import io.github.shadcn.ui.compose.Dialog
import io.github.shadcn.ui.compose.LoadingButton
import io.github.shadcn.ui.compose.OutlineButton
import io.github.shadcn.ui.compose.ShadcnTheme
import io.github.shadcn.ui.compose.TextField

@Composable
internal fun WorkspaceSearchDialog(
    uiState: WorkspaceSearchDialogUiState,
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onTabSelected: (WorkspaceSearchTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchRequest: () -> Unit,
    onOpenClassResult: (WorkspaceClassSearchResult) -> Unit,
    onOpenStringResult: (WorkspaceStringSearchResult) -> Unit,
) {
    if (!visible) return

    val activeErrorMessage = uiState.activeErrorMessage
    val resultCountText = uiState.resultCountText

    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            val dialogWidth = (maxWidth * 0.96f).coerceAtMost(580.dp)
            val dialogHeight = (maxHeight * 0.92f).coerceAtMost(540.dp)
            val compactLayout = dialogWidth < 460.dp

            Card(
                contentPadding = PaddingValues(20.dp),
                modifier = Modifier
                    .width(dialogWidth)
                    .height(dialogHeight),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        text = "搜索",
                        style = ShadcnTheme.textStyles.titleLarge,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    WorkspaceSearchTabs(
                        currentTab = uiState.currentTab,
                        onTabSelected = onTabSelected,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    if (compactLayout) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TextField(
                                value = uiState.activeQuery,
                                onValueChange = onQueryChange,
                                placeholder = uiState.currentTab.placeholder,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search,
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = { onSearchRequest() },
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 40.dp),
                            )
                            LoadingButton(
                                onClick = onSearchRequest,
                                isLoading = uiState.isCurrentTabSearching,
                                enabled = uiState.activeQuery.trim().isNotEmpty() && uiState.searchingTab == null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 40.dp),
                            ) {
                                Text(
                                    text = "搜索",
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TextField(
                                value = uiState.activeQuery,
                                onValueChange = onQueryChange,
                                placeholder = uiState.currentTab.placeholder,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search,
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = { onSearchRequest() },
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp),
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            LoadingButton(
                                onClick = onSearchRequest,
                                isLoading = uiState.isCurrentTabSearching,
                                enabled = uiState.activeQuery.trim().isNotEmpty() && uiState.searchingTab == null,
                                modifier = Modifier.heightIn(min = 40.dp),
                            ) {
                                Text(
                                    text = "搜索",
                                )
                            }
                        }
                    }

                    if (!activeErrorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = activeErrorMessage,
                            style = ShadcnTheme.textStyles.bodySmall.copy(
                                color = ShadcnTheme.colors.destructive,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(
                        color = ShadcnTheme.colors.border.copy(alpha = 0.6f),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        if (uiState.activeResultCount > 0) {
                            LazyColumn(
                                contentPadding = PaddingValues(vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                when (uiState.currentTab) {
                                    WorkspaceSearchTab.ClassName -> {
                                        items(
                                            items = uiState.classSearch.response.items,
                                            key = { it.className },
                                        ) { item ->
                                            WorkspaceClassSearchResultCard(
                                                result = item,
                                                onClick = {
                                                    onOpenClassResult(item)
                                                    onDismissRequest()
                                                },
                                            )
                                        }
                                    }

                                    WorkspaceSearchTab.StringLiteral -> {
                                        items(
                                            items = uiState.stringSearch.response.items,
                                            key = { it.methodDescriptor },
                                        ) { item ->
                                            WorkspaceStringSearchResultCard(
                                                result = item,
                                                onClick = {
                                                    onOpenStringResult(item)
                                                    onDismissRequest()
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (
                            uiState.isCurrentTabSearching ||
                            !activeErrorMessage.isNullOrBlank() ||
                            uiState.activeHasSearched
                        ) {
                            WorkspaceSearchStateText(
                                text = when {
                                    uiState.isCurrentTabSearching -> "正在查询，请稍候..."
                                    !activeErrorMessage.isNullOrBlank() -> "搜索失败，请调整关键词后重试。"
                                    else -> "未找到匹配的${uiState.currentTab.itemLabel}"
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (resultCountText != null) {
                            Text(
                                text = resultCountText,
                                style = ShadcnTheme.textStyles.bodySmall.copy(
                                    color = ShadcnTheme.colors.mutedForeground.copy(alpha = 0.92f),
                                ),
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        OutlineButton(
                            onClick = onDismissRequest,
                        ) {
                            Text(
                                text = "关闭",
                            )
                        }
                    }
                }
            }
        }
    }
}
