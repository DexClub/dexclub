package io.github.dexclub.codeview.runtime.surface

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.document.CodeDocumentSnapshot
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.core.surface.CodeSurfaceState
import io.github.dexclub.codeview.core.text.TextOffsetRange
import io.github.dexclub.codeview.core.token.CodeTokenKind
import io.github.dexclub.codeview.core.token.CodeTokenSpan
import io.github.dexclub.codeview.language.addon.CodeAddons
import io.github.dexclub.codeview.language.session.CodeLanguageSession
import io.github.dexclub.codeview.runtime.session.LanguageSessionHost
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultCodeSurfaceControllerTest {
    @Test
    fun refreshUpdatesTokensEvenWhenAnnotationBuildFails() = runBlocking {
        val document = CodeDocument.create(
            languageId = CodeLanguageId("java"),
            initialText = "static",
        )
        val session = FakeLanguageSession(
            highlightTokens = { snapshot ->
                listOf(
                    CodeTokenSpan(
                        range = TextOffsetRange(0, snapshot.text.length),
                        kind = if (snapshot.text == "static") CodeTokenKind.Keyword else CodeTokenKind.VariableName,
                    )
                )
            },
            annotations = { snapshot ->
                if (snapshot.text == "abcd") {
                    error("annotation parse failed")
                }
                emptyList()
            },
        )
        val controller = DefaultCodeSurfaceController(
            document = document,
            addons = CodeAddons.build { },
            sessionHost = FakeLanguageSessionHost(session),
        )

        try {
            controller.refresh()
            assertEquals(CodeTokenKind.Keyword, controller.tokens.value.single().kind)

            document.update("abcd")
            controller.refresh()

            assertEquals(CodeTokenKind.VariableName, controller.tokens.value.single().kind)
            assertTrue(controller.annotations.value.isEmpty())
            assertEquals(CodeSurfaceState.Degraded, controller.state.value)
        } finally {
            controller.close()
        }
    }

    @Test
    fun refreshFallsBackToPlainTextWhenHighlightFailsAfterPreviousSuccess() = runBlocking {
        val document = CodeDocument.create(
            languageId = CodeLanguageId("java"),
            initialText = "static",
        )
        val session = FakeLanguageSession(
            highlightTokens = { snapshot ->
                when (snapshot.text) {
                    "static" -> listOf(
                        CodeTokenSpan(
                            range = TextOffsetRange(0, snapshot.text.length),
                            kind = CodeTokenKind.Keyword,
                        )
                    )

                    else -> error("highlight parse failed")
                }
            },
        )
        val controller = DefaultCodeSurfaceController(
            document = document,
            addons = CodeAddons.build { },
            sessionHost = FakeLanguageSessionHost(session),
        )

        try {
            controller.refresh()
            assertEquals(CodeTokenKind.Keyword, controller.tokens.value.single().kind)

            document.update("abcd")
            controller.refresh()

            val token = controller.tokens.value.single()
            assertEquals(CodeTokenKind.PlainText, token.kind)
            assertEquals(TextOffsetRange(0, 4), token.range)
            assertEquals(CodeSurfaceState.Failed, controller.state.value)
        } finally {
            controller.close()
        }
    }

    @Test
    fun refreshSerializesConcurrentLanguageSessionCalls() = runBlocking {
        val document = CodeDocument.create(
            languageId = CodeLanguageId("smali"),
            initialText = ".class public LExample;",
        )
        val session = CountingLanguageSession()
        val controller = DefaultCodeSurfaceController(
            document = document,
            addons = CodeAddons.build { },
            sessionHost = FakeLanguageSessionHost(session),
        )

        try {
            awaitAll(
                async { controller.refresh() },
                async { controller.refresh() },
            )

            assertEquals(1, session.maxConcurrentHighlights, "refresh 应串行调用语言会话，避免并发进入 native parser")
        } finally {
            controller.close()
        }
    }

    private class FakeLanguageSessionHost(
        private val session: CodeLanguageSession,
    ) : LanguageSessionHost {
        override suspend fun getOrCreateSession(
            document: CodeDocument,
            addons: CodeAddons,
        ): CodeLanguageSession = session

        override fun releaseSession(documentId: DocumentId) = Unit

        override fun releaseAll() = Unit
    }

    private class FakeLanguageSession(
        private val highlightTokens: suspend (CodeDocumentSnapshot) -> List<CodeTokenSpan>,
        private val annotations: suspend (CodeDocumentSnapshot) -> List<CodeAnnotation> = { emptyList() },
    ) : CodeLanguageSession {
        override suspend fun highlightTokens(snapshot: CodeDocumentSnapshot): List<CodeTokenSpan> {
            return highlightTokens.invoke(snapshot)
        }

        override suspend fun annotations(snapshot: CodeDocumentSnapshot): List<CodeAnnotation> {
            return annotations.invoke(snapshot)
        }

        override fun close() = Unit
    }

    private class CountingLanguageSession : CodeLanguageSession {
        private val counterMutex = Mutex()
        private var activeHighlights: Int = 0
        var maxConcurrentHighlights: Int = 0
            private set

        override suspend fun highlightTokens(snapshot: CodeDocumentSnapshot): List<CodeTokenSpan> {
            counterMutex.withLock {
                activeHighlights += 1
                if (activeHighlights > maxConcurrentHighlights) {
                    maxConcurrentHighlights = activeHighlights
                }
            }
            try {
                delay(50)
                return listOf(
                    CodeTokenSpan(
                        range = TextOffsetRange(0, snapshot.text.length),
                        kind = CodeTokenKind.PlainText,
                    )
                )
            } finally {
                counterMutex.withLock {
                    activeHighlights -= 1
                }
            }
        }

        override suspend fun annotations(snapshot: CodeDocumentSnapshot): List<CodeAnnotation> {
            return emptyList()
        }

        override fun close() = Unit
    }
}
