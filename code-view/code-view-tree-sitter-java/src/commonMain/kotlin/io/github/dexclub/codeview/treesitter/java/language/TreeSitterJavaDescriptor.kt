package io.github.dexclub.codeview.treesitter.java.language

import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.language.capability.CodeLanguageCapability
import io.github.dexclub.codeview.language.descriptor.CodeLanguageDescriptor
import io.github.dexclub.codeview.treesitter.CodeTreeSitterFamily

public object TreeSitterJavaDescriptor {
    public val descriptor: CodeLanguageDescriptor = CodeLanguageDescriptor(
        languageId = CodeLanguageId("java"),
        familyId = CodeTreeSitterFamily.familyId,
        displayName = "Java",
        aliases = setOf("java"),
        fileExtensions = setOf("java"),
        capabilities = setOf(
            CodeLanguageCapability.Highlight,
            CodeLanguageCapability.Structure,
            CodeLanguageCapability.Navigation,
        ),
    )
}
