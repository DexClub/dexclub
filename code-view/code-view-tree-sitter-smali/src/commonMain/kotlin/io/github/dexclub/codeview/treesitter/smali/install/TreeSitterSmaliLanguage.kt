package io.github.dexclub.codeview.treesitter.smali.install

import io.github.dexclub.codeview.language.descriptor.CodeLanguageDescriptor
import io.github.dexclub.codeview.language.install.CodeLanguageInstallable
import io.github.dexclub.codeview.language.session.CodeLanguageProviderFactory
import io.github.dexclub.codeview.treesitter.smali.language.TreeSitterSmaliDescriptor
import io.github.dexclub.codeview.treesitter.smali.session.SmaliLanguageProviderFactory

public object TreeSitterSmaliLanguage : CodeLanguageInstallable {
    override val descriptor: CodeLanguageDescriptor = TreeSitterSmaliDescriptor.descriptor
    override val providerFactory: CodeLanguageProviderFactory = SmaliLanguageProviderFactory
}

public fun treeSitterSmaliLanguage(): CodeLanguageInstallable = TreeSitterSmaliLanguage
