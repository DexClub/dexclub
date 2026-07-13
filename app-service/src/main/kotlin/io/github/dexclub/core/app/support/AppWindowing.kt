package io.github.dexclub.core.app.support

data class WindowSlice<T>(
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
    val items: List<T>,
)

fun <T> applyWindowSlice(
    items: List<T>,
    offset: Int?,
    limit: Int?,
): WindowSlice<T> {
    val normalizedOffset = offset ?: 0
    require(normalizedOffset >= 0) { "offset must be non-negative" }
    require(limit == null || limit > 0) { "limit must be positive when specified" }
    if (normalizedOffset >= items.size) {
        val effectiveLimit = limit ?: 0
        return WindowSlice(
            total = items.size,
            offset = normalizedOffset,
            limit = effectiveLimit,
            hasMore = false,
            items = emptyList(),
        )
    }

    val toIndex = if (limit == null) items.size else minOf(items.size, normalizedOffset + limit)
    val slice = items.subList(normalizedOffset, toIndex)
    return WindowSlice(
        total = items.size,
        offset = normalizedOffset,
        limit = limit ?: slice.size,
        hasMore = toIndex < items.size,
        items = slice,
    )
}
