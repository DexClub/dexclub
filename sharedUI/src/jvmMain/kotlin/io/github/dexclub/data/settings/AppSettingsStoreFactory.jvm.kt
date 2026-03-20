package io.github.dexclub.data.settings

import java.io.File

import io.github.dexclub.Env
import io.github.dexclub.settings.AppSettings
import io.github.dexclub.utils.JsonFactory
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path

internal actual fun createAppSettingsStore(): KStore<AppSettings> {
    val configDir = File(Env.configsDir)
    if (configDir.exists()) {
        require(configDir.isDirectory) { "配置目录不是文件夹: ${configDir.absolutePath}" }
    } else {
        require(configDir.mkdirs()) { "创建配置目录失败: ${configDir.absolutePath}" }
    }

    return storeOf(
        file = Path(configDir.resolve(APP_SETTINGS_FILE_NAME).absolutePath),
        default = null,
        json = JsonFactory.json,
    )
}
