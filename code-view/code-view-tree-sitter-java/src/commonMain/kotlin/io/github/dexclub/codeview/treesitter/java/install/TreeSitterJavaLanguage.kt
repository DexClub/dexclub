package io.github.dexclub.codeview.treesitter.java.install

import io.github.dexclub.codeview.language.descriptor.CodeLanguageDescriptor
import io.github.dexclub.codeview.language.install.CodeLanguageInstallable
import io.github.dexclub.codeview.language.session.CodeLanguageProviderFactory
import io.github.dexclub.codeview.treesitter.java.language.TreeSitterJavaDescriptor
import io.github.dexclub.codeview.treesitter.java.session.JavaLanguageProviderFactory

public object TreeSitterJavaLanguage : CodeLanguageInstallable {
    override val descriptor: CodeLanguageDescriptor = TreeSitterJavaDescriptor.descriptor
    override val providerFactory: CodeLanguageProviderFactory = JavaLanguageProviderFactory
}

public fun treeSitterJavaLanguage(): CodeLanguageInstallable = TreeSitterJavaLanguage
