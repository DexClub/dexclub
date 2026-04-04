package io.github.dexclub.codeview.treesitter.smali.session

import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.language.CodeLanguageId
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class SmaliLanguageSessionTest {
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

        val tokens = session.highlightTokens(document.snapshots.value)

        assertTrue(tokens.isNotEmpty(), "Smali 高亮不应返回空 token 列表")
    }
}
