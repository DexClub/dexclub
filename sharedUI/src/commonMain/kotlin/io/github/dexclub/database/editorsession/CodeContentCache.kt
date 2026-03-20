package io.github.dexclub.database.editorsession

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.readByteArray

class CodeContentCache {
    private val cacheMutex = Mutex()
    private val contentCache = LinkedHashMap<String, String>(16, 0.75f, true)
    private val maxCacheSize = 20

    suspend fun readFileContent(file: PlatformFile): String {
        val filePath = file.absolutePath()

        cacheMutex.withLock {
            contentCache[filePath]?.let { return it }
        }

        val content = withContext(Dispatchers.IO) {
            val buffered = file.source().buffered()
            try {
                buffered.readByteArray().decodeToString()
            } finally {
                buffered.close()
            }
        }

        cacheMutex.withLock {
            contentCache[filePath]?.let { return it }
            contentCache[filePath] = content
            if (contentCache.size > maxCacheSize) {
                contentCache.remove(contentCache.entries.first().key)
            }
            return content
        }
    }

    suspend fun getCachedContent(filePath: String): String? {
        return cacheMutex.withLock { contentCache[filePath] }
    }

    suspend fun clearCache() {
        cacheMutex.withLock {
            contentCache.clear()
        }
    }

    suspend fun warmUpCache(file: PlatformFile) {
        readFileContent(file)
    }
}

