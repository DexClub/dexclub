package io.github.dexclub.core.search

import io.github.dexclub.core.workspace.ClassVisualKind

data class ClassSearchHit(
    val className: String,
    val classVisualKind: ClassVisualKind,
    val descriptor: String,
)

data class StringSearchHit(
    val className: String,
    val classVisualKind: ClassVisualKind,
    val methodDescriptor: String,
    val methodName: String,
    val methodDisplaySignature: String,
    val matchedString: String,
    val matchedStrings: List<String> = listOf(matchedString),
)
