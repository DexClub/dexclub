package io.github.dexclub.app.scene.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboard
import io.github.dexclub.core.navigation.NavigateRequestContext
import io.github.dexclub.lang.SemanticNode
import io.github.dexclub.utils.SignatureUtils
import io.github.shadcn.ui.compose.ContextMenu
import io.github.shadcn.ui.compose.ContextMenuItem
import io.github.shadcn.ui.compose.copyText
import kotlinx.coroutines.launch

@Composable
internal fun CodeContextMenu(
    currentClassName: String,
    selectedText: String,
    onSelectAll: () -> Unit,
    expanded: Boolean,
    position: Offset,
    navigateContext: NavigateRequestContext?,
    onNavigateToDefinition: (NavigateRequestContext) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val semanticSignatureToCopy = navigateContext?.semanticNode?.signatureToCopy(currentClassName)
    val semanticCopyLabel = navigateContext?.semanticNode?.signatureCopyLabel()

    fun copyValue(
        text: String,
        label: String,
    ) {
        if (text.isEmpty()) return
        scope.launch {
            clipboard.copyText(text, label = label)
        }
    }

    ContextMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        position = position,
        items = buildList {
            if (semanticSignatureToCopy != null && semanticCopyLabel != null) {
                add(
                    ContextMenuItem.Item(
                        label = semanticCopyLabel,
                        onClick = {
                            copyValue(
                                text = semanticSignatureToCopy,
                                label = "semantic_signature",
                            )
                        },
                    )
                )
            }
            if (selectedText.isNotEmpty()) {
                add(
                    ContextMenuItem.Item(
                        label = if (semanticSignatureToCopy != null) "\u590d\u5236\u9009\u4e2d\u6587\u672c" else "\u590d\u5236",
                        onClick = {
                            copyValue(
                                text = selectedText,
                                label = "selected_text",
                            )
                        },
                    )
                )
            }
            add(
                ContextMenuItem.Item(
                    label = "\u5168\u9009",
                    onClick = onSelectAll,
                )
            )
            if (navigateContext != null) {
                add(
                    ContextMenuItem.Item(
                        label = "\u66f4\u591a\u64cd\u4f5c",
                        children = listOf(
                            ContextMenuItem.Item(
                                label = "\u8df3\u8f6c\u5b9a\u4e49",
                                onClick = { onNavigateToDefinition(navigateContext) },
                            ),
                            ContextMenuItem.Item(
                                label = "\u67e5\u627e\u5f15\u7528",
                                onClick = {},
                            ),
                        ),
                    )
                )
            }
        },
    )
}

private fun SemanticNode.signatureCopyLabel(): String? {
    return when (kind) {
        "method" -> "\u590d\u5236\u65b9\u6cd5\u7b7e\u540d"
        "class" -> "\u590d\u5236\u7c7b\u7b7e\u540d"
        else -> null
    }
}

private fun SemanticNode.signatureToCopy(currentClassName: String): String? {
    return when (kind) {
        "method" -> methodSignatureToCopy(currentClassName)
        "class" -> classSignatureToCopy(currentClassName)
        else -> null
    }
}

private fun SemanticNode.methodSignatureToCopy(currentClassName: String): String? {
    val normalizedDescriptor = descriptor.trim()
    if (normalizedDescriptor.contains("->")) {
        return normalizedDescriptor
    }

    val ownerDescriptor = resolveOwnerDescriptor(currentClassName)
    return when {
        ownerDescriptor != null && normalizedDescriptor.startsWith("(") && name.isNotBlank() -> {
            "$ownerDescriptor->$name$normalizedDescriptor"
        }

        normalizedDescriptor.isNotBlank() -> "$name$normalizedDescriptor"
        name.isNotBlank() -> name
        else -> null
    }
}

private fun SemanticNode.classSignatureToCopy(currentClassName: String): String? {
    val normalizedDescriptor = descriptor.trim()
    if (normalizedDescriptor.startsWith('L') && normalizedDescriptor.endsWith(';')) {
        return normalizedDescriptor
    }
    if (lang.equals("smali", ignoreCase = true) && name.startsWith('L') && name.endsWith(';')) {
        return name
    }

    val effectiveClassName = when {
        owner.isNotBlank() && name.isNotBlank() -> buildJavaClassName(
            relativeOwner = owner,
            currentClassName = currentClassName,
            leafName = name,
        )

        name.isNotBlank() -> buildJavaClassName(
            relativeOwner = "",
            currentClassName = currentClassName,
            leafName = name,
        )

        else -> currentClassName
    }
    return effectiveClassName.takeIf { it.isNotBlank() }?.let(SignatureUtils::typeSignature)
}

private fun SemanticNode.resolveOwnerDescriptor(currentClassName: String): String? {
    val normalizedOwner = owner.trim()
    if (normalizedOwner.startsWith('L') && normalizedOwner.endsWith(';')) {
        return normalizedOwner
    }
    if (lang.equals("smali", ignoreCase = true) && normalizedOwner.isNotBlank()) {
        return SignatureUtils.typeSignature(
            normalizedOwner
                .removePrefix("L")
                .removeSuffix(";")
                .replace('/', '.'),
        )
    }

    val effectiveClassName = when {
        normalizedOwner.isNotBlank() -> buildJavaClassName(
            relativeOwner = normalizedOwner,
            currentClassName = currentClassName,
            leafName = null,
        )

        else -> currentClassName
    }
    return effectiveClassName.takeIf { it.isNotBlank() }?.let(SignatureUtils::typeSignature)
}

private fun buildJavaClassName(
    relativeOwner: String,
    currentClassName: String,
    leafName: String?,
): String {
    if (relativeOwner.startsWith('L') && relativeOwner.endsWith(';')) {
        return relativeOwner
            .removePrefix("L")
            .removeSuffix(";")
            .replace('/', '.')
    }

    val normalizedCurrentClassName = currentClassName
        .removePrefix("L")
        .removeSuffix(";")
        .replace('/', '.')
    val packageName = normalizedCurrentClassName.substringBeforeLast('.', "")

    val ownerSegments = relativeOwner
        .split('.')
        .filter { it.isNotBlank() }
    val classSegments = buildList {
        addAll(ownerSegments)
        if (!leafName.isNullOrBlank() && ownerSegments.lastOrNull() != leafName) {
            add(leafName)
        }
    }
    val binarySimpleName = when {
        classSegments.isNotEmpty() -> classSegments.joinToString("$")
        else -> normalizedCurrentClassName.substringAfterLast('.')
    }
    return if (packageName.isBlank()) {
        binarySimpleName
    } else {
        "$packageName.$binarySimpleName"
    }
}
