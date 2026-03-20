package io.github.dexclub.codeview.treesitter.query

import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.treesitter.CodeTreeSitterFamily

object TreeSitterQueryLoader {
    fun load(
        languageId: CodeLanguageId,
        fileName: String,
    ): String = PlatformResourceTextLoader.loadTextResource(
        resourcePath = CodeTreeSitterFamily.queryResourcePath(
            languageId = languageId,
            fileName = fileName,
        ),
    )
}
