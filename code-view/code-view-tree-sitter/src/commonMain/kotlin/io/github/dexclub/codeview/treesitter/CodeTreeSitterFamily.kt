package io.github.dexclub.codeview.treesitter

import io.github.dexclub.codeview.core.language.CodeLanguageFamilyId
import io.github.dexclub.codeview.core.language.CodeLanguageId

object CodeTreeSitterFamily {
    val familyId: CodeLanguageFamilyId = CodeLanguageFamilyId("tree-sitter")

    fun queryResourcePath(
        languageId: CodeLanguageId,
        fileName: String,
    ): String = "io/github/dexclub/codeview/treesitter/${languageId.value}/query/$fileName"
}
