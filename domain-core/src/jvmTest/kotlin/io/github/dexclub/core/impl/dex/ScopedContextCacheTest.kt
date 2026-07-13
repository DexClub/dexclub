package io.github.dexclub.core.impl.dex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ScopedContextCacheTest {
    @Test
    fun reusesContextWithinSameScopeAndCacheKey() {
        val closed = mutableListOf<String>()
        val cache = ScopedContextCache<String, String, String>(
            closeContext = closed::add,
        )
        var created = 0

        val first = cache.withContext(
            scope = "ws:a",
            cacheKey = "fingerprint-1",
            createContext = { "context-${++created}" },
        ) { it }
        val second = cache.withContext(
            scope = "ws:a",
            cacheKey = "fingerprint-1",
            createContext = { "context-${++created}" },
        ) { it }

        assertSame(first, second)
        assertEquals(1, created)
        assertEquals(emptyList(), closed)
    }

    @Test
    fun replacesContextOnlyInsideSameScopeWhenCacheKeyChanges() {
        val closed = mutableListOf<String>()
        val cache = ScopedContextCache<String, String, String>(
            closeContext = closed::add,
        )
        var created = 0

        val first = cache.withContext(
            scope = "ws:a",
            cacheKey = "fingerprint-1",
            createContext = { "context-${++created}" },
        ) { it }
        val second = cache.withContext(
            scope = "ws:a",
            cacheKey = "fingerprint-2",
            createContext = { "context-${++created}" },
        ) { it }

        assertEquals("context-1", first)
        assertEquals("context-2", second)
        assertEquals(listOf("context-1"), closed)
        assertEquals(1, cache.size())
    }

    @Test
    fun keepsIndependentScopesIsolated() {
        val closed = mutableListOf<String>()
        val cache = ScopedContextCache<String, String, String>(
            closeContext = closed::add,
        )
        var created = 0

        val first = cache.withContext(
            scope = "ws:a",
            cacheKey = "fingerprint-1",
            createContext = { "context-${++created}" },
        ) { it }
        val second = cache.withContext(
            scope = "ws:b",
            cacheKey = "fingerprint-1",
            createContext = { "context-${++created}" },
        ) { it }

        assertEquals("context-1", first)
        assertEquals("context-2", second)
        assertEquals(emptyList(), closed)
        assertEquals(2, cache.size())
    }

    @Test
    fun closeScopeClosesOnlyItsCachedContext() {
        val closed = mutableListOf<String>()
        val cache = ScopedContextCache<String, String, String>(
            closeContext = closed::add,
        )

        cache.withContext(
            scope = "ws:a",
            cacheKey = "fingerprint-1",
            createContext = { "context-a" },
        ) { it }
        cache.withContext(
            scope = "ws:b",
            cacheKey = "fingerprint-1",
            createContext = { "context-b" },
        ) { it }

        cache.closeScope("ws:a")

        assertEquals(listOf("context-a"), closed)
        assertEquals(1, cache.size())
    }

    @Test
    fun closeAllClosesEveryCachedContextOnce() {
        val closed = mutableListOf<String>()
        val cache = ScopedContextCache<String, String, String>(
            closeContext = closed::add,
        )
        var created = 0

        cache.withContext(
            scope = "ws:a",
            cacheKey = "fingerprint-1",
            createContext = { "context-${++created}" },
        ) { it }
        cache.withContext(
            scope = "ws:b",
            cacheKey = "fingerprint-1",
            createContext = { "context-${++created}" },
        ) { it }

        cache.closeAll()

        assertEquals(listOf("context-1", "context-2").sorted(), closed.sorted())
        assertEquals(0, cache.size())
    }
}
