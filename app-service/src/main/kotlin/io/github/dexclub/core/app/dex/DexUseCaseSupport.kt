package io.github.dexclub.core.app.dex

import io.github.dexclub.core.api.shared.MethodSmaliMode
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceService
import io.github.dexclub.core.app.session.ClassHandleRef
import io.github.dexclub.core.app.session.MethodHandleRef
import io.github.dexclub.core.app.session.SourceBackedHandleRef
import io.github.dexclub.core.app.session.TargetSession
import io.github.dexclub.core.app.session.TargetSessionService
import io.github.dexclub.core.app.session.TargetWorkspaceResolver
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText

internal class DexUseCaseSupport(
    private val workspaceService: WorkspaceService,
    private val sessionService: TargetSessionService,
) {
    private val workspaceResolver = TargetWorkspaceResolver(workspaceService, sessionService)

    fun resolveExecutionContext(
        workspace: WorkspaceContext?,
        sessionId: String?,
        workdir: String?,
    ) = workspaceResolver.resolve(
        workspace = workspace,
        sessionId = sessionId,
        workdir = workdir,
    )

    fun resolveMethodReference(
        session: TargetSession?,
        methodHandle: String?,
        descriptor: String?,
        sourcePath: String?,
        sourceEntry: String?,
    ): MethodHandleRef? {
        val normalizedHandle = methodHandle?.trim()?.ifEmpty { null }
        if (normalizedHandle != null) {
            requireNotNull(session) { "method_handle requires session_id" }
            return sessionService.getMethodHandle(session.sessionId, normalizedHandle)
                ?: throw IllegalArgumentException(
                    "method_handle not found. Handles must come from a previous dexclub result in the same session; do not construct placeholder handles manually",
                )
        }

        val normalizedDescriptor = descriptor?.trim()?.ifEmpty { null } ?: return null
        return MethodHandleRef(
            sessionId = session?.sessionId.orEmpty(),
            descriptor = normalizedDescriptor,
            sourcePath = sourcePath?.trim()?.ifEmpty { null },
            sourceEntry = sourceEntry?.trim()?.ifEmpty { null },
        )
    }

    fun resolveClassReference(
        session: TargetSession?,
        classHandle: String?,
        descriptor: String?,
        sourcePath: String?,
        sourceEntry: String?,
    ): ClassHandleRef? {
        val normalizedHandle = classHandle?.trim()?.ifEmpty { null }
        if (normalizedHandle != null) {
            requireNotNull(session) { "class_handle requires session_id" }
            return sessionService.getClassHandle(session.sessionId, normalizedHandle)
                ?: throw IllegalArgumentException(
                    "class_handle not found. Handles must come from a previous dexclub result in the same session; do not construct placeholder handles manually",
                )
        }

        val normalizedDescriptor = descriptor?.trim()?.ifEmpty { null } ?: return null
        return ClassHandleRef(
            sessionId = session?.sessionId.orEmpty(),
            descriptor = normalizedDescriptor,
            sourcePath = sourcePath?.trim()?.ifEmpty { null },
            sourceEntry = sourceEntry?.trim()?.ifEmpty { null },
        )
    }

    inline fun exportTextFile(workspace: WorkspaceContext, block: (Path) -> Unit): String {
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
}

internal fun buildSourceLocator(
    ref: SourceBackedHandleRef?,
    sourcePath: String?,
    sourceEntry: String?,
): SourceLocator =
    SourceLocator(
        sourcePath = sourcePath?.trim()?.ifEmpty { null } ?: ref?.sourcePath,
        sourceEntry = sourceEntry?.trim()?.ifEmpty { null } ?: ref?.sourceEntry,
    )

internal fun String?.toMethodSmaliMode(): MethodSmaliMode =
    when (this?.trim()?.lowercase()) {
        null, "", "snippet" -> MethodSmaliMode.Snippet
        "class" -> MethodSmaliMode.Class
        else -> throw IllegalArgumentException("Unsupported smali mode: $this. Supported modes: snippet, class")
    }
