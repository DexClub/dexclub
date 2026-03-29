package io.github.dexclub.codeview.treesitter.smali.internal.query

import io.github.dexclub.codeview.treesitter.query.TreeSitterQueryLoader
import io.github.dexclub.codeview.treesitter.smali.language.TreeSitterSmaliDescriptor

internal object SmaliQueries {
    fun highlights(): String = TreeSitterQueryLoader.load(
        languageId = TreeSitterSmaliDescriptor.descriptor.languageId,
        fileName = "highlights.scm",
    )
}
