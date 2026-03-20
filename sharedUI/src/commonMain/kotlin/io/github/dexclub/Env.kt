package io.github.dexclub

expect object Env {
    val configsDir: String

    val workspaceDir: String

    val platform: String

    fun onInit()
}

val Env.isAndroid: Boolean get() = platform == "Android"

val Env.isDesktop: Boolean get() = platform == "Desktop"
