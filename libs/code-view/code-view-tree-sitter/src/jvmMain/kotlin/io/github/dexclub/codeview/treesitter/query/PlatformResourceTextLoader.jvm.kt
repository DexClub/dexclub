package io.github.dexclub.codeview.treesitter.query

internal actual object PlatformResourceTextLoader {
    actual fun loadTextResource(resourcePath: String): String {
        val normalizedPath = resourcePath.trimStart('/')
        val loader = PlatformResourceTextLoader::class.java.classLoader
            ?: error("JVM ClassLoader 不可用: $normalizedPath")
        val stream = loader.getResourceAsStream(normalizedPath)
            ?: error("未找到 Tree-sitter query 资源: $normalizedPath")
        return stream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
    }
}
