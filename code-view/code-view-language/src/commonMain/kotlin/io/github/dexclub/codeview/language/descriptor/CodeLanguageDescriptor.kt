package io.github.dexclub.codeview.language.descriptor

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.language.CodeLanguageFamilyId
import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.language.capability.CodeLanguageCapability

@CodeViewApi
public data class CodeLanguageDescriptor(
    public val languageId: CodeLanguageId,
    public val familyId: CodeLanguageFamilyId,
    public val displayName: String,
    public val aliases: Set<String> = emptySet(),
    public val fileExtensions: Set<String> = emptySet(),
    public val fileNames: Set<String> = emptySet(),
    public val mimeTypes: Set<String> = emptySet(),
    public val capabilities: Set<CodeLanguageCapability> = emptySet(),
) {
    init {
        require(displayName.isNotBlank()) { "displayName 不能为空" }
    }
}
