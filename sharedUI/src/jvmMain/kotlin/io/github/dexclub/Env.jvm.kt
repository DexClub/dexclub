package io.github.dexclub

import java.io.File

import io.github.vinceglb.filekit.FileKit

actual object Env {
    actual val platform: String
        get() = "Desktop"

    actual val configsDir: String
        get() = File(System.getProperty("user.home")).resolve(".dexclub").absolutePath

    actual val workspaceDir: String
        get() = File(System.getProperty("user.home")).resolve("DexClubProjects").absolutePath

    actual fun onInit() {
        DexClubCrashHandler.install()
        FileKit.init(appId = "DexClub", filesDir = File(configsDir))
        DexClubLogger.initialize()
    }
}
