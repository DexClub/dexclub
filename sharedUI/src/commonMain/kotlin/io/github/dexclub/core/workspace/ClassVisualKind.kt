package io.github.dexclub.core.workspace

enum class ClassVisualKind {
    Annotation,
    Interface,
    Enum,
    Record,
    Abstract,
    Class,
}

fun resolveClassVisualKind(
    modifiers: Int,
): ClassVisualKind {
    return when {
        modifiers.hasClassModifier(CLASS_MODIFIER_ANNOTATION) -> ClassVisualKind.Annotation
        modifiers.hasClassModifier(CLASS_MODIFIER_INTERFACE) -> ClassVisualKind.Interface
        modifiers.hasClassModifier(CLASS_MODIFIER_ENUM) -> ClassVisualKind.Enum
        modifiers.hasClassModifier(CLASS_MODIFIER_RECORD) -> ClassVisualKind.Record
        modifiers.hasClassModifier(CLASS_MODIFIER_ABSTRACT) -> ClassVisualKind.Abstract
        else -> ClassVisualKind.Class
    }
}

private fun Int.hasClassModifier(
    mask: Int,
): Boolean {
    return this and mask != 0
}

private const val CLASS_MODIFIER_INTERFACE = 0x0200
private const val CLASS_MODIFIER_ABSTRACT = 0x0400
private const val CLASS_MODIFIER_ANNOTATION = 0x2000
private const val CLASS_MODIFIER_ENUM = 0x4000
private const val CLASS_MODIFIER_RECORD = 0x1_0000
