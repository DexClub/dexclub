package io.github.dexclub.core.editor

internal fun defaultKindPriorities(tabId: String): List<EditorSessionKindPriorityRecord> {
    return listOf(
        EditorSessionKindPriorityRecord(
            tabId = tabId,
            kind = EDITOR_SESSION_KIND_SMALI,
            priority = 0,
        ),
        EditorSessionKindPriorityRecord(
            tabId = tabId,
            kind = EDITOR_SESSION_KIND_JAVA,
            priority = 1,
        ),
    )
}
