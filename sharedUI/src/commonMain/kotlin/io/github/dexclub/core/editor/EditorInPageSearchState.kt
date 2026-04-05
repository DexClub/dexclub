package io.github.dexclub.core.editor

data class EditorInPageSearchState(
    val queryText: String = "",
    val matchQuery: String = "",
    val source: EditorInPageSearchSource = EditorInPageSearchSource.Manual,
    val activeMatchIndex: Int = 0,
    val isVisible: Boolean = false,
    val requestFocusToken: Long = 0L,
)

enum class EditorInPageSearchSource {
    Manual,
    DexKitString,
    DexKitClass,
}
