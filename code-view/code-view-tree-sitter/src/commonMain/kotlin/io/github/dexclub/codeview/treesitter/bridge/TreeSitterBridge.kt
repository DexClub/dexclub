package io.github.dexclub.codeview.treesitter.bridge

import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Parser
import io.github.treesitter.ktreesitter.Tree

public typealias TSLanguage = Language
public typealias TSNode = Node
public typealias TSParser = Parser
public typealias TSTree = Tree

public fun TSParser.parseString(oldTree: TSTree?, source: String): TSTree {
    return parse(source, oldTree)
}

public fun TSNode.getChild(index: Int): TSNode {
    require(index >= 0) { "Child index cannot be negative: $index" }
    return child(index.toUInt()) ?: throw IndexOutOfBoundsException("Child index out of bounds: $index")
}

public fun TSNode.getChild(index: UInt): TSNode {
    return child(index) ?: throw IndexOutOfBoundsException("Child index out of bounds: $index")
}

public infix fun Int.until(to: UInt): IntRange {
    return this until to.toInt()
}
