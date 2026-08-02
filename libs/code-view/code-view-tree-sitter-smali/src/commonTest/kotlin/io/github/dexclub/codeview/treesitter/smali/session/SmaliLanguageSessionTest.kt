package io.github.dexclub.codeview.treesitter.smali.session

import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.core.token.CodeTokenKind
import io.github.dexclub.codeview.core.token.CodeTokenSpan
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmaliLanguageSessionTest {
    @Test
    fun annotationsKeepClassAndMethodRangesAlignedAfterUnicodePrefix() = runBlocking {
        val text = """
            # 中文注释
            .class public Lcom/example/Demo;
            .super Ljava/lang/Object;

            .method public test()V
                .locals 0
                return-void
            .end method
        """.trimIndent()
        val document = CodeDocument.create(
            languageId = CodeLanguageId("smali"),
            initialText = text,
        )
        val session = SmaliLanguageSession(document)

        try {
            val annotations = session.annotations(document.snapshots.value)
            val classAnnotation = annotations.firstOrNull { annotation -> annotation.kind == "class" }
            val methodAnnotation = annotations.firstOrNull { annotation -> annotation.kind == "method" }

            requireNotNull(classAnnotation) { "应生成类名注解" }
            requireNotNull(methodAnnotation) { "应生成方法名注解" }

            assertEquals("Lcom/example/Demo;", text.substring(classAnnotation.range.start, classAnnotation.range.end))
            assertEquals("test", text.substring(methodAnnotation.range.start, methodAnnotation.range.end))
        } finally {
            session.close()
        }
    }

    @Test
    fun highlightTokensReturnsSpansForBasicSmaliSnippet() = runBlocking {
        val document = CodeDocument.create(
            languageId = CodeLanguageId("smali"),
            initialText = """
                .class public final Lcom/example/Test;
                .super Ljava/lang/Object;

                .method public constructor <init>()V
                    .locals 0
                    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
                    return-void
                .end method
            """.trimIndent(),
        )
        val session = SmaliLanguageSession(document)

        try {
            val tokens = session.highlightTokens(document.snapshots.value)

            assertTrue(tokens.isNotEmpty(), "Smali 高亮不应返回空 token 列表")
        } finally {
            session.close()
        }
    }

    @Test
    fun highlightTokensMarksSmaliDirectivesAsKeywords() = runBlocking {
        val text = """
            .class public final Lcom/example/Test;
            .super Ljava/lang/Object;
            .source "Test.java"
            .implements Ljava/io/Serializable;

            .field private static final TAG:Ljava/lang/String; = "Test"

            .method public constructor <init>()V
                .locals 0
                invoke-direct {p0}, Ljava/lang/Object;-><init>()V
                return-void
            .end method
        """.trimIndent()
        val document = CodeDocument.create(
            languageId = CodeLanguageId("smali"),
            initialText = text,
        )
        val session = SmaliLanguageSession(document)

        try {
            val tokens = session.highlightTokens(document.snapshots.value)

            listOf(
                ".class",
                ".super",
                ".source",
                ".implements",
                ".field",
                ".method",
                ".locals",
                ".end method",
            ).forEach { directive ->
                assertHasKeywordToken(
                    tokens = tokens,
                    text = text,
                    tokenText = directive,
                )
            }
        } finally {
            session.close()
        }
    }

    private fun assertHasKeywordToken(
        tokens: List<CodeTokenSpan>,
        text: String,
        tokenText: String,
    ) {
        assertTrue(
            tokens.any { token ->
                token.kind == CodeTokenKind.Keyword &&
                    text.substring(token.range.start, token.range.end) == tokenText
            },
            "应存在 $tokenText 的关键字高亮，实际 tokens=${
                tokens.joinToString { token ->
                    "${token.kind}:${text.substring(token.range.start, token.range.end)}"
                }
            }",
        )
    }
}
