package io.github.dexclub.codeview.treesitter.query

import java.io.FileNotFoundException

import io.github.dexclub.codeview.treesitter.CodeTreeSitterAndroid

internal actual object PlatformResourceTextLoader {
    actual fun loadTextResource(resourcePath: String): String {
        val normalizedPath = resourcePath.trimStart('/')
        val assetManager = CodeTreeSitterAndroid.requireAssetManager()
        val stream = try {
            assetManager.open(normalizedPath)
        } catch (_: FileNotFoundException) {
            error("未找到 Android Tree-sitter query 资源: $normalizedPath")
        }
        return stream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
    }
}
