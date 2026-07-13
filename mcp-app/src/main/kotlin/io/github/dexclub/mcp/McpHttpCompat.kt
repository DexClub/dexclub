package io.github.dexclub.mcp

import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest

internal class CompatibleAcceptingCall(
    private val delegate: ApplicationCall,
) : ApplicationCall by delegate {
    override val request: ApplicationRequest = CompatibleAcceptingRequest(delegate.request, this)
}

private class CompatibleAcceptingRequest(
    private val delegate: ApplicationRequest,
    private val callDelegate: ApplicationCall,
) : ApplicationRequest by delegate {
    override val headers: Headers = CompatibleAcceptingHeaders(delegate.headers)
    override val call: ApplicationCall
        get() = callDelegate
}

private class CompatibleAcceptingHeaders(
    private val delegate: Headers,
) : Headers {
    override val caseInsensitiveName: Boolean
        get() = delegate.caseInsensitiveName

    override fun get(name: String): String? =
        if (name.equals(HttpHeaders.Accept, ignoreCase = true)) {
            compatibleAccept(delegate[HttpHeaders.Accept])
        } else {
            delegate[name]
        }

    override fun getAll(name: String): List<String>? =
        if (name.equals(HttpHeaders.Accept, ignoreCase = true)) {
            listOf(compatibleAccept(delegate[HttpHeaders.Accept]))
        } else {
            delegate.getAll(name)
        }

    override fun names(): Set<String> = delegate.names()

    override fun entries(): Set<Map.Entry<String, List<String>>> =
        delegate.entries().mapTo(linkedSetOf()) { entry ->
            if (entry.key.equals(HttpHeaders.Accept, ignoreCase = true)) {
                object : Map.Entry<String, List<String>> {
                    override val key: String = entry.key
                    override val value: List<String> = listOf(compatibleAccept(entry.value.firstOrNull()))
                }
            } else {
                entry
            }
        }

    override fun isEmpty(): Boolean = delegate.isEmpty()

    override fun contains(name: String): Boolean =
        name.equals(HttpHeaders.Accept, ignoreCase = true) || delegate.contains(name)

    override fun contains(name: String, value: String): Boolean =
        getAll(name)?.any { it.equals(value, ignoreCase = true) } == true

    override fun forEach(body: (String, List<String>) -> Unit) {
        entries().forEach { body(it.key, it.value) }
    }
}

private fun compatibleAccept(original: String?): String =
    buildString {
        val base = original?.trim().orEmpty()
        if (base.isNotEmpty()) {
            append(base)
        }
        if (!base.contains("application/json", ignoreCase = true)) {
            if (isNotEmpty()) append(", ")
            append("application/json")
        }
        if (!base.contains("text/event-stream", ignoreCase = true)) {
            if (isNotEmpty()) append(", ")
            append("text/event-stream")
        }
    }
