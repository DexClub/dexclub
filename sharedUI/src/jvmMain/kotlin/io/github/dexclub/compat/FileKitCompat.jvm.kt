package io.github.dexclub.compat

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.path
import java.io.File
import java.util.zip.ZipInputStream

actual val PlatformFile.displayPath: String
    get() = this.path

actual suspend fun FileKit.openDirectoryPickerCompat(
    title: String?,
    directory: PlatformFile?,
    dialogSettings: FileKitDialogSettings
): PlatformFile? {
    return FileKit.openDirectoryPicker(
        directory = directory,
        dialogSettings = dialogSettings,
    )
}

actual suspend fun PlatformFile.deleteCompat() {
    this.file.deleteRecursively()
}

actual fun PlatformFile.findFileCompat(child: String): PlatformFile? {
    if (this.file.isFile) throw IllegalStateException("File is not a directory")
    val newFile = File(this.file, child)
    return if (newFile.exists()) PlatformFile(newFile) else null
}

actual fun PlatformFile.directoriesCompat(vararg children: String): PlatformFile {
    if (this.file.isFile) throw IllegalStateException("File is not a directory")
    val newFile = File(this.file, children.joinToString(File.separator))
    if (!newFile.exists()) newFile.mkdirs()
    return PlatformFile(newFile)
}

actual suspend fun PlatformFile.unzipTo(
    dir: PlatformFile,
    callback: (String) -> Boolean,
): List<PlatformFile> {
    if (!dir.file.exists()) throw IllegalArgumentException("dir does not exist: ${dir.path}")
    if (!dir.file.isDirectory) throw IllegalArgumentException("dir is not a directory: ${dir.path}")

    val resultFiles = mutableListOf<PlatformFile>()
    ZipInputStream(file.inputStream()).use { zis ->
        var nextEntry = zis.nextEntry
        while (nextEntry != null) {
            if (!callback(nextEntry.name)) {
                nextEntry = zis.nextEntry
                continue
            }

            val file = File(dir.file, nextEntry.name)
            if (nextEntry.isDirectory) {
                file.mkdirs()
            } else {
                file.outputStream().use { fos ->
                    zis.copyTo(fos)
                }
            }
            resultFiles.add(PlatformFile(file))
            nextEntry = zis.nextEntry
        }
    }

    return resultFiles
}
