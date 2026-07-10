package io.github.dexclub.mcp

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.ExportClassJavaRequest
import io.github.dexclub.core.api.dex.ExportClassSmaliRequest
import io.github.dexclub.core.api.dex.ExportMethodJavaRequest
import io.github.dexclub.core.api.dex.ExportMethodSmaliRequest
import io.github.dexclub.core.api.dex.InspectMethodRequest
import io.github.dexclub.core.api.dex.MethodDetailSection
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.shared.MethodSmaliMode
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.workspace.WorkspaceContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText

internal fun McpApp.inspectMethod(
    workspace: WorkspaceContext,
    descriptor: String,
    source: SourceLocator = SourceLocator(),
    includes: Set<MethodDetailSection>,
) = services.dex.inspectMethod(
    workspace = workspace,
    request = InspectMethodRequest(
        descriptor = descriptor,
        source = source,
        includes = includes,
    ),
)

internal fun McpApp.exportMethodJavaText(
    workspace: WorkspaceContext,
    descriptor: String,
    source: SourceLocator = SourceLocator(),
    mode: String? = null,
): String {
    require(mode.isNullOrBlank()) { "mode is only supported for export_method_smali" }
    return exportTextFile(workspace) {
        services.dex.exportMethodJava(
            workspace = workspace,
            request = ExportMethodJavaRequest(
                methodSignature = descriptor,
                source = source,
                outputPath = it.toString(),
            ),
        )
    }
}

internal fun McpApp.exportMethodSmaliText(
    workspace: WorkspaceContext,
    descriptor: String,
    source: SourceLocator = SourceLocator(),
    mode: String? = null,
): String {
    val smaliMode = when (mode?.trim()?.lowercase()) {
        null, "", "snippet" -> MethodSmaliMode.Snippet
        "class" -> MethodSmaliMode.Class
        else -> throw IllegalArgumentException("Unsupported smali mode: $mode. Supported modes: snippet, class")
    }
    return exportTextFile(workspace) {
        services.dex.exportMethodSmali(
            workspace = workspace,
            request = ExportMethodSmaliRequest(
                methodSignature = descriptor,
                source = source,
                outputPath = it.toString(),
                mode = smaliMode,
            ),
        )
    }
}

internal fun McpApp.exportClassJavaText(
    workspace: WorkspaceContext,
    descriptor: String,
    source: SourceLocator = SourceLocator(),
): String = exportTextFile(workspace) {
    services.dex.exportClassJava(
        workspace = workspace,
        request = ExportClassJavaRequest(
            className = descriptor,
            source = source,
            outputPath = it.toString(),
        ),
    )
}

internal fun McpApp.exportClassSmaliText(
    workspace: WorkspaceContext,
    descriptor: String,
    source: SourceLocator = SourceLocator(),
): String = exportTextFile(workspace) {
    services.dex.exportClassSmali(
        workspace = workspace,
        request = ExportClassSmaliRequest(
            className = descriptor,
            source = source,
            outputPath = it.toString(),
        ),
    )
}

internal fun McpApp.findClassesUsingStrings(
    workspace: WorkspaceContext,
    containsAnyStrings: List<String>,
    containsAllStrings: List<String>,
    offset: Int? = null,
    limit: Int? = null,
): WindowedClassHits {
    if (containsAnyStrings.isEmpty() && containsAllStrings.isEmpty()) {
        throw IllegalArgumentException("At least one string filter is required")
    }

    val anyHits = if (containsAnyStrings.isNotEmpty()) {
        services.dex.findClassesUsingStrings(
            workspace = workspace,
            request = buildFindClassesUsingStringsRequest(
                strings = containsAnyStrings,
                requireAll = false,
            ),
        )
    } else {
        emptyList()
    }

    val allHits = if (containsAllStrings.isNotEmpty()) {
        services.dex.findClassesUsingStrings(
            workspace = workspace,
            request = buildFindClassesUsingStringsRequest(
                strings = containsAllStrings,
                requireAll = true,
            ),
        )
    } else {
        emptyList()
    }

    val combined = when {
        containsAnyStrings.isNotEmpty() && containsAllStrings.isNotEmpty() ->
            anyHits.intersect(allHits.toSet()).toList()
        containsAnyStrings.isNotEmpty() -> anyHits
        else -> allHits
    }.sortedWith(
        compareBy<ClassHit>(
            { it.className },
            { it.sourcePath.orEmpty() },
            { it.sourceEntry.orEmpty() },
        ),
    )

    return applyClassWindow(
        items = combined,
        offset = offset,
        limit = limit,
    )
}

internal fun McpApp.findMethodsUsingStrings(
    workspace: WorkspaceContext,
    containsAnyStrings: List<String>,
    containsAllStrings: List<String>,
    offset: Int? = null,
    limit: Int? = null,
): WindowedMethodHits {
    if (containsAnyStrings.isEmpty() && containsAllStrings.isEmpty()) {
        throw IllegalArgumentException("At least one string filter is required")
    }

    val anyHits = if (containsAnyStrings.isNotEmpty()) {
        services.dex.findMethodsUsingStrings(
            workspace = workspace,
            request = buildFindMethodsUsingStringsRequest(
                strings = containsAnyStrings,
                requireAll = false,
            ),
        )
    } else {
        emptyList()
    }

    val allHits = if (containsAllStrings.isNotEmpty()) {
        services.dex.findMethodsUsingStrings(
            workspace = workspace,
            request = buildFindMethodsUsingStringsRequest(
                strings = containsAllStrings,
                requireAll = true,
            ),
        )
    } else {
        emptyList()
    }

    val combined = when {
        containsAnyStrings.isNotEmpty() && containsAllStrings.isNotEmpty() ->
            anyHits.intersect(allHits.toSet()).toList()
        containsAnyStrings.isNotEmpty() -> anyHits
        else -> allHits
    }.sortedWith(
        compareBy<MethodHit>(
            { it.className },
            { it.methodName },
            { it.descriptor },
            { it.sourcePath.orEmpty() },
            { it.sourceEntry.orEmpty() },
        ),
    )

    return applyMethodWindow(
        items = combined,
        offset = offset,
        limit = limit,
    )
}

internal fun McpApp.findMethods(
    workspace: WorkspaceContext,
    classNameContains: String? = null,
    methodNameContains: String? = null,
    descriptorContains: String? = null,
    offset: Int? = null,
    limit: Int? = null,
): WindowedMethodHits {
    val normalizedDescriptor = descriptorContains?.trim()?.ifEmpty { null }
    val request = buildFindMethodsRequest(
        classNameContains = classNameContains,
        methodNameContains = methodNameContains,
    )
    val baseHits = services.dex.findMethods(
        workspace = workspace,
        request = request,
    )
    val filtered = if (normalizedDescriptor == null) {
        baseHits
    } else {
        baseHits.filter { it.descriptor.contains(normalizedDescriptor, ignoreCase = false) }
    }.sortedWith(
        compareBy<MethodHit>(
            { it.className },
            { it.methodName },
            { it.descriptor },
            { it.sourcePath.orEmpty() },
            { it.sourceEntry.orEmpty() },
        ),
    )
    return applyMethodWindow(
        items = filtered,
        offset = offset,
        limit = limit,
    )
}

private inline fun McpApp.exportTextFile(workspace: WorkspaceContext, block: (Path) -> Unit): String {
    val exportTempRoot = Paths.get(
        workspace.dexclubDir,
        "targets",
        workspace.activeTargetId,
        "cache",
        "exports",
        "tmp",
    )
    Files.createDirectories(exportTempRoot)
    val output = Files.createTempFile(exportTempRoot, "mcp-export-", ".txt")
    return try {
        block(output)
        output.readText()
    } finally {
        output.deleteIfExists()
    }
}
