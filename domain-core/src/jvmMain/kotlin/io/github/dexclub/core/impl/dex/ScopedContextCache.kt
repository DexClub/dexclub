package io.github.dexclub.core.impl.dex

import java.util.concurrent.ConcurrentHashMap

internal class ScopedContextCache<Scope : Any, CacheKey : Any, Context : Any>(
    private val closeContext: (Context) -> Unit,
) {
    private val scopeStates = ConcurrentHashMap<Scope, ScopeState<CacheKey, Context>>()

    fun <T> withContext(
        scope: Scope,
        cacheKey: CacheKey,
        createContext: () -> Context,
        block: (Context) -> T,
    ): T {
        val state = retainScope(scope)
        return try {
            synchronized(state.lock) {
                val cached = state.cachedContext
                if (cached != null && cached.cacheKey == cacheKey) {
                    return@synchronized block(cached.context)
                }

                val context = createContext()
                state.cachedContext = CachedContext(cacheKey = cacheKey, context = context)
                cached?.let { closeContext(it.context) }
                block(context)
            }
        } finally {
            releaseScope(scope, state)
        }
    }

    fun closeScope(scope: Scope) {
        val state = scopeStates[scope] ?: return
        synchronized(state.lock) {
            state.cachedContext?.let { closeContext(it.context) }
            state.cachedContext = null
        }
        releaseScopeIfUnused(scope, state)
    }

    fun closeAll() {
        scopeStates.keys.toList().forEach(::closeScope)
    }

    internal fun size(): Int = scopeStates.values.count { state ->
        synchronized(state.lock) { state.cachedContext != null }
    }

    private fun retainScope(scope: Scope): ScopeState<CacheKey, Context> =
        scopeStates.compute(scope) { _, existing ->
            (existing ?: ScopeState()).also { state ->
                synchronized(state.lock) {
                    state.users += 1
                }
            }
        }!!

    private fun releaseScope(scope: Scope, state: ScopeState<CacheKey, Context>) {
        scopeStates.computeIfPresent(scope) { _, current ->
            if (current !== state) {
                current
            } else {
                synchronized(state.lock) {
                    check(state.users > 0) { "scope user count underflow" }
                    state.users -= 1
                    state.takeUnless { it.users == 0 && it.cachedContext == null }
                }
            }
        }
    }

    private fun releaseScopeIfUnused(scope: Scope, state: ScopeState<CacheKey, Context>) {
        scopeStates.computeIfPresent(scope) { _, current ->
            if (current !== state) {
                current
            } else {
                synchronized(state.lock) {
                    state.takeUnless { it.users == 0 && it.cachedContext == null }
                }
            }
        }
    }

    private class ScopeState<CacheKey : Any, Context : Any> {
        val lock = Any()
        var users = 0
        var cachedContext: CachedContext<CacheKey, Context>? = null
    }

    private data class CachedContext<CacheKey : Any, Context : Any>(
        val cacheKey: CacheKey,
        val context: Context,
    )
}
