package io.github.dexclub.compat

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import io.github.dexclub.Env
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.coroutines.resume

actual val PlatformFile.displayPath: String
    get() = when (val af = this.androidFile) {
        is AndroidFile.FileWrapper -> af.file.absolutePath
        is AndroidFile.UriWrapper -> {
            val path = af.uri.path?.substringAfterLast(":")!!
            val directory = Environment.getExternalStorageDirectory()
            File(directory, path).absolutePath
        }
    }

actual suspend fun FileKit.openDirectoryPickerCompat(
    title: String?,
    directory: PlatformFile?,
    dialogSettings: FileKitDialogSettings
): PlatformFile? {
    // val resultFile = FileKit.openDirectoryPicker(
    //     title = title,
    //     directory = directory,
    //     dialogSettings = dialogSettings,
    // )
    // if (resultFile != null) {
    //     val af = resultFile.androidFile
    //     if (af is AndroidFile.UriWrapper) {
    //         val uri = af.uri
    //         val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    //         context.applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)
    //     }
    // }

    val activityResultRegistry = Env.activityResultRegistry ?: throw IllegalStateException("ActivityResultRegistry is not initialized")
    val key = UUID.randomUUID().toString()

    val resultFile = suspendCancellableCoroutine { continuation ->
        val contract = ActivityResultContracts.OpenDocumentTree()
        lateinit var launcher: ActivityResultLauncher<Uri?>
        launcher = activityResultRegistry.register(key, contract) { treeUri ->
            if (treeUri != null) {
                // takePersistableUriPermission
                val contentResolver = context.contentResolver
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(treeUri, takeFlags)

                continuation.resume(PlatformFile(AndroidFile.UriWrapper(treeUri)))
            } else {
                continuation.resume(null)
            }

            launcher.unregister()
        }
        continuation.invokeOnCancellation { launcher.unregister() }

        val initialUri = (directory?.androidFile as? AndroidFile.UriWrapper)?.uri
        try {
            launcher.launch(initialUri)
        } catch (e: Exception) {
            launcher.unregister()
            continuation.resume(null)
        }
    }

    return resultFile
}

actual suspend fun PlatformFile.deleteCompat() {
    when (val af = this.androidFile) {
        is AndroidFile.FileWrapper -> af.file.deleteRecursively()
        is AndroidFile.UriWrapper -> {
            val result = DocumentFile.fromSingleUri(FileKit.context, af.uri)?.delete() ?: false
            // if (!result) DocumentFile.fromTreeUri(FileKit.context, af.uri)?.delete()
        }
    }
}

actual fun PlatformFile.findFileCompat(child: String): PlatformFile? {
    return when (val af = this.androidFile) {
        is AndroidFile.FileWrapper -> {
            if (af.file.isFile) throw IllegalStateException("File is not a directory")
            val newFile = File(af.file, child)
            if (newFile.exists()) PlatformFile(newFile) else null
        }

        is AndroidFile.UriWrapper -> {
            if (!DocumentsContract.isTreeUri(af.uri)) throw IllegalStateException("File is not a directory")
            val treeFile = DocumentFile.fromTreeUri(FileKit.context, af.uri)
            treeFile?.findFile(child)?.let { PlatformFile(AndroidFile.UriWrapper(it.uri)) }
        }
    }
}

actual fun PlatformFile.directoriesCompat(vararg children: String): PlatformFile {
    return when (val af = this.androidFile) {
        is AndroidFile.FileWrapper -> {
            if (af.file.isFile) throw IllegalStateException("File is not a directory")
            val newFile = File(af.file, children.joinToString(File.separator))
            if (!newFile.exists()) newFile.mkdirs()
            PlatformFile(newFile)
        }

        is AndroidFile.UriWrapper -> {
            if (!DocumentsContract.isTreeUri(af.uri)) throw IllegalStateException("File is not a directory")
            val children = children.joinToString(File.separator).split(File.separator).filter { it.isNotEmpty() }
            var treeFile = DocumentFile.fromTreeUri(FileKit.context, af.uri)
            for (child in children) {
                val nextTreeFile = treeFile?.findFile(child)
                if (nextTreeFile != null) {
                    treeFile = nextTreeFile
                    continue // child already exists
                }

                val newFile = treeFile?.createDirectory(child)
                treeFile = newFile
            }
            PlatformFile(AndroidFile.UriWrapper(treeFile!!.uri))
        }
    }
}

actual suspend fun PlatformFile.unzipTo(
    dir: PlatformFile,
    callback: (String) -> Boolean,
): List<PlatformFile> {
    val resultFiles = mutableListOf<PlatformFile>()
    val inputStream = getInputStream() ?: return emptyList()
    inputStream.use { input ->
        ZipInputStream(input).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name
                if (entryName.contains("..")) {
                    entry = zis.nextEntry
                    continue
                }

                if (callback(entryName)) {
                    val resultFile = extractEntry(zis, entry, dir)
                    if (resultFile != null) {
                        resultFiles.add(resultFile)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    return resultFiles
}

private fun PlatformFile.getInputStream(): InputStream? {
    return when (val af = this.androidFile) {
        is AndroidFile.FileWrapper -> af.file.inputStream()
        is AndroidFile.UriWrapper -> FileKit.context.contentResolver.openInputStream(af.uri)
    }
}

private fun extractEntry(zis: ZipInputStream, entry: ZipEntry, destDir: PlatformFile): PlatformFile? {
    val androidDest = destDir.androidFile
    return if (androidDest is AndroidFile.FileWrapper) {
        val targetFile = File(androidDest.file, entry.name)
        if (entry.isDirectory) {
            targetFile.mkdirs()
            null
        } else {
            targetFile.parentFile?.mkdirs()
            targetFile.outputStream().use { zis.copyTo(it) }
            PlatformFile(targetFile)
        }
    } else if (androidDest is AndroidFile.UriWrapper) {
        extractToUri(zis, entry, androidDest)
    } else {
        null
    }
}

private fun extractToUri(zis: ZipInputStream, entry: ZipEntry, destWrapper: AndroidFile.UriWrapper): PlatformFile? {
    val context = FileKit.context
    val treeDoc = DocumentFile.fromTreeUri(context, destWrapper.uri) ?: return null

    val pathParts = entry.name.split("/").filter { it.isNotEmpty() }
    if (pathParts.isEmpty()) return null

    var currentDir = treeDoc
    for (i in 0 until pathParts.size - 1) {
        val part = pathParts[i]
        currentDir = currentDir.findFile(part) ?: currentDir.createDirectory(part) ?: return null
    }

    return if (entry.isDirectory) {
        currentDir.findFile(pathParts.last()) ?: currentDir.createDirectory(pathParts.last())
        null
    } else {
        val fileName = pathParts.last()
        val existingFile = currentDir.findFile(fileName)
        val fileDoc = existingFile ?: currentDir.createFile("application/octet-stream", fileName) ?: return null

        context.contentResolver.openOutputStream(fileDoc.uri)?.use { os ->
            zis.copyTo(os)
        }
        PlatformFile(AndroidFile.UriWrapper(fileDoc.uri))
    }
}
