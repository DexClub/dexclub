package io.github.dexclub.codeview.treesitter.kotlin.language

import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.language.capability.CodeLanguageCapability
import io.github.dexclub.codeview.language.descriptor.CodeLanguageDescriptor
import io.github.dexclub.codeview.treesitter.CodeTreeSitterFamily

object TreeSitterKotlinDescriptor {
    val descriptor: CodeLanguageDescriptor = CodeLanguageDescriptor(
        languageId = CodeLanguageId("kotlin"),
        familyId = CodeTreeSitterFamily.familyId,
        displayName = "Kotlin",
        aliases = setOf("kotlin", "kt", "kts"),
        fileExtensions = setOf("kt", "kts"),
        capabilities = setOf(
            CodeLanguageCapability.Highlight,
            CodeLanguageCapability.Structure,
            CodeLanguageCapability.Navigation,
            CodeLanguageCapability.Diagnostics,
        ),
    )
}
