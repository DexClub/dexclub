package io.github.dexclub

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

import io.github.vinceglb.filekit.PlatformFile

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

data class LogEvent(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val timestampMillis: Long = LogPlatform.nowMillis(),
    val throwable: Throwable? = null,
    val workspaceId: Long? = null,
    val workspaceName: String? = null,
)

interface Logger {
    fun log(
        level: LogLevel,
        message: String,
        tag: String = DEFAULT_TAG,
        throwable: Throwable? = null,
        workspaceId: Long? = null,
        workspaceName: String? = null,
    )

    companion object {
        const val DEFAULT_TAG = "DexClub"
    }
}

data class LogExportRequest(
    val outputDir: PlatformFile,
)

data class LogExportResult(
    val outputPath: String,
    val fileCount: Int,
)

object DexClubLogger : Logger {
    private const val INTERNAL_TAG = "DexClubLogger"
    private const val QUEUE_CAPACITY = 4096

    private val policy = LogFilePolicy(
        fileNamePrefix = "dexclub",
        retentionDays = 7,
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Channel<LogTask>(capacity = QUEUE_CAPACITY)
    private val startLock = Any()

    @Volatile
    private var started = false

    fun initialize() {
        ensureStarted()
    }

    override fun log(
        level: LogLevel,
        message: String,
        tag: String,
        throwable: Throwable?,
        workspaceId: Long?,
        workspaceName: String?,
    ) {
        ensureStarted()
        val event = createEvent(
            level = level,
            message = message,
            tag = tag,
            throwable = throwable,
            workspaceId = workspaceId,
            workspaceName = workspaceName,
        )

        val accepted = queue.trySend(LogTask.Write(event)).isSuccess
        if (!accepted) {
            fallbackConsole(
                level = LogLevel.ERROR,
                tag = INTERNAL_TAG,
                message = "日志队列不可用，已降级到控制台输出",
            )
            writeImmediately(event)
        }
    }

    fun captureUncaughtException(
        threadName: String,
        throwable: Throwable,
    ) {
        drainPendingTasks()
        writeImmediately(
            createEvent(
                level = LogLevel.ERROR,
                message = "捕获到未处理异常，线程=${threadName.ifBlank { "unknown" }}，准备交由系统结束进程",
                tag = INTERNAL_TAG,
                throwable = throwable,
            ),
        )
    }

    fun debug(
        message: String,
        tag: String = Logger.DEFAULT_TAG,
        throwable: Throwable? = null,
        workspaceId: Long? = null,
        workspaceName: String? = null,
    ) {
        log(
            level = LogLevel.DEBUG,
            message = message,
            tag = tag,
            throwable = throwable,
            workspaceId = workspaceId,
            workspaceName = workspaceName,
        )
    }

    fun info(
        message: String,
        tag: String = Logger.DEFAULT_TAG,
        throwable: Throwable? = null,
        workspaceId: Long? = null,
        workspaceName: String? = null,
    ) {
        log(
            level = LogLevel.INFO,
            message = message,
            tag = tag,
            throwable = throwable,
            workspaceId = workspaceId,
            workspaceName = workspaceName,
        )
    }

    fun warn(
        message: String,
        tag: String = Logger.DEFAULT_TAG,
        throwable: Throwable? = null,
        workspaceId: Long? = null,
        workspaceName: String? = null,
    ) {
        log(
            level = LogLevel.WARN,
            message = message,
            tag = tag,
            throwable = throwable,
            workspaceId = workspaceId,
            workspaceName = workspaceName,
        )
    }

    fun error(
        message: String,
        tag: String = Logger.DEFAULT_TAG,
        throwable: Throwable? = null,
        workspaceId: Long? = null,
        workspaceName: String? = null,
    ) {
        log(
            level = LogLevel.ERROR,
            message = message,
            tag = tag,
            throwable = throwable,
            workspaceId = workspaceId,
            workspaceName = workspaceName,
        )
    }

    suspend fun exportWorkspaceLogs(request: LogExportRequest): Result<LogExportResult> {
        ensureStarted()
        val result = CompletableDeferred<Result<LogExportResult>>()
        return try {
            queue.send(LogTask.Export(request, result))
            result.await()
        } catch (throwable: Throwable) {
            Result.failure(IllegalStateException("日志队列不可用，无法导出日志", throwable))
        }
    }

    private fun ensureStarted() {
        if (started) return
        synchronized(startLock) {
            if (started) return
            started = true
            scope.launch {
                runCatching {
                    LogPlatform.initializeStorage(policy)
                }.onFailure { throwable ->
                    fallbackConsole(
                        level = LogLevel.ERROR,
                        tag = INTERNAL_TAG,
                        message = "日志系统初始化失败，已降级到控制台输出",
                        throwable = throwable,
                    )
                }

                for (task in queue) {
                    when (task) {
                        is LogTask.Write -> handleWrite(task.event)
                        is LogTask.Export -> {
                            val exported = runCatching {
                                LogPlatform.exportLogs(
                                    request = task.request,
                                    policy = policy,
                                )
                            }
                            task.result.complete(exported)
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleWrite(event: LogEvent) {
        val rendered = renderEvent(event)

        LogPlatform.writeConsole(
            level = event.level,
            tag = event.tag,
            text = rendered,
        )

        runCatching {
            LogPlatform.appendLogLine(
                text = rendered,
                timestampMillis = event.timestampMillis,
                policy = policy,
            )
        }.onFailure { throwable ->
            fallbackConsole(
                level = LogLevel.ERROR,
                tag = INTERNAL_TAG,
                message = "日志写盘失败，已降级到控制台输出",
                throwable = throwable,
            )
        }
    }

    private fun writeToConsoleOnly(event: LogEvent) {
        val rendered = renderEvent(event)
        LogPlatform.writeConsole(
            level = event.level,
            tag = event.tag,
            text = rendered,
        )
    }

    private fun writeImmediately(event: LogEvent) {
        val rendered = renderEvent(event)
        LogPlatform.writeConsole(
            level = event.level,
            tag = event.tag,
            text = rendered,
        )

        runCatching {
            LogPlatform.appendLogLineBlocking(
                text = rendered,
                timestampMillis = event.timestampMillis,
                policy = policy,
            )
        }.onFailure { throwable ->
            fallbackConsole(
                level = LogLevel.ERROR,
                tag = INTERNAL_TAG,
                message = "日志同步写盘失败，已降级到控制台输出",
                throwable = throwable,
            )
        }
    }

    private fun fallbackConsole(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        val event = LogEvent(
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
        )
        writeToConsoleOnly(event)
    }

    private fun createEvent(
        level: LogLevel,
        message: String,
        tag: String,
        throwable: Throwable? = null,
        workspaceId: Long? = null,
        workspaceName: String? = null,
    ): LogEvent {
        return LogEvent(
            level = level,
            tag = tag.ifBlank { Logger.DEFAULT_TAG },
            message = message,
            throwable = throwable,
            workspaceId = workspaceId,
            workspaceName = workspaceName?.takeIf { it.isNotBlank() },
        )
    }

    private fun renderEvent(event: LogEvent): String {
        return LogLineCodec.render(
            event = event,
            timestampText = LogPlatform.formatTimestamp(event.timestampMillis),
        )
    }

    private fun drainPendingTasks() {
        while (true) {
            val task = queue.tryReceive().getOrNull() ?: break
            when (task) {
                is LogTask.Write -> writeImmediately(task.event)
                is LogTask.Export -> {
                    task.result.complete(
                        Result.failure(IllegalStateException("应用正在崩溃，日志导出已取消")),
                    )
                }
            }
        }
    }

    private sealed interface LogTask {
        data class Write(val event: LogEvent) : LogTask

        data class Export(
            val request: LogExportRequest,
            val result: CompletableDeferred<Result<LogExportResult>>,
        ) : LogTask
    }
}

internal data class LogFilePolicy(
    val fileNamePrefix: String,
    val retentionDays: Int,
)

internal object LogLineCodec {
    fun render(event: LogEvent, timestampText: String): String {
        val normalizedMessage = event.message
            .replace("\r\n", "\n")
            .replace('\n', ' ')
            .trim()

        val throwableSummary = event.throwable?.let { throwable ->
            buildString {
                append(" | exception=")
                append(sanitizeToken(throwable::class.simpleName ?: "Throwable"))
                val throwableMessage = throwable.message
                    ?.replace("\r\n", "\n")
                    ?.replace('\n', ' ')
                    ?.trim()
                if (!throwableMessage.isNullOrBlank()) {
                    append(": ")
                    append(throwableMessage)
                }
            }
        }.orEmpty()

        val header = buildString {
            append('[')
            append(timestampText)
            append(']')

            append('[')
            append("ts=")
            append(event.timestampMillis)
            append(']')

            append('[')
            append(event.level.name)
            append(']')

            append('[')
            append("tag=")
            append(sanitizeToken(event.tag))
            append(']')

            if (event.workspaceId != null) {
                append('[')
                append("workspaceId=")
                append(event.workspaceId)
                append(']')
            }

            if (!event.workspaceName.isNullOrBlank()) {
                append('[')
                append("workspaceName=")
                append(sanitizeToken(event.workspaceName))
                append(']')
            }

            append(' ')
            append(normalizedMessage)
            append(throwableSummary)
        }

        val stack = event.throwable?.stackTraceToString()?.trim()
        return if (stack.isNullOrEmpty()) {
            header
        } else {
            "$header\n$stack"
        }
    }

    private fun sanitizeToken(token: String): String {
        return token.replace('[', '(').replace(']', ')')
    }
}

internal expect object LogPlatform {
    fun nowMillis(): Long

    fun formatTimestamp(timestampMillis: Long): String

    fun writeConsole(level: LogLevel, tag: String, text: String)

    suspend fun initializeStorage(policy: LogFilePolicy)

    suspend fun appendLogLine(
        text: String,
        timestampMillis: Long,
        policy: LogFilePolicy,
    )

    fun appendLogLineBlocking(
        text: String,
        timestampMillis: Long,
        policy: LogFilePolicy,
    )

    suspend fun exportLogs(
        request: LogExportRequest,
        policy: LogFilePolicy,
    ): LogExportResult
}

fun Any.logger(
    text: String,
    level: LogLevel = LogLevel.INFO,
    throwable: Throwable? = null,
    workspaceId: Long? = null,
    workspaceName: String? = null,
    tag: String = this::class.simpleName ?: Logger.DEFAULT_TAG,
) = DexClubLogger.log(
    level = level,
    message = text,
    tag = tag,
    throwable = throwable,
    workspaceId = workspaceId,
    workspaceName = workspaceName,
)

fun Any.loggerDebug(
    text: String,
    throwable: Throwable? = null,
    workspaceId: Long? = null,
    workspaceName: String? = null,
    tag: String = this::class.simpleName ?: Logger.DEFAULT_TAG,
) = logger(
    text = text,
    level = LogLevel.DEBUG,
    throwable = throwable,
    workspaceId = workspaceId,
    workspaceName = workspaceName,
    tag = tag,
)

fun Any.loggerInfo(
    text: String,
    throwable: Throwable? = null,
    workspaceId: Long? = null,
    workspaceName: String? = null,
    tag: String = this::class.simpleName ?: Logger.DEFAULT_TAG,
) = logger(
    text = text,
    level = LogLevel.INFO,
    throwable = throwable,
    workspaceId = workspaceId,
    workspaceName = workspaceName,
    tag = tag,
)

fun Any.loggerWarn(
    text: String,
    throwable: Throwable? = null,
    workspaceId: Long? = null,
    workspaceName: String? = null,
    tag: String = this::class.simpleName ?: Logger.DEFAULT_TAG,
) = logger(
    text = text,
    level = LogLevel.WARN,
    throwable = throwable,
    workspaceId = workspaceId,
    workspaceName = workspaceName,
    tag = tag,
)

fun Any.loggerError(
    text: String,
    throwable: Throwable? = null,
    workspaceId: Long? = null,
    workspaceName: String? = null,
    tag: String = this::class.simpleName ?: Logger.DEFAULT_TAG,
) = logger(
    text = text,
    level = LogLevel.ERROR,
    throwable = throwable,
    workspaceId = workspaceId,
    workspaceName = workspaceName,
    tag = tag,
)
