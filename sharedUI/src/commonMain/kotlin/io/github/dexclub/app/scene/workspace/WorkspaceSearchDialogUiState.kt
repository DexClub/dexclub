package io.github.dexclub.app.scene.workspace

data class WorkspaceClassSearchUiState(
    val query: String = "",
    val response: WorkspaceSearchResponse<WorkspaceClassSearchResult> = WorkspaceSearchResponse(items = emptyList()),
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
)

data class WorkspaceStringSearchUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val response: WorkspaceSearchResponse<WorkspaceStringSearchResult> = WorkspaceSearchResponse(items = emptyList()),
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
)

data class WorkspaceSearchDialogUiState(
    val currentTab: WorkspaceSearchTab = WorkspaceSearchTab.ClassName,
    val classSearch: WorkspaceClassSearchUiState = WorkspaceClassSearchUiState(),
    val stringSearch: WorkspaceStringSearchUiState = WorkspaceStringSearchUiState(),
    val searchingTab: WorkspaceSearchTab? = null,
) {
    val activeQuery: String
        get() = when (currentTab) {
            WorkspaceSearchTab.ClassName -> classSearch.query
            WorkspaceSearchTab.StringLiteral -> stringSearch.query
        }

    val activeErrorMessage: String?
        get() = when (currentTab) {
            WorkspaceSearchTab.ClassName -> classSearch.errorMessage
            WorkspaceSearchTab.StringLiteral -> stringSearch.errorMessage
        }

    val activeHasSearched: Boolean
        get() = when (currentTab) {
            WorkspaceSearchTab.ClassName -> classSearch.hasSearched
            WorkspaceSearchTab.StringLiteral -> stringSearch.hasSearched
        }

    val activeResultCount: Int
        get() = when (currentTab) {
            WorkspaceSearchTab.ClassName -> classSearch.response.items.size
            WorkspaceSearchTab.StringLiteral -> stringSearch.response.items.size
        }

    val isCurrentTabSearching: Boolean
        get() = searchingTab == currentTab

    val resultCountText: String?
        get() = when {
            isCurrentTabSearching && activeResultCount > 0 -> "已显示 $activeResultCount 条结果"
            activeHasSearched -> "搜索结果 $activeResultCount 条"
            else -> null
        }
}

internal fun WorkspaceSearchDialogUiState.selectTab(tab: WorkspaceSearchTab): WorkspaceSearchDialogUiState {
    return if (currentTab == tab) this else copy(currentTab = tab)
}

internal fun WorkspaceSearchDialogUiState.updateActiveQuery(query: String): WorkspaceSearchDialogUiState {
    return when (currentTab) {
        WorkspaceSearchTab.ClassName -> copy(
            classSearch = classSearch.copy(
                query = query,
                errorMessage = null,
            ),
        )

        WorkspaceSearchTab.StringLiteral -> copy(
            stringSearch = stringSearch.copy(
                query = query,
                errorMessage = null,
            ),
        )
    }
}

internal fun WorkspaceSearchDialogUiState.withEmptyQueryError(tab: WorkspaceSearchTab): WorkspaceSearchDialogUiState {
    return when (tab) {
        WorkspaceSearchTab.ClassName -> copy(
            classSearch = classSearch.copy(
                errorMessage = "请输入类名关键词",
            ),
        )

        WorkspaceSearchTab.StringLiteral -> copy(
            stringSearch = stringSearch.copy(
                errorMessage = "请输入字符串关键词",
            ),
        )
    }
}

internal fun WorkspaceSearchDialogUiState.markSearchStarted(tab: WorkspaceSearchTab): WorkspaceSearchDialogUiState {
    return when (tab) {
        WorkspaceSearchTab.ClassName -> copy(
            searchingTab = tab,
            classSearch = classSearch.copy(
                errorMessage = null,
            ),
        )

        WorkspaceSearchTab.StringLiteral -> copy(
            searchingTab = tab,
            stringSearch = stringSearch.copy(
                errorMessage = null,
            ),
        )
    }
}

internal fun WorkspaceSearchDialogUiState.withClassSearchSuccess(
    response: WorkspaceSearchResponse<WorkspaceClassSearchResult>,
): WorkspaceSearchDialogUiState {
    return copy(
        searchingTab = null,
        classSearch = classSearch.copy(
            response = response,
            errorMessage = null,
            hasSearched = true,
        ),
    )
}

internal fun WorkspaceSearchDialogUiState.withStringSearchSuccess(
    submittedQuery: String,
    response: WorkspaceSearchResponse<WorkspaceStringSearchResult>,
): WorkspaceSearchDialogUiState {
    return copy(
        searchingTab = null,
        stringSearch = stringSearch.copy(
            submittedQuery = submittedQuery,
            response = response,
            errorMessage = null,
            hasSearched = true,
        ),
    )
}

internal fun WorkspaceSearchDialogUiState.withSearchFailure(
    tab: WorkspaceSearchTab,
    message: String,
): WorkspaceSearchDialogUiState {
    return when (tab) {
        WorkspaceSearchTab.ClassName -> copy(
            searchingTab = null,
            classSearch = classSearch.copy(
                response = WorkspaceSearchResponse(items = emptyList()),
                errorMessage = message,
                hasSearched = true,
            ),
        )

        WorkspaceSearchTab.StringLiteral -> copy(
            searchingTab = null,
            stringSearch = stringSearch.copy(
                response = WorkspaceSearchResponse(items = emptyList()),
                errorMessage = message,
                hasSearched = true,
            ),
        )
    }
}
