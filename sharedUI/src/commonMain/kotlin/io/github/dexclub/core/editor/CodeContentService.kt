package io.github.dexclub.core.editor

import io.github.dexclub.database.editorsession.CodeContentCache
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.exists

typealias CodeContentWarnHandler = (String, Throwable?) -> Unit

fun buildEditorContentKey(tabId: String, kind: String): String {
    return "$tabId#$kind"
}

class CodeContentService(
    private val openTabService: OpenTabService,
    private val codeContentCache: CodeContentCache = CodeContentCache(),
    private val onWarn: CodeContentWarnHandler = { _, _ -> },
) {
    suspend fun loadTabContents(
        tabId: String,
        exportCodePathForClass: ExportCodePathForClass,
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()

        val contents = openTabService.loadRequiredContents(
            tabId = tabId,
            exportCodePathForClass = exportCodePathForClass,
        )

        for ((kind, content) in contents) {
            val cacheKey = buildEditorContentKey(tabId, kind)
            val text = loadOpenTabContent(content)
            if (text != null) {
                result[cacheKey] = text
            }
        }

        return result
    }

    private suspend fun loadOpenTabContent(content: EditorSessionContentRecord): String? {
        val file = PlatformFile(content.codePath)
        if (!file.exists()) {
            warn("代码文件不存在: ${file.absolutePath()}")
            return null
        }

        val text = codeContentCache.getCachedContent(file.absolutePath())
            ?: codeContentCache.readFileContent(file)

        if (text.isBlank()) {
            warn("代码文件内容为空: ${file.absolutePath()}")
            return null
        }

        return text
    }

    private fun warn(text: String, throwable: Throwable? = null) {
        onWarn(text, throwable)
    }
}
