package io.github.dexclub.data.settings

import io.github.dexclub.settings.AppSettings
import io.github.xxfast.kstore.KStore

internal const val APP_SETTINGS_FILE_NAME = "setting.json"

internal expect fun createAppSettingsStore(): KStore<AppSettings>
