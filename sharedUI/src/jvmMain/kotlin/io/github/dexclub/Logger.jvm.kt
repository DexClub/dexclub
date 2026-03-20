package io.github.dexclub

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import io.github.dexclub.compat.displayPath
import io.github.vinceglb.filekit.PlatformFile

internal actual object LogPlatform {
    private const val LOG_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"
    private const val LOG_FILE_DATE_PATTERN = "yyyy-MM-dd"
    private const val EXPORT_TIME_PATTERN = "yyyyMMdd_HHmmss_SSS"

    private val fileWriteLock = Any()
    private val timestampFormatterLocal = ThreadLocal<SimpleDateFormat>()
    private val logFileDateFormatterLocal = ThreadLocal<SimpleDateFormat>()
    private val exportNameFormatterLocal = ThreadLocal<SimpleDateFormat>()

    private var lastCleanupDateKey: String? = null

    actual fun nowMillis(): Long {
        return System.currentTimeMillis()
    }

    actual fun formatTimestamp(timestampMillis: Long): String {
        return timestampFormatter().format(Date(timestampMillis))
    }

    actual fun writeConsole(level: LogLevel, tag: String, text: String) {
        when (level) {
            LogLevel.DEBUG, LogLevel.INFO -> System.out.println(text)
            LogLevel.WARN, LogLevel.ERROR -> System.err.println(text)
        }
    }

    actual suspend fun initializeStorage(policy: LogFilePolicy) = withContext(Dispatchers.IO) {
        synchronized(fileWriteLock) {
            val dir = logsDir()
            ensureDirectory(dir)
            cleanupExpiredLogs(dir, policy)
        }
    }

    actual suspend fun appendLogLine(
        text: String,
        timestampMillis: Long,
        policy: LogFilePolicy,
    ) = withContext(Dispatchers.IO) {
        appendLogLineInternal(
            text = text,
            timestampMillis = timestampMillis,
            policy = policy,
        )
    }

    actual fun appendLogLineBlocking(
        text: String,
        timestampMillis: Long,
        policy: LogFilePolicy,
    ) {
        appendLogLineInternal(
            text = text,
            timestampMillis = timestampMillis,
            policy = policy,
        )
    }

    actual suspend fun exportLogs(
        request: LogExportRequest,
        policy: LogFilePolicy,
    ): LogExportResult = withContext(Dispatchers.IO) {
        synchronized(fileWriteLock) {
            val logDir = logsDir()
            ensureDirectory(logDir)
            cleanupExpiredLogs(logDir, policy)

            val logFiles = listLogFiles(
                dir = logDir,
                policy = policy,
            ).sortedBy { it.name }
            if (logFiles.isEmpty()) {
                throw IllegalStateException("日志目录为空，无法导出")
            }

            val outputFile = createOutputFile(
                outputDir = request.outputDir,
                fileName = buildExportFileName(),
            )

            openOutputStream(outputFile).use { outputStream ->
                ZipOutputStream(outputStream).use { zipOutputStream ->
                    logFiles.forEach { file ->
                        zipOutputStream.putNextEntry(ZipEntry("logs/${file.name}"))
                        file.inputStream().use { input ->
                            input.copyTo(zipOutputStream)
                        }
                        zipOutputStream.closeEntry()
                    }
                }
            }

            LogExportResult(
                outputPath = outputFile.displayPath,
                fileCount = logFiles.size,
            )
        }
    }

    private fun createOutputFile(
        outputDir: PlatformFile,
        fileName: String,
    ): PlatformFile {
        val dir = outputDir.file
        ensureDirectory(dir)

        val outputFile = File(dir, fileName)
        if (outputFile.exists()) {
            outputFile.delete()
        }
        outputFile.createNewFile()
        return PlatformFile(outputFile)
    }

    private fun openOutputStream(file: PlatformFile): OutputStream {
        return FileOutputStream(file.file)
    }

    private fun logsDir(): File {
        return File(Env.configsDir).resolve("logs")
    }

    private fun appendLogLineInternal(
        text: String,
        timestampMillis: Long,
        policy: LogFilePolicy,
    ) {
        synchronized(fileWriteLock) {
            val dir = logsDir()
            ensureDirectory(dir)
            maybeCleanupExpiredLogs(
                dir = dir,
                policy = policy,
                timestampMillis = timestampMillis,
            )

            val encoded = (text + "\n").toByteArray(Charsets.UTF_8)
            val logFile = File(dir, buildLogFileName(timestampMillis, policy))
            if (!logFile.exists()) {
                logFile.createNewFile()
            }

            FileOutputStream(logFile, true).use { stream ->
                stream.write(encoded)
                stream.flush()
            }
        }
    }

    private fun maybeCleanupExpiredLogs(
        dir: File,
        policy: LogFilePolicy,
        timestampMillis: Long,
    ) {
        val currentDateKey = logFileDateFormatter().format(Date(timestampMillis))
        if (lastCleanupDateKey == currentDateKey) return
        cleanupExpiredLogs(dir, policy)
        lastCleanupDateKey = currentDateKey
    }

    private fun ensureDirectory(dir: File) {
        if (!dir.exists()) {
            val created = dir.mkdirs()
            require(created) { "无法创建日志目录: ${dir.absolutePath}" }
        }
        require(dir.isDirectory) { "日志目录不是文件夹: ${dir.absolutePath}" }
    }

    private fun cleanupExpiredLogs(
        dir: File,
        policy: LogFilePolicy,
    ) {
        listLogFiles(dir, policy)
            .sortedByDescending { it.name }
            .drop(policy.retentionDays)
            .forEach { file ->
                file.delete()
            }
    }

    private fun listLogFiles(
        dir: File,
        policy: LogFilePolicy,
    ): List<File> {
        if (!dir.exists()) return emptyList()
        val prefix = "${policy.fileNamePrefix}-"
        return dir.listFiles()
            ?.filter { file ->
                file.isFile &&
                    file.name.startsWith(prefix) &&
                    file.name.endsWith(".log") &&
                    isValidLogDateToken(
                        token = file.name.removePrefix(prefix).removeSuffix(".log"),
                    )
            }
            .orEmpty()
    }

    private fun buildLogFileName(
        timestampMillis: Long,
        policy: LogFilePolicy,
    ): String {
        val datePart = logFileDateFormatter().format(Date(timestampMillis))
        return "${policy.fileNamePrefix}-$datePart.log"
    }

    private fun isValidLogDateToken(token: String): Boolean {
        return LOG_FILE_DATE_REGEX.matches(token)
    }

    private fun buildExportFileName(): String {
        val timestampPart = exportNameFormatter().format(Date(nowMillis()))
        return "dexclub-logs-$timestampPart.zip"
    }

    private fun timestampFormatter(): SimpleDateFormat {
        return timestampFormatterLocal.get()
            ?: SimpleDateFormat(LOG_TIME_PATTERN, Locale.US).also(timestampFormatterLocal::set)
    }

    private fun logFileDateFormatter(): SimpleDateFormat {
        return logFileDateFormatterLocal.get()
            ?: SimpleDateFormat(LOG_FILE_DATE_PATTERN, Locale.US).also(logFileDateFormatterLocal::set)
    }

    private fun exportNameFormatter(): SimpleDateFormat {
        return exportNameFormatterLocal.get()
            ?: SimpleDateFormat(EXPORT_TIME_PATTERN, Locale.US).also(exportNameFormatterLocal::set)
    }

    private val LOG_FILE_DATE_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")
}
