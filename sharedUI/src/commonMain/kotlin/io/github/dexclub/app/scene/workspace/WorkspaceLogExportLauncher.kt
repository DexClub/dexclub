package io.github.dexclub.app.scene.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import io.github.dexclub.compat.openDirectoryPickerCompat
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
import kotlinx.coroutines.launch

@Composable
internal fun rememberWorkspaceLogExportLauncher(
    initialDirectoryPath: String,
    onDirectoryPicked: (PlatformFile?) -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val latestOnDirectoryPicked = rememberUpdatedState(onDirectoryPicked)

    return remember(initialDirectoryPath, scope) {
        {
            PickerResultLauncher {
                scope.launch {
                    val outputDir = FileKit.openDirectoryPickerCompat(
                        title = "选择日志导出目录",
                        directory = PlatformFile(initialDirectoryPath),
                        dialogSettings = FileKitDialogSettings.createDefault(),
                    )
                    latestOnDirectoryPicked.value(outputDir)
                }
            }.launch()
        }
    }
}
