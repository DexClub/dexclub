package io.github.dexclub.utils

import kotlinx.serialization.json.Json

object JsonFactory {
    val json by lazy {
        Json { ignoreUnknownKeys = true }
    }

    inline fun <reified T> encodeToString(value: T): String = json.encodeToString(value)

    inline fun <reified T> decodeFromString(string: String): T = json.decodeFromString(string)
}