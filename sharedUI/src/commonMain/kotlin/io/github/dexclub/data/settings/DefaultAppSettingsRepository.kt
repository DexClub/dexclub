package io.github.dexclub.data.settings

import io.github.dexclub.core.settings.AppSettingsRepository
import io.github.dexclub.loggerWarn
import io.github.dexclub.settings.AppSettings
import io.github.xxfast.kstore.KStore

class DefaultAppSettingsRepository(
    private val store: KStore<AppSettings> = sharedStore,
    private val defaultSettings: AppSettings = AppSettings(),
) : AppSettingsRepository {
    override suspend fun load(): AppSettings {
        val settings = runCatching {
            store.get()
        }.onFailure { throwable ->
            loggerWarn(
                text = "加载设置失败，使用默认配置",
                throwable = throwable,
                tag = TAG,
            )
        }.getOrNull()

        return settings ?: restoreDefaultSettings()
    }

    override suspend fun save(settings: AppSettings): AppSettings {
        store.set(settings)
        return settings
    }

    private suspend fun restoreDefaultSettings(): AppSettings {
        return runCatching {
            store.set(defaultSettings)
            defaultSettings
        }.onFailure { throwable ->
            loggerWarn(
                text = "重置默认设置失败，继续使用默认配置",
                throwable = throwable,
                tag = TAG,
            )
        }.getOrDefault(defaultSettings)
    }

    private companion object {
        private const val TAG = "DefaultAppSettingsRepository"

        private val sharedStore: KStore<AppSettings> by lazy(::createAppSettingsStore)
    }
}
