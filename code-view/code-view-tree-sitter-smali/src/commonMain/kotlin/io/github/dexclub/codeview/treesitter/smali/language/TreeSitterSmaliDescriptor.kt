package io.github.dexclub.codeview.treesitter.smali.language

import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.language.capability.CodeLanguageCapability
import io.github.dexclub.codeview.language.descriptor.CodeLanguageDescriptor
import io.github.dexclub.codeview.treesitter.CodeTreeSitterFamily

public object TreeSitterSmaliDescriptor {
    public val descriptor: CodeLanguageDescriptor = CodeLanguageDescriptor(
        languageId = CodeLanguageId("smali"),
        familyId = CodeTreeSitterFamily.familyId,
        displayName = "Smali",
        aliases = setOf("smali"),
        fileExtensions = setOf("smali"),
        capabilities = setOf(
            CodeLanguageCapability.Highlight,
            CodeLanguageCapability.Navigation,
        ),
    )
}
