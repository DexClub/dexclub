package io.github.dexclub.core.search

import io.github.dexclub.core.DexEngine
import io.github.dexclub.core.workspace.WorkspaceIndexService
import io.github.dexclub.dexkit.result.ClassData
import kotlinx.coroutines.withTimeoutOrNull

typealias ClassSearchExecutor = suspend (String) -> List<ClassData>

class ClassSearchService(
    private val workspaceIndexService: WorkspaceIndexService,
    private val dexEngineProvider: (() -> DexEngine?)? = null,
    private val searchExecutor: ClassSearchExecutor? = null,
    private val searchTimeoutMs: Long = SEARCH_TIMEOUT_MS,
) {
    suspend fun searchByName(
        keyword: String,
        sortByRelevance: Boolean = true,
    ): List<ClassSearchHit> {
        val normalizedKeyword = keyword.trim()
        require(normalizedKeyword.isNotEmpty()) { "请输入类名关键词" }

        val dexKitResults = executeSearch(normalizedKeyword)
        if (dexKitResults.isEmpty()) {
            return emptyList()
        }

        val classInfosByName = workspaceIndexService.loadSearchClassInfos(
            names = dexKitResults.map(::normalizeDexKitClassName),
        )

        val mappedResults = dexKitResults.mapNotNull { classData ->
            val className = normalizeDexKitClassName(classData)
            val classInfo = classInfosByName[className] ?: return@mapNotNull null
            ClassSearchHit(
                className = classInfo.className,
                classVisualKind = classInfo.classVisualKind,
                descriptor = classData.descriptor,
            )
        }.distinctBy(ClassSearchHit::className)

        return if (sortByRelevance) {
            mappedResults.sortedWith(
                compareBy<ClassSearchHit> { classSearchRank(normalizedKeyword, it.className) }
                    .thenBy { it.className.length }
                    .thenBy { it.className },
            )
        } else {
            mappedResults.sortedBy(ClassSearchHit::className)
        }
    }

    private suspend fun executeSearch(
        keyword: String,
    ): List<ClassData> {
        searchExecutor?.let { executor ->
            return executor(keyword)
        }

        val dexEngine = dexEngineProvider?.invoke()
            ?: throw IllegalStateException("当前工作区没有可搜索的 dex 文件")
        return withTimeoutOrNull(searchTimeoutMs) {
            dexEngine.searchClassesByName(keyword)
        } ?: throw IllegalStateException("类名搜索超时，请缩小范围后重试")
    }

    private companion object {
        private const val SEARCH_TIMEOUT_MS = 15_000L
    }
}
