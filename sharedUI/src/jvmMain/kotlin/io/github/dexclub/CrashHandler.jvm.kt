package io.github.dexclub

internal actual object CrashHandlerPlatform {
    private val installLock = Any()

    @Volatile
    private var installed = false

    actual fun install(onUnhandledException: (threadName: String, throwable: Throwable) -> Unit) {
        if (installed) return
        synchronized(installLock) {
            if (installed) return
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                onUnhandledException(thread.name, throwable)
                if (previousHandler != null) {
                    previousHandler.uncaughtException(thread, throwable)
                } else {
                    thread.threadGroup?.uncaughtException(thread, throwable)
                }
            }
            installed = true
        }
    }
}
