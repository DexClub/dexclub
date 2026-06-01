package io.github.dexclub.core.search

import io.github.dexclub.core.DexEngine
import io.github.dexclub.core.workspace.WorkspaceIndexService
import io.github.dexclub.dexkit.result.MethodData
import kotlinx.coroutines.withTimeoutOrNull

typealias StringSearchExecutor = suspend (String) -> List<MethodData>

class StringSearchService(
    private val workspaceIndexService: WorkspaceIndexService,
    private val dexEngineProvider: (() -> DexEngine?)? = null,
    private val searchExecutor: StringSearchExecutor? = null,
    private val searchTimeoutMs: Long = SEARCH_TIMEOUT_MS,
) {
    suspend fun searchByString(
        keyword: String,
    ): List<StringSearchHit> {
        val normalizedKeyword = keyword.trim()
        require(normalizedKeyword.isNotEmpty()) { "请输入字符串关键词" }

        val dexKitResults = executeSearch(normalizedKeyword)
        if (dexKitResults.isEmpty()) {
            return emptyList()
        }

        val classInfosByName = workspaceIndexService.loadSearchClassInfos(
            names = dexKitResults.map(::normalizeDexKitClassName),
        )

        return dexKitResults.mapNotNull { methodData ->
            val className = normalizeDexKitClassName(methodData)
            val classInfo = classInfosByName[className] ?: return@mapNotNull null
            val matchedStrings = methodData.usingStrings
                .filter { value -> value.contains(normalizedKeyword, ignoreCase = true) }
                .distinct()
            StringSearchHit(
                className = classInfo.className,
                classVisualKind = classInfo.classVisualKind,
                methodDescriptor = methodData.descriptor,
                methodName = methodData.name,
                methodDisplaySignature = buildMethodDisplaySignature(methodData),
                matchedString = matchedStrings.firstOrNull() ?: normalizedKeyword,
                matchedStrings = matchedStrings.ifEmpty { listOf(normalizedKeyword) },
            )
        }.distinctBy(StringSearchHit::methodDescriptor)
            .sortedWith(
                compareBy<StringSearchHit> { it.className }
                    .thenBy { it.methodName }
                    .thenBy { it.methodDescriptor },
            )
    }

    private suspend fun executeSearch(
        keyword: String,
    ): List<MethodData> {
        searchExecutor?.let { executor ->
            return executor(keyword)
        }

        val dexEngine = dexEngineProvider?.invoke()
            ?: throw IllegalStateException("当前工作区没有可搜索的 dex 文件")
        return withTimeoutOrNull(searchTimeoutMs) {
            dexEngine.searchMethodsByString(keyword)
        } ?: throw IllegalStateException("字符串搜索超时，请缩小范围后重试")
    }

    private companion object {
        private const val SEARCH_TIMEOUT_MS = 15_000L
    }
}
