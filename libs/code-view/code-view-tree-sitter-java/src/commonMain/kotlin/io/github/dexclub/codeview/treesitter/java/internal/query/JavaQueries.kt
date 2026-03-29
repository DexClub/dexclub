package io.github.dexclub.codeview.treesitter.java.internal.query

import io.github.dexclub.codeview.treesitter.java.language.TreeSitterJavaDescriptor
import io.github.dexclub.codeview.treesitter.query.TreeSitterQueryLoader

internal object JavaQueries {
    fun highlights(): String = TreeSitterQueryLoader.load(
        languageId = TreeSitterJavaDescriptor.descriptor.languageId,
        fileName = "highlights.scm",
    )
}
