package io.github.dexclub.core.impl.dex

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.FieldHit
import io.github.dexclub.core.api.dex.InspectMethodRequest
import io.github.dexclub.core.api.dex.MethodDetail
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.impl.workspace.model.MaterialInventory
import io.github.dexclub.core.impl.workspace.store.WorkspaceStore
import io.github.dexclub.dexkit.DexKitBridge
import io.github.dexclub.dexkit.query.BatchFindClassUsingStrings
import io.github.dexclub.dexkit.query.BatchFindMethodUsingStrings
import io.github.dexclub.dexkit.query.FindClass
import io.github.dexclub.dexkit.query.FindField
import io.github.dexclub.dexkit.query.FindMethod
import io.github.dexclub.dexkit.result.MethodData
import java.nio.file.Path
import java.nio.file.Paths

internal class DefaultDexSearchExecutor(
    private val store: WorkspaceStore,
) : DexSearchExecutor {
    private val apkDexCache = DexSearchApkCache(store)
    private val methodDetailLoader = DexMethodDetailLoader(::loadMethodFromSource)

    private val targetContexts = ScopedContextCache<TargetDexScope, TargetDexCacheKey, TargetDexContext>(
        closeContext = { context -> context.bridge.close() },
    )

    override fun findClasses(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        query: FindClass,
    ): List<ClassHit> {
        ensureDexKitLoaded()
        val workdirPath = Paths.get(workspace.workdir)
        return withTargetDexContext(workspace, workdirPath, inventory) { context ->
            val hits = context.bridge.findClass(query).map { result -> context.toClassHit(result) }
            if (query.findFirst) hits.firstOrNull()?.let(::listOf).orEmpty() else hits
        }
    }

    override fun findMethods(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        query: FindMethod,
    ): List<MethodHit> {
        ensureDexKitLoaded()
        val workdirPath = Paths.get(workspace.workdir)
        return withTargetDexContext(workspace, workdirPath, inventory) { context ->
            val hits = context.bridge.findMethod(query).map { result ->
                val source = context.resolveSource(result.dexId)
                result.toMethodHit(source)
            }
            if (query.findFirst) hits.firstOrNull()?.let(::listOf).orEmpty() else hits
        }
    }

    override fun findFields(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        query: FindField,
    ): List<FieldHit> {
        ensureDexKitLoaded()
        val workdirPath = Paths.get(workspace.workdir)
        return withTargetDexContext(workspace, workdirPath, inventory) { context ->
            val classSourceCache = mutableMapOf<String, MemberSource?>()
            val hits = context.bridge.findField(query).map { result ->
                result.toResolvedFieldHit(context, classSourceCache, ::resolveClassSource)
            }
            if (query.findFirst) hits.firstOrNull()?.let(::listOf).orEmpty() else hits
        }
    }

    override fun findClassesUsingStrings(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        query: BatchFindClassUsingStrings,
    ): List<ClassHit> {
        ensureDexKitLoaded()
        val workdirPath = Paths.get(workspace.workdir)
        return withTargetDexContext(workspace, workdirPath, inventory) { context ->
            context.bridge.batchFindClassUsingStrings(query)
                .values
                .asSequence()
                .flatten()
                .map { result -> context.toClassHit(result) }
                .distinct()
                .toList()
        }
    }

    override fun findMethodsUsingStrings(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        query: BatchFindMethodUsingStrings,
    ): List<MethodHit> {
        ensureDexKitLoaded()
        val workdirPath = Paths.get(workspace.workdir)
        return withTargetDexContext(workspace, workdirPath, inventory) { context ->
            val classSourceCache = mutableMapOf<String, MemberSource?>()
            context.bridge.batchFindMethodUsingStrings(query)
                .values
                .asSequence()
                .flatten()
                .map { result ->
                    result.toResolvedMethodHit(context, classSourceCache, ::resolveClassSource)
                }
                .distinct()
                .toList()
        }
    }

    override fun inspectMethod(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        request: InspectMethodRequest,
    ): MethodDetail {
        ensureDexKitLoaded()
        val workdirPath = Paths.get(workspace.workdir)
        return withTargetDexContext(workspace, workdirPath, inventory) { context ->
            methodDetailLoader.inspectMethod(context, request)
        }
    }

    private fun loadMethodFromSource(dexPath: Path, descriptor: String): MethodData? =
        withSingleSourceBridge(dexPath) { bridge ->
            bridge.getMethodData(descriptor)
        }

    private fun <T> withSingleSourceBridge(dexPath: Path, block: (DexKitBridge) -> T): T {
        val bridge = DexKitBridge(listOf(dexPath.toString()))
        return try {
            block(bridge)
        } finally {
            bridge.close()
        }
    }

    private fun ensureDexKitLoaded() {
        DexKitNativeLoader.ensureLoaded()
    }

    override fun close() {
        targetContexts.closeAll()
    }

    private fun <T> withTargetDexContext(
        workspace: WorkspaceContext,
        workdirPath: Path,
        inventory: MaterialInventory,
        block: (TargetDexContext) -> T,
    ): T =
        targetContexts.withContext(
            scope = TargetDexScope(
                workdir = workspace.workdir,
                activeTargetId = workspace.activeTargetId,
            ),
            cacheKey = TargetDexCacheKey(
                contentFingerprint = workspace.snapshot.contentFingerprint,
            ),
            createContext = {
                buildTargetDexContext(workspace, workdirPath, inventory)
            },
            block = block,
        )

    private fun buildTargetDexContext(
        workspace: WorkspaceContext,
        workdirPath: Path,
        inventory: MaterialInventory,
    ): TargetDexContext {
        val sources = buildTargetDexSources(workspace, workdirPath, inventory)
        return TargetDexContext(
            bridge = DexKitBridge(sources.map { it.dexPath.toString() }),
            sources = sources,
            sourcesByDexId = sources.mapIndexed { index, source -> index to source.memberSource }.toMap(),
        )
    }

    private fun buildTargetDexSources(
        workspace: WorkspaceContext,
        workdirPath: Path,
        inventory: MaterialInventory,
    ): List<TargetDexSource> {
        val sources = mutableListOf<TargetDexSource>()
        inventory.apkFiles.forEach { apkPath ->
            apkDexCache.prepareApkDexFiles(workspace, workdirPath, apkPath).forEach { (entryName, dexPath) ->
                sources += TargetDexSource(
                    dexPath = dexPath,
                    memberSource = MemberSource(sourcePath = apkPath, sourceEntry = entryName),
                )
            }
        }
        inventory.dexFiles.forEach { dexPath ->
            sources += TargetDexSource(
                dexPath = workdirPath.resolve(dexPath).normalize(),
                memberSource = MemberSource(sourcePath = dexPath, sourceEntry = null),
            )
        }
        return sources
    }
}
