package io.github.dexclub.core.impl.dex

import java.util.concurrent.ConcurrentHashMap

internal class ScopedContextCache<Scope : Any, CacheKey : Any, Context : Any>(
    private val closeContext: (Context) -> Unit,
) {
    private val scopeLocks = ConcurrentHashMap<Scope, Any>()
    private val cachedContexts = ConcurrentHashMap<Scope, CachedContext<CacheKey, Context>>()

    fun <T> withContext(
        scope: Scope,
        cacheKey: CacheKey,
        createContext: () -> Context,
        block: (Context) -> T,
    ): T {
        val lock = scopeLocks.computeIfAbsent(scope) { Any() }
        return synchronized(lock) {
            val cached = cachedContexts[scope]
            if (cached != null && cached.cacheKey == cacheKey) {
                return@synchronized block(cached.context)
            }

            val context = createContext()
            val previous = cachedContexts.put(scope, CachedContext(cacheKey = cacheKey, context = context))
            previous?.let { closeContext(it.context) }
            block(context)
        }
    }

    fun closeAll() {
        cachedContexts.keys.toList().forEach { scope ->
            val lock = scopeLocks.computeIfAbsent(scope) { Any() }
            synchronized(lock) {
                cachedContexts.remove(scope)?.let { closeContext(it.context) }
                scopeLocks.remove(scope, lock)
            }
        }
    }

    internal fun size(): Int = cachedContexts.size

    private data class CachedContext<CacheKey : Any, Context : Any>(
        val cacheKey: CacheKey,
        val context: Context,
    )
}
