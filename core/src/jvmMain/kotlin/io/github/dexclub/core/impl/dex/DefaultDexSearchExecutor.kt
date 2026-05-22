package io.github.dexclub.core.impl.dex

import io.github.dexclub.core.api.dex.ClassHit
import io.github.dexclub.core.api.dex.DexInspectError
import io.github.dexclub.core.api.dex.DexInspectErrorReason
import io.github.dexclub.core.api.dex.FieldHit
import io.github.dexclub.core.api.dex.FieldUsageType
import io.github.dexclub.core.api.dex.InspectMethodRequest
import io.github.dexclub.core.api.dex.MethodDetail
import io.github.dexclub.core.api.dex.MethodDetailSection
import io.github.dexclub.core.api.dex.MethodFieldUsage
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.impl.workspace.model.MaterialInventory
import io.github.dexclub.core.impl.workspace.store.WorkspaceStore
import java.nio.file.Files
import io.github.dexclub.dexkit.DexKitBridge
import io.github.dexclub.dexkit.query.ClassMatcher
import io.github.dexclub.dexkit.query.BatchFindClassUsingStrings
import io.github.dexclub.dexkit.query.BatchFindMethodUsingStrings
import io.github.dexclub.dexkit.query.FindClass
import io.github.dexclub.dexkit.query.FindField
import io.github.dexclub.dexkit.query.FindMethod
import io.github.dexclub.dexkit.query.MatchType
import io.github.dexclub.dexkit.query.MethodMatcher
import io.github.dexclub.dexkit.query.MethodsMatcher
import io.github.dexclub.dexkit.query.ParameterMatcher
import io.github.dexclub.dexkit.query.ParametersMatcher
import io.github.dexclub.dexkit.query.StringMatchType
import io.github.dexclub.dexkit.query.StringMatcher
import io.github.dexclub.dexkit.result.ClassData
import io.github.dexclub.dexkit.result.FieldData
import io.github.dexclub.dexkit.result.MethodData
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

internal class DefaultDexSearchExecutor(
    private val store: WorkspaceStore,
) : DexSearchExecutor {
    private val targetContextLock = Any()
    private var cachedTargetDexContext: CachedTargetDexContext? = null

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
                result.toMethodHit(source?.sourcePath, source?.sourceEntry)
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
                result.toResolvedFieldHit(context, classSourceCache)
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
                    result.toResolvedMethodHit(context, classSourceCache)
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
        val descriptor = request.descriptor.trim()
        validateMethodDescriptor(descriptor)
        val workdirPath = Paths.get(workspace.workdir)
        return withTargetDexContext(workspace, workdirPath, inventory) { context ->
            val locatedMethod = resolveUniqueLocatedMethod(context, descriptor, request.source)
            val classSourceCache = mutableMapOf<String, MemberSource?>()
            val callers = request.includes.takeIf { MethodDetailSection.Callers in it }?.let {
                findMethodCallers(context, locatedMethod.method)
            }

            loadMethodDetail(
                context = context,
                locatedMethod = locatedMethod,
                includes = request.includes,
                classSourceCache = classSourceCache,
                callers = callers,
            )
        }
    }

    private fun loadMethodDetail(
        context: TargetDexContext,
        locatedMethod: LocatedMethod,
        includes: Set<MethodDetailSection>,
        classSourceCache: MutableMap<String, MemberSource?>,
        callers: List<MethodHit>?,
    ): MethodDetail {
        val sourceMethodDetail = if (locatedMethod.source.sourceEntry != null) {
            loadMethodDetailInApk(
                context = context,
                relativeApkPath = locatedMethod.source.sourcePath,
                sourceEntry = locatedMethod.source.sourceEntry,
                descriptor = locatedMethod.method.descriptor,
                includes = includes,
                classSourceCache = classSourceCache,
            )
        } else {
            loadMethodDetailInDexFile(
                context = context,
                relativeDexPath = locatedMethod.source.sourcePath,
                descriptor = locatedMethod.method.descriptor,
                includes = includes,
                classSourceCache = classSourceCache,
            )
        }

        return MethodDetail(
            method = locatedMethod.toMethodHit(),
            usingFields = sourceMethodDetail.usingFields,
            callers = callers,
            invokes = sourceMethodDetail.invokes,
            strings = sourceMethodDetail.strings,
            annotations = sourceMethodDetail.annotations,
        )
    }

    private fun loadMethodDetailInApk(
        context: TargetDexContext,
        relativeApkPath: String,
        sourceEntry: String,
        descriptor: String,
        includes: Set<MethodDetailSection>,
        classSourceCache: MutableMap<String, MemberSource?>,
    ): MethodDetail {
        return loadMethodDetailInBridge(
            context = context,
            bridge = context.bridge,
            descriptor = descriptor,
            sourcePath = relativeApkPath,
            sourceEntry = sourceEntry,
            includes = includes,
            classSourceCache = classSourceCache,
        )
    }

    private fun loadMethodDetailInDexFile(
        context: TargetDexContext,
        relativeDexPath: String,
        descriptor: String,
        includes: Set<MethodDetailSection>,
        classSourceCache: MutableMap<String, MemberSource?>,
    ): MethodDetail {
        return loadMethodDetailInBridge(
            context = context,
            bridge = context.bridge,
            descriptor = descriptor,
            sourcePath = relativeDexPath,
            sourceEntry = null,
            includes = includes,
            classSourceCache = classSourceCache,
        )
    }

    private fun loadMethodDetailInBridge(
        context: TargetDexContext,
        bridge: DexKitBridge,
        descriptor: String,
        sourcePath: String,
        sourceEntry: String?,
        includes: Set<MethodDetailSection>,
        classSourceCache: MutableMap<String, MemberSource?>,
    ): MethodDetail =
        MethodDetail(
            method = bridge.getMethodData(descriptor)?.toMethodHit(sourcePath, sourceEntry)
                ?: error("method not found in resolved source: $descriptor"),
            usingFields = includes.takeIf { MethodDetailSection.UsingFields in it }?.let {
                bridge.getMethodUsingFields(descriptor).map { usage ->
                    MethodFieldUsage(
                        usingType = usage.usingType.toFieldUsageType(),
                        field = usage.field.toResolvedFieldHit(
                            context = context,
                            classSourceCache = classSourceCache,
                        ),
                    )
                }
            },
            callers = null,
            invokes = includes.takeIf { MethodDetailSection.Invokes in it }?.let {
                bridge.getMethodInvokes(descriptor).map { invoked ->
                    invoked.toResolvedMethodHit(
                        context = context,
                        classSourceCache = classSourceCache,
                    )
                }
            },
            strings = includes.takeIf { MethodDetailSection.Strings in it }?.let {
                bridge.getMethodUsingStrings(descriptor)
            },
            annotations = includes.takeIf { MethodDetailSection.Annotations in it }?.let {
                bridge.getMethodAnnotations(descriptor)
            },
        )

    private fun findMethodCallers(context: TargetDexContext, method: MethodData): List<MethodHit> {
        val query = buildExactCallersQuery(method)
        return context.bridge.findMethod(query)
            .map {
                val source = context.resolveSource(it.dexId)
                it.toMethodHit(source?.sourcePath, source?.sourceEntry)
            }
            .distinct()
    }

    private fun loadMethodInvokes(
        context: TargetDexContext,
        locatedMethod: LocatedMethod,
        classSourceCache: MutableMap<String, MemberSource?>,
    ): List<MethodHit> =
        if (locatedMethod.source.sourceEntry != null) {
            loadMethodDetailInApk(
                context = context,
                relativeApkPath = locatedMethod.source.sourcePath,
                sourceEntry = locatedMethod.source.sourceEntry,
                descriptor = locatedMethod.method.descriptor,
                includes = setOf(MethodDetailSection.Invokes),
                classSourceCache = classSourceCache,
            ).invokes.orEmpty()
        } else {
            loadMethodDetailInDexFile(
                context = context,
                relativeDexPath = locatedMethod.source.sourcePath,
                descriptor = locatedMethod.method.descriptor,
                includes = setOf(MethodDetailSection.Invokes),
                classSourceCache = classSourceCache,
            ).invokes.orEmpty()
        }

    private fun buildExactCallersQuery(method: MethodData): FindMethod =
        FindMethod(
            matcher = MethodMatcher(
                invokeMethods = MethodsMatcher(
                    methods = mutableListOf(method.toExactMethodMatcher()),
                    matchType = MatchType.Contains,
                ),
            ),
        )

    private fun buildExactMethodQuery(method: MethodData, className: String): FindMethod =
        FindMethod(
            matcher = MethodMatcher(
                name = StringMatcher(value = method.name, matchType = StringMatchType.Equals),
                declaredClass = ClassMatcher(
                    className = StringMatcher(value = className, matchType = StringMatchType.Equals),
                ),
                returnType = ClassMatcher(
                    className = StringMatcher(value = method.returnTypeName, matchType = StringMatchType.Equals),
                ),
                params = ParametersMatcher(
                    params = method.paramTypeNames.map { typeName ->
                        ParameterMatcher(
                            type = ClassMatcher(
                                className = StringMatcher(value = typeName, matchType = StringMatchType.Equals),
                            ),
                        )
                    }.toMutableList(),
                ),
            ),
        )

    private fun buildExactDescriptorMethodQuery(descriptor: String): FindMethod {
        val arrowIndex = descriptor.indexOf("->")
        val classDescriptor = descriptor.substring(0, arrowIndex)
        val methodNameStart = arrowIndex + 2
        val paramsStart = descriptor.indexOf('(', startIndex = methodNameStart)
        val methodName = descriptor.substring(methodNameStart, paramsStart)
        val paramsEnd = descriptor.indexOf(')', startIndex = paramsStart)
        val paramDescriptors = parseParameterTypeDescriptors(descriptor.substring(paramsStart + 1, paramsEnd))
        val returnDescriptor = descriptor.substring(paramsEnd + 1)
        return FindMethod(
            matcher = MethodMatcher(
                name = StringMatcher(value = methodName, matchType = StringMatchType.Equals),
                declaredClass = ClassMatcher(
                    className = StringMatcher(
                        value = descriptorTypeToClassName(classDescriptor),
                        matchType = StringMatchType.Equals,
                    ),
                ),
                returnType = ClassMatcher(
                    className = StringMatcher(
                        value = descriptorTypeToClassName(returnDescriptor),
                        matchType = StringMatchType.Equals,
                    ),
                ),
                params = ParametersMatcher(
                    params = paramDescriptors.map { paramDescriptor ->
                        ParameterMatcher(
                            type = ClassMatcher(
                                className = StringMatcher(
                                    value = descriptorTypeToClassName(paramDescriptor),
                                    matchType = StringMatchType.Equals,
                                ),
                            ),
                        )
                    }.toMutableList(),
                ),
            ),
        )
    }

    private fun parseParameterTypeDescriptors(paramsDescriptor: String): List<String> {
        val descriptors = mutableListOf<String>()
        var index = 0
        while (index < paramsDescriptor.length) {
            val start = index
            while (index < paramsDescriptor.length && paramsDescriptor[index] == '[') {
                index++
            }
            check(index < paramsDescriptor.length) { "invalid parameter descriptor: $paramsDescriptor" }
            if (paramsDescriptor[index] == 'L') {
                val end = paramsDescriptor.indexOf(';', startIndex = index)
                check(end >= 0) { "invalid object parameter descriptor: $paramsDescriptor" }
                descriptors += paramsDescriptor.substring(start, end + 1)
                index = end + 1
            } else {
                descriptors += paramsDescriptor.substring(start, index + 1)
                index++
            }
        }
        return descriptors
    }

    private fun descriptorTypeToClassName(typeDescriptor: String): String =
        when {
            typeDescriptor.startsWith('[') -> typeDescriptor.replace('/', '.')
            typeDescriptor.startsWith('L') && typeDescriptor.endsWith(';') ->
                typeDescriptor.substring(1, typeDescriptor.length - 1).replace('/', '.')
            else -> when (typeDescriptor) {
                "V" -> "void"
                "Z" -> "boolean"
                "B" -> "byte"
                "S" -> "short"
                "C" -> "char"
                "I" -> "int"
                "J" -> "long"
                "F" -> "float"
                "D" -> "double"
                else -> error("unsupported type descriptor: $typeDescriptor")
            }
        }

    private fun MethodData.toExactMethodMatcher(): MethodMatcher =
        MethodMatcher(
            name = StringMatcher(value = name, matchType = StringMatchType.Equals),
            declaredClass = ClassMatcher(
                className = StringMatcher(value = className, matchType = StringMatchType.Equals),
            ),
            returnType = ClassMatcher(
                className = StringMatcher(value = returnTypeName, matchType = StringMatchType.Equals),
            ),
            params = ParametersMatcher(
                params = paramTypeNames.map { typeName ->
                    ParameterMatcher(
                        type = ClassMatcher(
                            className = StringMatcher(value = typeName, matchType = StringMatchType.Equals),
                        ),
                    )
                }.toMutableList(),
            ),
        )

    private fun resolveClassSource(
        context: TargetDexContext,
        className: String,
        classSourceCache: MutableMap<String, MemberSource?>,
    ): MemberSource? =
        classSourceCache.getOrPut(className) {
            context.bridge.getClassData(toTypeSignature(className))
                ?.dexId
                ?.let(context::resolveSource)
        }

    private fun resolveUniqueLocatedMethod(
        context: TargetDexContext,
        descriptor: String,
        source: SourceLocator = SourceLocator(),
    ): LocatedMethod {
        val matches = context.bridge.findMethod(buildExactDescriptorMethodQuery(descriptor))
            .map { method -> LocatedMethod(method = method, source = context.requireSource(method.dexId)) }
            .filter { located ->
                (source.sourcePath == null || located.source.sourcePath == source.sourcePath) &&
                    (source.sourceEntry == null || located.source.sourceEntry == source.sourceEntry)
            }
        return when (matches.size) {
            1 -> matches.single()
            0 -> throw DexInspectError(
                reason = DexInspectErrorReason.MethodNotFound,
                message = buildString {
                    append("method not found: ")
                    append(descriptor)
                    source.describe()?.let {
                        append(" (")
                        append(it)
                        append(')')
                    }
                },
            )
            else -> throw DexInspectError(
                reason = DexInspectErrorReason.AmbiguousMethod,
                message = buildString {
                    append("method resolves to multiple dex sources and inspect-method requires a unique descriptor within the workspace: ")
                    append(descriptor)
                    source.describe()?.let {
                        append(" (")
                        append(it)
                        append(')')
                    }
                },
            )
        }
    }

    private fun prepareApkDexFiles(
        workspace: WorkspaceContext,
        workdirPath: Path,
        relativeApkPath: String,
    ): List<Pair<String, Path>> {
        val apkPath = workdirPath.resolve(relativeApkPath).normalize()
        val cacheDir = Paths.get(store.apkDexDir(workspace.workdir, workspace.activeTargetId))
        val fingerprintFile = cacheDir.resolve(".content-fingerprint")
        Files.createDirectories(cacheDir)

        val cachedFingerprint = if (Files.isRegularFile(fingerprintFile)) {
            Files.readString(fingerprintFile)
        } else {
            null
        }

        if (cachedFingerprint != workspace.snapshot.contentFingerprint) {
            clearDirectory(cacheDir)
            extractApkDexEntries(apkPath, cacheDir)
            Files.writeString(fingerprintFile, workspace.snapshot.contentFingerprint)
        }

        val dexFiles = Files.list(cacheDir).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().endsWith(".dex", ignoreCase = true) }
                .sorted(compareBy({ dexEntrySortKey(it.fileName.toString()).first }, { dexEntrySortKey(it.fileName.toString()).second }, { it.fileName.toString() }))
                .toList()
        }

        check(dexFiles.isNotEmpty()) { "APK does not contain any dex entries: $apkPath" }
        return dexFiles.map { it.fileName.toString() to it }
    }

    private fun extractApkDexEntries(apkPath: Path, cacheDir: Path) {
        ZipFile(apkPath.toFile()).use { zip ->
            val dexEntries = zip.entries().asSequence()
                .filterNot { it.isDirectory }
                .filter { it.name.endsWith(".dex", ignoreCase = true) }
                .sortedWith(compareBy({ dexEntrySortKey(it.name).first }, { dexEntrySortKey(it.name).second }, { it.name }))
                .toList()
            check(dexEntries.isNotEmpty()) { "APK does not contain any dex entries: $apkPath" }
            for (entry in dexEntries) {
                val output = cacheDir.resolve(entry.name)
                zip.getInputStream(entry).use { input ->
                    Files.copy(input, output)
                }
            }
        }
    }

    private fun clearDirectory(path: Path) {
        if (!Files.isDirectory(path)) return
        Files.list(path).use { paths ->
            paths.forEach { child ->
                if (Files.isDirectory(child)) {
                    clearDirectory(child)
                }
                Files.deleteIfExists(child)
            }
        }
    }

    private fun dexEntrySortKey(name: String): Pair<Int, String> {
        if (name == "classes.dex") {
            return 1 to name
        }
        val suffix = name.removePrefix("classes").removeSuffix(".dex")
        return if (suffix.toIntOrNull() != null) {
            suffix.toInt() to name
        } else {
            Int.MAX_VALUE to name
        }
    }

    private fun ensureDexKitLoaded() {
        DexKitNativeLoader.ensureLoaded()
    }

    override fun close() {
        synchronized(targetContextLock) {
            closeCachedTargetDexContextLocked()
        }
    }

    private fun <T> withTargetDexContext(
        workspace: WorkspaceContext,
        workdirPath: Path,
        inventory: MaterialInventory,
        block: (TargetDexContext) -> T,
    ): T =
        synchronized(targetContextLock) {
            block(getOrCreateTargetDexContextLocked(workspace, workdirPath, inventory))
        }

    private fun getOrCreateTargetDexContextLocked(
        workspace: WorkspaceContext,
        workdirPath: Path,
        inventory: MaterialInventory,
    ): TargetDexContext {
        val cacheKey = TargetDexCacheKey(
            workdir = workspace.workdir,
            activeTargetId = workspace.activeTargetId,
            contentFingerprint = workspace.snapshot.contentFingerprint,
        )
        val cached = cachedTargetDexContext
        if (cached != null && cached.cacheKey == cacheKey) {
            return cached.context
        }

        closeCachedTargetDexContextLocked()
        val context = buildTargetDexContext(workspace, workdirPath, inventory)
        cachedTargetDexContext = CachedTargetDexContext(cacheKey = cacheKey, context = context)
        return context
    }

    private fun closeCachedTargetDexContextLocked() {
        val cached = cachedTargetDexContext ?: return
        cachedTargetDexContext = null
        cached.context.bridge.close()
    }

    private fun buildTargetDexContext(
        workspace: WorkspaceContext,
        workdirPath: Path,
        inventory: MaterialInventory,
    ): TargetDexContext {
        val sources = buildTargetDexSources(workspace, workdirPath, inventory)
        return TargetDexContext(
            bridge = DexKitBridge(sources.map { it.dexPath.toString() }),
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
            prepareApkDexFiles(workspace, workdirPath, apkPath).forEach { (entryName, dexPath) ->
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

    private fun validateMethodDescriptor(descriptor: String) {
        val arrowIndex = descriptor.indexOf("->")
        val descriptorStart = descriptor.indexOf('(', startIndex = arrowIndex + 2)
        val descriptorEnd = descriptor.lastIndexOf(')')
        if (
            descriptor.isBlank() ||
            arrowIndex <= 0 ||
            descriptorStart <= arrowIndex + 2 ||
            descriptorEnd < descriptorStart ||
            descriptorEnd == descriptor.lastIndex
        ) {
            throw DexInspectError(
                reason = DexInspectErrorReason.InvalidMethodDescriptor,
                message = "invalid method descriptor: $descriptor",
            )
        }
    }

    private fun MethodData.toMethodHit(sourcePath: String?, sourceEntry: String?): MethodHit =
        MethodHit(
            className = className,
            methodName = name,
            descriptor = descriptor,
            sourcePath = sourcePath,
            sourceEntry = sourceEntry,
        )

    private fun FieldData.toFieldHit(sourcePath: String?, sourceEntry: String?): FieldHit =
        FieldHit(
            className = className,
            fieldName = name,
            descriptor = descriptor,
            sourcePath = sourcePath,
            sourceEntry = sourceEntry,
        )

    private fun TargetDexContext.toClassHit(classData: ClassData): ClassHit {
        val source = resolveSource(classData.dexId)
        return ClassHit(
            className = classData.descriptor,
            sourcePath = source?.sourcePath,
            sourceEntry = source?.sourceEntry,
        )
    }

    private fun MethodData.toResolvedMethodHit(
        context: TargetDexContext,
        classSourceCache: MutableMap<String, MemberSource?>,
    ): MethodHit {
        val source = context.resolveSource(dexId) ?: resolveClassSource(context, className, classSourceCache)
        return toMethodHit(source?.sourcePath, source?.sourceEntry)
    }

    private fun FieldData.toResolvedFieldHit(
        context: TargetDexContext,
        classSourceCache: MutableMap<String, MemberSource?>,
    ): FieldHit {
        val source = context.resolveSource(dexId) ?: resolveClassSource(context, className, classSourceCache)
        return toFieldHit(source?.sourcePath, source?.sourceEntry)
    }

    private fun io.github.dexclub.dexkit.result.FieldUsingType.toFieldUsageType(): FieldUsageType =
        when (this) {
            io.github.dexclub.dexkit.result.FieldUsingType.Read -> FieldUsageType.Read
            io.github.dexclub.dexkit.result.FieldUsingType.Write -> FieldUsageType.Write
        }

    private data class TargetDexSource(
        val dexPath: Path,
        val memberSource: MemberSource,
    )

    private data class TargetDexCacheKey(
        val workdir: String,
        val activeTargetId: String,
        val contentFingerprint: String,
    )

    private data class CachedTargetDexContext(
        val cacheKey: TargetDexCacheKey,
        val context: TargetDexContext,
    )

    private data class TargetDexContext(
        val bridge: DexKitBridge,
        val sourcesByDexId: Map<Int, MemberSource>,
    ) {
        fun resolveSource(dexId: Int): MemberSource? = sourcesByDexId[dexId]

        fun requireSource(dexId: Int): MemberSource =
            checkNotNull(resolveSource(dexId)) { "missing source mapping for dexId=$dexId" }
    }

    private data class LocatedMethod(
        val method: MethodData,
        val source: MemberSource,
    ) {
        fun toMethodHit(): MethodHit =
            MethodHit(
                className = method.className,
                methodName = method.name,
                descriptor = method.descriptor,
                sourcePath = source.sourcePath,
                sourceEntry = source.sourceEntry,
            )
    }

    private data class MemberSource(
        val sourcePath: String,
        val sourceEntry: String?,
    )
}

private fun SourceLocator.describe(): String? =
    when {
        sourcePath != null && sourceEntry != null -> "source_path=$sourcePath, source_entry=$sourceEntry"
        sourcePath != null -> "source_path=$sourcePath"
        sourceEntry != null -> "source_entry=$sourceEntry"
        else -> null
    }
