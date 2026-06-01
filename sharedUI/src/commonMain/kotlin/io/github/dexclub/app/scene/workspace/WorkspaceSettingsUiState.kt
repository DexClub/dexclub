package io.github.dexclub.app.scene.workspace

import io.github.dexclub.settings.AppSettings

data class WorkspaceSettingsUiState(
    val smaliUnicodeDecodeEnabled: Boolean = AppSettings().smaliUnicodeDecode,
    val javaUnicodeDecodeEnabled: Boolean = AppSettings().javaUnicodeDecode,
    val codeScrollPastEnd: Int = AppSettings().codeScrollPastEnd,
)

internal fun buildWorkspaceSettingsUiState(
    appSettings: AppSettings,
): WorkspaceSettingsUiState {
    return WorkspaceSettingsUiState(
        smaliUnicodeDecodeEnabled = appSettings.smaliUnicodeDecode,
        javaUnicodeDecodeEnabled = appSettings.javaUnicodeDecode,
        codeScrollPastEnd = appSettings.codeScrollPastEnd,
    )
}
