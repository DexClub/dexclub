package io.github.dexclub.core.settings

import io.github.dexclub.settings.AppSettings

interface AppSettingsRepository {
    suspend fun load(): AppSettings

    suspend fun save(settings: AppSettings): AppSettings
}
