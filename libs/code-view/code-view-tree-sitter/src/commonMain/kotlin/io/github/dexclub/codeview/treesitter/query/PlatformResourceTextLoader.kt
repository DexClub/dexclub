package io.github.dexclub.codeview.treesitter.query

internal expect object PlatformResourceTextLoader {
    fun loadTextResource(resourcePath: String): String
}
