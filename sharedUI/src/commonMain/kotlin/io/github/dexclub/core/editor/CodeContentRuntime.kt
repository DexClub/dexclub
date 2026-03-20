package io.github.dexclub.core.editor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class CodeContentRuntime(
    private val codeContentService: CodeContentService,
) {
    private val _codeContents = MutableStateFlow<Map<String, String>>(emptyMap())

    val codeContents: StateFlow<Map<String, String>> = _codeContents

    suspend fun loadTabContents(
        tabId: String,
        exportCodePathForClass: ExportCodePathForClass,
    ) {
        val loaded = withContext(Dispatchers.IO) {
            codeContentService.loadTabContents(
                tabId = tabId,
                exportCodePathForClass = exportCodePathForClass,
            )
        }

        if (loaded.isEmpty()) return

        _codeContents.update { current -> current + loaded }
    }

    fun clearActiveHighlighters() {
        // no-op: highlight pipeline removed in P8
    }

    fun clearTabRuntimeStates(tabIds: Set<String>) {
        if (tabIds.isEmpty()) return
        _codeContents.update { current ->
            filterRuntimeStateKeys(current, tabIds)
        }
    }
}

internal fun <T> filterRuntimeStateKeys(
    current: Map<String, T>,
    tabIds: Set<String>,
): Map<String, T> {
    if (tabIds.isEmpty()) return current
    return current.filterKeys { key ->
        tabIds.none { tabId -> key.startsWith("$tabId#") }
    }
}
