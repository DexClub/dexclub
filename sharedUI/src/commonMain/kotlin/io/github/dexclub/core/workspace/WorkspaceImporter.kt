package io.github.dexclub.core.workspace

import io.github.dexclub.Env
import io.github.dexclub.compat.deleteCompat
import io.github.dexclub.compat.directoriesCompat
import io.github.dexclub.compat.displayPath
import io.github.dexclub.compat.findFileCompat
import io.github.dexclub.compat.unzipTo
import io.github.dexclub.core.DexFactory
import io.github.dexclub.loggerError
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.resolve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkspaceImporter(
    private val workspaceRepository: WorkspaceRepository,
) {
    suspend fun importWorkspace(
        workspaceName: String,
        targetFile: PlatformFile,
        onProgress: (String) -> Unit = {},
    ): WorkspaceCreationResult {
        require(workspaceName.isNotBlank()) { "工作区名称不能为空" }

        val workspaceDir = PlatformFile(Env.workspaceDir)
        var projectDir: PlatformFile? = null

        return try {
            projectDir = withContext(Dispatchers.IO) {
                val existed = workspaceDir.findFileCompat(workspaceName)
                if (existed != null) {
                    return@withContext null
                }
                workspaceDir.directoriesCompat(workspaceName)
            }

            val currentProjectDir = projectDir ?: return WorkspaceCreationResult.Failure("已存在同名项目")
            val validDexs = copyAndCollectDexFiles(
                projectDir = currentProjectDir,
                targetFile = targetFile,
                onProgress = onProgress,
            )
            if (validDexs.isEmpty()) {
                withContext(Dispatchers.IO) {
                    currentProjectDir.deleteCompat()
                }
                return WorkspaceCreationResult.Failure("没有有效的Dex文件")
            }

            val record = withContext(Dispatchers.IO) {
                workspaceRepository.insert(
                    WorkspaceRecord(
                        name = workspaceName,
                        absolutePath = currentProjectDir.absolutePath(),
                        displayPath = currentProjectDir.displayPath,
                        dexsAbsolutePathList = validDexs.map { it.absolutePath() },
                        validDexs = validDexs.size,
                    )
                )
            }
            WorkspaceCreationResult.Success(record)
        } catch (throwable: Throwable) {
            loggerError(
                text = "创建项目失败",
                throwable = throwable,
                tag = TAG,
            )
            withContext(Dispatchers.IO) {
                projectDir?.deleteCompat()
            }
            WorkspaceCreationResult.Failure("创建项目失败: ${throwable.message}")
        }
    }

    private suspend fun copyAndCollectDexFiles(
        projectDir: PlatformFile,
        targetFile: PlatformFile,
        onProgress: (String) -> Unit,
    ): List<PlatformFile> {
        return withContext(Dispatchers.IO) {
            when (targetFile.extension) {
                "dex" -> {
                    val isDex = DexFactory.isDex(targetFile)
                    if (!isDex) {
                        return@withContext emptyList()
                    }

                    val dexsDir = projectDir.directoriesCompat("dexs")
                    onProgress("复制Dex: ${targetFile.displayPath} -> ${dexsDir.displayPath}")
                    targetFile.copyTo(dexsDir)
                    listOf(dexsDir.resolve(targetFile.name))
                }

                "apk" -> {
                    val apk = projectDir.directoriesCompat("apk")
                    onProgress("复制Apk: ${targetFile.displayPath} -> ${apk.displayPath}")
                    targetFile.copyTo(apk)

                    val dexsDir = projectDir.directoriesCompat("dexs")
                    val dexs = targetFile.unzipTo(dexsDir) { entryName ->
                        val shouldExtract = entryName.endsWith(".dex")
                        if (shouldExtract) {
                            onProgress("导出Dex: $entryName")
                        }
                        shouldExtract
                    }

                    dexs.filter {
                        onProgress("校验Dex: ${it.displayPath}")
                        val isDex = DexFactory.isDex(it)
                        if (!isDex) {
                            it.delete()
                        }
                        isDex
                    }
                }

                else -> {
                    emptyList()
                }
            }
        }
    }

    companion object {
        private const val TAG = "WorkspaceImporter"
    }
}


sealed interface WorkspaceCreationResult {
    data class Success(val workspace: WorkspaceRecord) : WorkspaceCreationResult

    data class Failure(val message: String) : WorkspaceCreationResult
}
