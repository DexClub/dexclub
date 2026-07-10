package io.github.dexclub.core.impl.dex

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.DexAnalysisService
import io.github.dexclub.core.api.dex.ExportClassDexRequest
import io.github.dexclub.core.api.dex.ExportClassJavaRequest
import io.github.dexclub.core.api.dex.ExportClassSmaliRequest
import io.github.dexclub.core.api.dex.ExportMethodDexRequest
import io.github.dexclub.core.api.dex.ExportMethodJavaRequest
import io.github.dexclub.core.api.dex.ExportMethodSmaliRequest
import io.github.dexclub.core.api.dex.ExportResult
import io.github.dexclub.core.api.dex.FindClassesRequest
import io.github.dexclub.core.api.dex.FindClassesUsingStringsRequest
import io.github.dexclub.core.api.dex.InspectMethodRequest
import io.github.dexclub.core.api.dex.FindMethodsRequest
import io.github.dexclub.core.api.dex.FindMethodsUsingStringsRequest
import io.github.dexclub.core.api.dex.FindFieldsRequest
import io.github.dexclub.core.api.shared.Operation
import io.github.dexclub.core.api.shared.applyPageWindow
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.api.workspace.WorkspaceResolveError
import io.github.dexclub.core.api.workspace.WorkspaceResolveErrorReason
import io.github.dexclub.core.impl.workspace.model.MaterialInventory
import io.github.dexclub.core.impl.workspace.store.WorkspaceStore

internal class DefaultDexAnalysisService(
    private val store: WorkspaceStore,
    private val capabilityChecker: CapabilityChecker,
    private val queryParser: DexQueryParser,
    private val searchExecutor: DexSearchExecutor,
    private val exportExecutor: DexExportExecutor,
) : DexAnalysisService, AutoCloseable {
    override fun findClasses(workspace: WorkspaceContext, request: FindClassesRequest) =
        runSearch(workspace, Operation.FindClass, request.window, DexAnalysisResultSorter::sortClassHits) { inventory ->
            searchExecutor.findClasses(
                workspace = workspace,
                inventory = inventory,
                query = queryParser.parseFindClass(request.queryText),
            )
        }

    override fun findMethods(workspace: WorkspaceContext, request: FindMethodsRequest) =
        runSearch(workspace, Operation.FindMethod, request.window, DexAnalysisResultSorter::sortMethodHits) { inventory ->
            searchExecutor.findMethods(
                workspace = workspace,
                inventory = inventory,
                query = queryParser.parseFindMethod(request.queryText),
            )
        }

    override fun findFields(workspace: WorkspaceContext, request: FindFieldsRequest) =
        runSearch(workspace, Operation.FindField, request.window, DexAnalysisResultSorter::sortFieldHits) { inventory ->
            searchExecutor.findFields(
                workspace = workspace,
                inventory = inventory,
                query = queryParser.parseFindField(request.queryText),
            )
        }

    override fun findClassesUsingStrings(
        workspace: WorkspaceContext,
        request: FindClassesUsingStringsRequest,
    ) = runSearch(workspace, Operation.FindClass, request.window, DexAnalysisResultSorter::sortClassHits) { inventory ->
        searchExecutor.findClassesUsingStrings(
            workspace = workspace,
            inventory = inventory,
            query = queryParser.parseFindClassUsingStrings(request.queryText),
        )
    }

    override fun findMethodsUsingStrings(
        workspace: WorkspaceContext,
        request: FindMethodsUsingStringsRequest,
    ) = runSearch(workspace, Operation.FindMethod, request.window, DexAnalysisResultSorter::sortMethodHits) { inventory ->
        searchExecutor.findMethodsUsingStrings(
            workspace = workspace,
            inventory = inventory,
            query = queryParser.parseFindMethodUsingStrings(request.queryText),
        )
    }

    override fun inspectMethod(
        workspace: WorkspaceContext,
        request: InspectMethodRequest,
    ) = withInventory(workspace, Operation.FindMethod) { inventory ->
        DexAnalysisResultSorter.sortMethodDetail(
            searchExecutor.inspectMethod(
                workspace = workspace,
                inventory = inventory,
                request = request,
            )
        )
    }

    override fun exportClassDex(workspace: WorkspaceContext, request: ExportClassDexRequest) =
        runExport(workspace, Operation.ExportDex) { inventory ->
            exportExecutor.exportClassDex(
                workspace = workspace,
                inventory = inventory,
                request = request,
            )
        }

    override fun exportClassSmali(workspace: WorkspaceContext, request: ExportClassSmaliRequest) =
        runExport(workspace, Operation.ExportSmali) { inventory ->
            exportExecutor.exportClassSmali(
                workspace = workspace,
                inventory = inventory,
                request = request,
            )
        }

    override fun exportClassJava(workspace: WorkspaceContext, request: ExportClassJavaRequest) =
        runExport(workspace, Operation.ExportJava) { inventory ->
            exportExecutor.exportClassJava(
                workspace = workspace,
                inventory = inventory,
                request = request,
            )
        }

    override fun exportMethodSmali(
        workspace: WorkspaceContext,
        request: ExportMethodSmaliRequest,
    ) = runExport(workspace, Operation.ExportSmali) { inventory ->
        exportExecutor.exportMethodSmali(
            workspace = workspace,
            inventory = inventory,
            request = request,
        )
    }

    override fun exportMethodDex(
        workspace: WorkspaceContext,
        request: ExportMethodDexRequest,
    ) = runExport(workspace, Operation.ExportDex) { inventory ->
        exportExecutor.exportMethodDex(
            workspace = workspace,
            inventory = inventory,
            request = request,
        )
    }

    override fun exportMethodJava(
        workspace: WorkspaceContext,
        request: ExportMethodJavaRequest,
    ) = runExport(workspace, Operation.ExportJava) { inventory ->
        exportExecutor.exportMethodJava(
            workspace = workspace,
            inventory = inventory,
            request = request,
        )
    }

    private inline fun <T> runSearch(
        workspace: WorkspaceContext,
        operation: Operation,
        window: io.github.dexclub.core.api.shared.PageWindow,
        sorter: (List<T>) -> List<T>,
        search: (MaterialInventory) -> List<T>,
    ): List<T> = withInventory(workspace, operation) { inventory ->
        sorter(search(inventory)).applyPageWindow(window)
    }

    private inline fun <T> runExport(
        workspace: WorkspaceContext,
        operation: Operation,
        export: (MaterialInventory) -> T,
    ): T = withInventory(workspace, operation, export)

    private inline fun <T> withInventory(
        workspace: WorkspaceContext,
        operation: Operation,
        block: (MaterialInventory) -> T,
    ): T {
        capabilityChecker.require(workspace, operation)
        return block(loadActiveInventory(workspace))
    }

    private fun loadActiveInventory(workspace: WorkspaceContext): MaterialInventory =
        store.loadSnapshot(workspace.workdir, workspace.activeTargetId)?.inventory
            ?: throw WorkspaceResolveError(
                reason = WorkspaceResolveErrorReason.InvalidSnapshot,
                workdir = workspace.workdir,
                message = "Active target snapshot is missing: ${workspace.activeTargetId}",
            )

    override fun close() {
        searchExecutor.close()
    }
}
