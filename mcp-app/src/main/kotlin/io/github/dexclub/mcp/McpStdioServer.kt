package io.github.dexclub.mcp

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

internal suspend fun runStdioServer(
    server: Server,
    input: InputStream = System.`in`,
    output: OutputStream = System.out,
) {
    val closed = CompletableDeferred<Unit>()
    val ready = CountDownLatch(1)
    // The SDK must register its session and close handlers before the reader can see EOF.
    val sessionInput = object : FilterInputStream(input) {
        override fun read(): Int {
            ready.await()
            return super.read()
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            ready.await()
            return super.read(buffer, offset, length)
        }
    }
    val transport = StdioServerTransport(
        input = sessionInput.asSource().buffered(),
        output = output.asSink().buffered(),
    )
    try {
        server.createSession(transport)
        transport.onClose { closed.complete(Unit) }
        ready.countDown()
        closed.await()
    } finally {
        ready.countDown()
        try {
            transport.close()
        } finally {
            server.close()
        }
    }
}
