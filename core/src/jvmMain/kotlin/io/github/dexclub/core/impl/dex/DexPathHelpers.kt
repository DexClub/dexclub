package io.github.dexclub.core.impl.dex

import io.github.dexclub.core.api.shared.SourceLocator

internal fun dexEntrySortKey(name: String): Pair<Int, String> {
    if (name == "classes.dex") {
        return 1 to name
    }
    val suffix = name.removePrefix("classes").removeSuffix(".dex")
    return if (suffix.toIntOrNull() != null) {
        suffix.toInt() to name
    } else {
        Int.MAX_VALUE to name
    }
}

internal fun SourceLocator.describe(): String? =
    when {
        sourcePath != null && sourceEntry != null -> "source_path=$sourcePath, source_entry=$sourceEntry"
        sourcePath != null -> "source_path=$sourcePath"
        sourceEntry != null -> "source_entry=$sourceEntry"
        else -> null
    }
