package io.github.dexclub.settings

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val smaliUnicodeDecode: Boolean = false,
    val javaUnicodeDecode: Boolean = false,
    val codeScrollPastEnd: Int = 5,
    val projectCacheDir: String = "",
)
