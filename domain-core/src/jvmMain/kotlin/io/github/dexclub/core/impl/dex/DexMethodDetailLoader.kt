package io.github.dexclub.core.impl.dex

import io.github.dexclub.core.api.dex.DexInspectError
import io.github.dexclub.core.api.dex.DexInspectErrorReason
import io.github.dexclub.core.api.dex.InspectMethodRequest
import io.github.dexclub.core.api.dex.MethodDetail
import io.github.dexclub.core.api.dex.MethodDetailSection
import io.github.dexclub.core.api.dex.MethodFieldUsage
import io.github.dexclub.core.api.dex.MethodHit
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.dexkit.DexKitBridge
import io.github.dexclub.dexkit.result.MethodData
import java.nio.file.Path

internal class DexMethodDetailLoader(
    private val loadMethodFromSource: (Path, String) -> MethodData?,
) {
    fun inspectMethod(
        context: TargetDexContext,
        request: InspectMethodRequest,
    ): MethodDetail {
        val descriptor = request.descriptor.trim()
        validateMethodDescriptor(descriptor)
        val locatedMethod = resolveUniqueLocatedMethod(context, descriptor, request.source)
        val classSourceCache = mutableMapOf<String, MemberSource?>()
        val callers = request.includes.takeIf { MethodDetailSection.Callers in it }?.let {
            findMethodCallers(context, locatedMethod.method)
        }

        return loadMethodDetail(
            context = context,
            locatedMethod = locatedMethod,
            includes = request.includes,
            classSourceCache = classSourceCache,
            callers = callers,
        )
    }

    private fun loadMethodDetail(
        context: TargetDexContext,
        locatedMethod: LocatedMethod,
        includes: Set<MethodDetailSection>,
        classSourceCache: MutableMap<String, MemberSource?>,
        callers: List<MethodHit>?,
    ): MethodDetail {
        val sourceMethodDetail = loadMethodDetailInSource(
            context = context,
            source = locatedMethod.source,
            descriptor = locatedMethod.method.descriptor,
            includes = includes,
            classSourceCache = classSourceCache,
        )

        return MethodDetail(
            method = locatedMethod.toMethodHit(),
            usingFields = sourceMethodDetail.usingFields,
            callers = callers,
            invokes = sourceMethodDetail.invokes,
            strings = sourceMethodDetail.strings,
            annotations = sourceMethodDetail.annotations,
        )
    }

    private fun loadMethodDetailInSource(
        context: TargetDexContext,
        source: MemberSource,
        descriptor: String,
        includes: Set<MethodDetailSection>,
        classSourceCache: MutableMap<String, MemberSource?>,
    ): MethodDetail =
        loadMethodDetailInBridge(
            context = context,
            bridge = context.bridge,
            descriptor = descriptor,
            source = source,
            includes = includes,
            classSourceCache = classSourceCache,
        )

    private fun loadMethodDetailInBridge(
        context: TargetDexContext,
        bridge: DexKitBridge,
        descriptor: String,
        source: MemberSource,
        includes: Set<MethodDetailSection>,
        classSourceCache: MutableMap<String, MemberSource?>,
    ): MethodDetail =
        MethodDetail(
            method = bridge.getMethodData(descriptor)?.toMethodHit(source)
                ?: error("method not found in resolved source: $descriptor"),
            usingFields = includes.takeIf { MethodDetailSection.UsingFields in it }?.let {
                bridge.getMethodUsingFields(descriptor).map { usage ->
                    MethodFieldUsage(
                        usingType = usage.usingType.toFieldUsageType(),
                        field = usage.field.toResolvedFieldHit(
                            context = context,
                            classSourceCache = classSourceCache,
                            resolveClassSource = ::resolveClassSource,
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
                        resolveClassSource = ::resolveClassSource,
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
                it.toMethodHit(source)
            }
            .distinct()
    }

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
        val matches = context.sources
            .asSequence()
            .filter { candidate ->
                (source.sourcePath == null || candidate.memberSource.sourcePath == source.sourcePath) &&
                    (source.sourceEntry == null || candidate.memberSource.sourceEntry == source.sourceEntry)
            }
            .mapNotNull { candidate ->
                loadMethodFromSource(candidate.dexPath, descriptor)?.let { method ->
                    LocatedMethod(method = method, source = candidate.memberSource)
                }
            }
            .toList()
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
}
