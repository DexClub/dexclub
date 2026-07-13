package io.github.dexclub.core.impl.dex

import com.android.tools.smali.dexlib2.iface.Method
import io.github.dexclub.core.api.dex.DexExportError
import io.github.dexclub.core.api.dex.DexExportErrorReason

internal data class ParsedMethodSignature(
    val classSignature: String,
    val methodName: String,
    val descriptor: String,
)

internal fun parseMethodSignature(methodSignature: String): ParsedMethodSignature {
    val trimmed = methodSignature.trim()
    val arrowIndex = trimmed.indexOf("->")
    if (arrowIndex <= 0) {
        throw DexExportError(
            reason = DexExportErrorReason.InvalidMethodSignature,
            message = "invalid method signature: $methodSignature",
        )
    }
    val classPart = trimmed.substring(0, arrowIndex)
    val memberPart = trimmed.substring(arrowIndex + 2)
    val descriptorStart = memberPart.indexOf('(')
    if (descriptorStart <= 0 || !memberPart.contains(')')) {
        throw DexExportError(
            reason = DexExportErrorReason.InvalidMethodSignature,
            message = "invalid method signature: $methodSignature",
        )
    }
    val methodName = memberPart.substring(0, descriptorStart)
    val descriptor = memberPart.substring(descriptorStart)
    if (methodName.isBlank() || descriptor.endsWith(")")) {
        throw DexExportError(
            reason = DexExportErrorReason.InvalidMethodSignature,
            message = "invalid method signature: $methodSignature",
        )
    }
    return ParsedMethodSignature(
        classSignature = toTypeSignature(classPart),
        methodName = methodName,
        descriptor = descriptor,
    )
}

internal fun methodDescriptorOf(method: Method): String =
    buildString {
        append('(')
        method.parameterTypes.forEach { append(it) }
        append(')')
        append(method.returnType)
    }

internal fun extractMethodBlock(
    classSmali: String,
    methodName: String,
    descriptor: String,
): String {
    val suffix = "$methodName$descriptor"
    val captured = mutableListOf<String>()
    var collecting = false
    for (line in classSmali.lineSequence()) {
        val trimmed = line.trimStart()
        if (!collecting) {
            if (trimmed.startsWith(".method ") && trimmed.removePrefix(".method ").endsWith(suffix)) {
                collecting = true
                captured += line
            }
            continue
        }

        captured += line
        if (trimmed == ".end method") {
            return captured.joinToString(separator = "\n")
        }
    }
    throw DexExportError(
        reason = DexExportErrorReason.MethodNotFound,
        message = "method block not found in rendered class: $methodName$descriptor",
    )
}

internal fun buildMethodClassShell(classSmali: String, methodBlock: String): String {
    val header = mutableListOf<String>()
    for (line in classSmali.lineSequence()) {
        val trimmed = line.trimStart()
        if (
            trimmed.startsWith(".annotation ") ||
            trimmed.startsWith(".field ") ||
            trimmed.startsWith(".method ")
        ) {
            break
        }
        header += line
    }

    val headerText = header.joinToString(separator = "\n").trimEnd()
    return buildString {
        append(headerText)
        if (headerText.isNotEmpty()) {
            append("\n\n")
        }
        append(methodBlock.trimEnd())
        append('\n')
    }
}

internal fun ensureTrailingNewline(text: String): String =
    if (text.endsWith('\n')) text else "$text\n"
