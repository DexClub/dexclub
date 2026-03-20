package io.github.dexclub.compat

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings

expect val PlatformFile.displayPath: String

expect suspend fun FileKit.openDirectoryPickerCompat(
    title: String? = null,
    directory: PlatformFile? = null,
    dialogSettings: FileKitDialogSettings = FileKitDialogSettings.createDefault(),
): PlatformFile?

expect suspend fun PlatformFile.deleteCompat()

expect fun PlatformFile.findFileCompat(child: String): PlatformFile?

expect fun PlatformFile.directoriesCompat(vararg children: String): PlatformFile

expect suspend fun PlatformFile.unzipTo(
    dir: PlatformFile,
    callback: (String) -> Boolean,
): List<PlatformFile>