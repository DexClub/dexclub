package io.github.dexclub.app.scene.workspace

import io.github.dexclub.settings.AppSettings

data class WorkspaceSettingsUiState(
    val autoUnicodeDecodeEnabled: Boolean = AppSettings().autoUnicodeDecode,
    val codeScrollPastEnd: Int = AppSettings().codeScrollPastEnd,
)

internal fun buildWorkspaceSettingsUiState(
    appSettings: AppSettings,
): WorkspaceSettingsUiState {
    return WorkspaceSettingsUiState(
        autoUnicodeDecodeEnabled = appSettings.autoUnicodeDecode,
        codeScrollPastEnd = appSettings.codeScrollPastEnd,
    )
}
