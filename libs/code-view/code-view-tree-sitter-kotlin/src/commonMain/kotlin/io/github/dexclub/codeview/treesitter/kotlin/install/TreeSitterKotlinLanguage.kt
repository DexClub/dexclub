package io.github.dexclub.codeview.treesitter.kotlin.install

import io.github.dexclub.codeview.language.install.CodeLanguageInstallable
import io.github.dexclub.codeview.treesitter.kotlin.language.TreeSitterKotlinDescriptor

object TreeSitterKotlinLanguage : CodeLanguageInstallable {
    override val descriptor = TreeSitterKotlinDescriptor.descriptor
}


fun treeSitterKotlinLanguage(): CodeLanguageInstallable = TreeSitterKotlinLanguage
