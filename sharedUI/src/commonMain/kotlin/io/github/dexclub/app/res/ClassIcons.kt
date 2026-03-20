package io.github.dexclub.app.res

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.dexclub.app.res.icons.AbstractClass
import io.github.dexclub.app.res.icons.AnnotationClass
import io.github.dexclub.app.res.icons.EnumClass
import io.github.dexclub.app.res.icons.InterfaceClass
import io.github.dexclub.app.res.icons.JavaClass
import io.github.dexclub.app.res.icons.MixedAbstractClass
import io.github.dexclub.app.res.icons.MixedAnnotationClass
import io.github.dexclub.app.res.icons.MixedClass
import io.github.dexclub.app.res.icons.MixedEnumClass
import io.github.dexclub.app.res.icons.MixedInterfaceClass
import io.github.dexclub.app.res.icons.MixedRecordClass
import io.github.dexclub.app.res.icons.RecordClass
import io.github.dexclub.core.workspace.ClassVisualKind

fun resolveClassIcon(
    visualKind: ClassVisualKind?,
): ImageVector {
    return when (visualKind) {
        ClassVisualKind.Annotation -> IconRes.AnnotationClass
        ClassVisualKind.Interface -> IconRes.InterfaceClass
        ClassVisualKind.Enum -> IconRes.EnumClass
        ClassVisualKind.Record -> IconRes.RecordClass
        ClassVisualKind.Abstract -> IconRes.AbstractClass
        ClassVisualKind.Class, null -> IconRes.JavaClass
    }
}

fun resolveMixedClassIcon(
    visualKind: ClassVisualKind?,
): ImageVector {
    return when (visualKind) {
        ClassVisualKind.Annotation -> IconRes.MixedAnnotationClass
        ClassVisualKind.Interface -> IconRes.MixedInterfaceClass
        ClassVisualKind.Enum -> IconRes.MixedEnumClass
        ClassVisualKind.Record -> IconRes.MixedRecordClass
        ClassVisualKind.Abstract -> IconRes.MixedAbstractClass
        ClassVisualKind.Class, null -> IconRes.MixedClass
    }
}
