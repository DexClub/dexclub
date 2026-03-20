package io.github.dexclub.codeview.treesitter.kotlin.internal.query

import io.github.dexclub.codeview.treesitter.kotlin.language.TreeSitterKotlinDescriptor
import io.github.dexclub.codeview.treesitter.query.TreeSitterQueryLoader

object KotlinQueries {
    fun highlights(): String = TreeSitterQueryLoader.load(
        languageId = TreeSitterKotlinDescriptor.descriptor.languageId,
        fileName = "highlights.scm",
    )
}
