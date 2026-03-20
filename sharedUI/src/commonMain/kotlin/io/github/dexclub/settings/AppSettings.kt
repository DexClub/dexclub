package io.github.dexclub.settings

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val autoUnicodeDecode: Boolean = true,
)
