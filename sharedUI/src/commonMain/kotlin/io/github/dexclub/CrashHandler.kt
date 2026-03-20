package io.github.dexclub

object DexClubCrashHandler {
    private const val TAG = "DexClubCrashHandler"
    private val installLock = Any()

    @Volatile
    private var installed = false

    fun install() {
        if (installed) return
        synchronized(installLock) {
            if (installed) return
            CrashHandlerPlatform.install { threadName, throwable ->
                runCatching {
                    DexClubLogger.captureUncaughtException(
                        threadName = threadName,
                        throwable = throwable,
                    )
                }.onFailure { loggingError ->
                    LogPlatform.writeConsole(
                        level = LogLevel.ERROR,
                        tag = TAG,
                        text = "未捕获异常日志写盘失败: ${loggingError.message ?: "unknown"}",
                    )
                    LogPlatform.writeConsole(
                        level = LogLevel.ERROR,
                        tag = TAG,
                        text = throwable.stackTraceToString(),
                    )
                }
            }
            installed = true
        }
    }
}

internal expect object CrashHandlerPlatform {
    fun install(onUnhandledException: (threadName: String, throwable: Throwable) -> Unit)
}
