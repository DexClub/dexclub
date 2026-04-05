package io.github.dexclub.codeview.treesitter.java.session

import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.core.token.CodeTokenKind
import io.github.dexclub.codeview.core.token.CodeTokenSpan
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JavaLanguageSessionTest {
    @Test
    fun highlightTokensDropsStaleKeywordRangeAfterEdit() = runBlocking {
        val document = CodeDocument.create(
            languageId = CodeLanguageId("java"),
            initialText = "int",
        )
        val session = JavaLanguageSession(document)

        try {
            val initialTokens = session.highlightTokens(document.snapshots.value)
            assertTrue(
                initialTokens.any { token ->
                    token.kind == CodeTokenKind.KeywordType && token.range.start == 0 && token.range.end == 3
                },
                "初始关键字应被识别为类型关键字",
            )

            document.update("你好啊")
            val editedTokens = session.highlightTokens(document.snapshots.value)

            assertFalse(
                editedTokens.any { token ->
                    token.kind == CodeTokenKind.KeywordType && token.range.start == 0 && token.range.end <= 3
                },
                "编辑后不应继续保留旧关键字范围的高亮",
            )
        } finally {
            session.close()
        }
    }

    @Test
    fun highlightTokensDoNotKeepPrimitiveKeywordColorForPartialOrArbitraryEdits() = runBlocking {
        val document = CodeDocument.create(
            languageId = CodeLanguageId("java"),
            initialText = "int",
        )
        val session = JavaLanguageSession(document)

        try {
            assertHasPrimitiveKeywordToken(
                tokens = session.highlightTokens(document.snapshots.value),
                textLength = 3,
            )

            listOf("i", "你", "你好", "你好啊", "f", "fx", "float").forEach { nextText ->
                document.update(nextText)
                val tokens = session.highlightTokens(document.snapshots.value)
                val hasPrimitiveKeyword = tokens.any { token ->
                    token.kind == CodeTokenKind.KeywordType &&
                        token.range.start == 0 &&
                        token.range.end == nextText.length
                }

                if (nextText == "float") {
                    assertTrue(hasPrimitiveKeyword, "完整关键字 float 应恢复类型关键字高亮")
                } else {
                    assertFalse(hasPrimitiveKeyword, "编辑为 $nextText 后不应继续沿用旧关键字高亮")
                }
            }
        } finally {
            session.close()
        }
    }

    @Test
    fun highlightTokensDoNotColorPartialModifierKeywordAsType() = runBlocking {
        val document = CodeDocument.create(
            languageId = CodeLanguageId("java"),
            initialText = "public final int value;",
        )
        val session = JavaLanguageSession(document)

        try {
            val validTokens = session.highlightTokens(document.snapshots.value)
            assertTrue(
                validTokens.any { token ->
                    token.kind == CodeTokenKind.Keyword &&
                        token.range.start == 0 &&
                        token.range.end == "public".length
                },
                "完整 public 应保持关键字高亮",
            )

            listOf("p", "pu", "pub", "publix").forEach { modifierPrefix ->
                val text = "$modifierPrefix final int value;"
                document.update(text)
                val tokens = session.highlightTokens(document.snapshots.value)

                assertFalse(
                    tokens.any { token ->
                        token.kind == CodeTokenKind.TypeName &&
                            token.range.start == 0 &&
                            token.range.end == modifierPrefix.length
                    },
                    "不完整或错误的修饰符 $modifierPrefix 不应被着成类型名颜色",
                )
            }
        } finally {
            session.close()
        }
    }

    @Test
    fun highlightTokensDoNotColorPartialModifierKeywordAsTypeInIncompleteDeclaration() = runBlocking {
        val document = CodeDocument.create(
            languageId = CodeLanguageId("java"),
            initialText = "public final int",
        )
        val session = JavaLanguageSession(document)

        try {
            val validTokens = session.highlightTokens(document.snapshots.value)
            assertTrue(
                validTokens.any { token ->
                    token.kind == CodeTokenKind.Keyword &&
                        token.range.start == 0 &&
                        token.range.end == "public".length
                },
                "完整 public 应保持关键字高亮",
            )

            listOf("p", "pu", "pub", "publix").forEach { modifierPrefix ->
                val text = "$modifierPrefix final int"
                document.update(text)
                val tokens = session.highlightTokens(document.snapshots.value)

                assertFalse(
                    tokens.any { token ->
                        token.kind == CodeTokenKind.TypeName &&
                            token.range.start == 0 &&
                            token.range.end == modifierPrefix.length
                    },
                    "不完整或错误的修饰符 $modifierPrefix 在不完整声明中不应被着成类型名颜色，实际 tokens=${
                        tokens.joinToString { token ->
                            "${token.kind}:${token.range.start}-${token.range.end}"
                        }
                    }",
                )
            }
        } finally {
            session.close()
        }
    }

    @Test
    fun highlightTokensDoNotColorStandalonePartialModifierKeywordAsType() = runBlocking {
        val document = CodeDocument.create(
            languageId = CodeLanguageId("java"),
            initialText = "public",
        )
        val session = JavaLanguageSession(document)

        try {
            val validTokens = session.highlightTokens(document.snapshots.value)
            assertTrue(
                validTokens.any { token ->
                    token.kind == CodeTokenKind.Keyword &&
                        token.range.start == 0 &&
                        token.range.end == "public".length
                },
                "完整 public 应保持关键字高亮",
            )

            listOf("p", "pu", "pub", "publix").forEach { modifierPrefix ->
                document.update(modifierPrefix)
                val tokens = session.highlightTokens(document.snapshots.value)

                assertFalse(
                    tokens.any { token ->
                        token.kind == CodeTokenKind.TypeName &&
                            token.range.start == 0 &&
                            token.range.end == modifierPrefix.length
                    },
                    "单独输入 $modifierPrefix 时不应被着成类型名颜色，实际 tokens=${
                        tokens.joinToString { token ->
                            "${token.kind}:${token.range.start}-${token.range.end}"
                        }
                    }",
                )
            }
        } finally {
            session.close()
        }
    }

    @Test
    fun highlightTokensKeepWholeFileHighlightAfterMalformedLineAndUnicodeCommentEdit() = runBlocking {
        val initialText = """
            // default package

            import com.ss.android.ugc.aweme.utils.EventBusWrapper;
            import kotlin.Unit;
            import kotlin.jvm.functions.Function0;
            import kotlin.jvm.internal.CallableReference;
            import kotlin.jvm.internal.FunctionReferenceImpl;

            public /* synthetic */ class MemoriesBottomUIModule${'$'}presenter${'$'}2 extends FunctionReferenceImpl implements Function0<Unit> {
                public MemoriesBottomUIModule${'$'}presenter${'$'}2(Object obj) {
                    afastgagqgatawdgwargabg
                    super(0, obj, MemoriesBottomUIModule.class, "unbind", "unbind()V", 0);
                }

                public final Object invoke() {
                    Object obj = ((CallableReference) this).receiver;
                    obj.getClass();
                    EventBusWrapper.unregister(obj);
                    return Unit.INSTANCE;
                }
            }
        """.trimIndent()
        val editedText = """
            // default package

            import com.ss.android.ugc.aweme.utils.EventBusWrapper;
            import kotlin.Unit;
            import kotlin.jvm.functions.Function0;
            import kotlin.jvm.internal.CallableReference;
            import kotlin.jvm.internal.FunctionReferenceImpl;

            public /* synthetic */ class MemoriesBottomUIModule${'$'}presenter${'$'}2 extends FunctionReferenceImpl implements Function0<Unit> {
                public MemoriesBottomUIModule${'$'}presenter${'$'}2(Object obj) {
                    afastgagqgatawdgwargabg //这一行都是橙色
                    super(0, obj, MemoriesBottomUIModule.class, "unbind", "unbind()V", 0); // 高亮不应整行丢失
                }

                public final Object invoke() {
                    Object obj = ((CallableReference) this).receiver;
                    obj.getClass();
                    EventBusWrapper.unregister(obj);
                    return Unit.INSTANCE;
                }
            }
        """.trimIndent()
        val document = CodeDocument.create(
            languageId = CodeLanguageId("java"),
            initialText = initialText,
        )
        val session = JavaLanguageSession(document)

        try {
            val initialTokens = session.highlightTokens(document.snapshots.value)
            assertHasKeywordToken(
                tokens = initialTokens,
                text = initialText,
                tokenText = "public",
            )
            assertFalse(
                initialTokens.hasTypeNameTokenFor("afastgagqgatawdgwargabg", initialText),
                "错误行中的随机小写标识符不应被着成类型名颜色，实际 tokens=${
                    initialTokens.joinToString { token ->
                        "${token.kind}:${token.range.start}-${token.range.end}:${initialText.substring(token.range.start, token.range.end)}"
                    }
                }",
            )

            document.update(editedText)
            val editedTokens = session.highlightTokens(document.snapshots.value)

            assertTrue(editedTokens.isNotEmpty(), "加入中文注释后不应导致整文件 token 丢失")
            assertHasKeywordToken(
                tokens = editedTokens,
                text = editedText,
                tokenText = "public",
            )
            assertHasKeywordToken(
                tokens = editedTokens,
                text = editedText,
                tokenText = "return",
            )
            assertHasCommentTokenContaining(
                tokens = editedTokens,
                text = editedText,
                snippet = "这一行都是橙色",
            )
            assertFalse(
                editedTokens.hasTypeNameTokenFor("afastgagqgatawdgwargabg", editedText),
                "加入注释后，错误行中的随机小写标识符仍不应被着成类型名颜色，实际 tokens=${
                    editedTokens.joinToString { token ->
                        "${token.kind}:${token.range.start}-${token.range.end}:${editedText.substring(token.range.start, token.range.end)}"
                    }
                }",
            )
        } finally {
            session.close()
        }
    }

    @Test
    fun highlightTokensMarksThisAndSuperAsBuiltin() = runBlocking {
        val text = """
            class Demo {
                Demo() {
                    super();
                }

                void call() {
                    this.toString();
                }
            }
        """.trimIndent()
        val document = CodeDocument.create(
            languageId = CodeLanguageId("java"),
            initialText = text,
        )
        val session = JavaLanguageSession(document)

        try {
            val tokens = session.highlightTokens(document.snapshots.value)
            assertTrue(
                tokens.any { token ->
                    token.kind == CodeTokenKind.Builtin &&
                        text.substring(token.range.start, token.range.end) == "super"
                },
                "super 应被标记为 builtin",
            )
            assertTrue(
                tokens.any { token ->
                    token.kind == CodeTokenKind.Builtin &&
                        text.substring(token.range.start, token.range.end) == "this"
                },
                "this 应被标记为 builtin",
            )
        } finally {
            session.close()
        }
    }

    @Test
    fun highlightTokensDoNotColorImportIdentifiersAsBuiltin() = runBlocking {
        val text = """
            import X.0EEo;
            import android.content.Context;
            import kotlin.jvm.internal.Intrinsics;
        """.trimIndent()
        val document = CodeDocument.create(
            languageId = CodeLanguageId("java"),
            initialText = text,
        )
        val session = JavaLanguageSession(document)

        try {
            val tokens = session.highlightTokens(document.snapshots.value)

            assertFalse(
                tokens.any { token ->
                    token.kind == CodeTokenKind.Builtin &&
                        text.substring(token.range.start, token.range.end) in setOf(
                            "X",
                            "android",
                            "content",
                            "Context",
                            "kotlin",
                            "jvm",
                            "internal",
                            "Intrinsics",
                        )
                },
                "import 路径中的普通标识符不应被标记为 builtin，实际 tokens=${
                    tokens.joinToString { token ->
                        "${token.kind}:${token.range.start}-${token.range.end}:${text.substring(token.range.start, token.range.end)}"
                    }
                }",
            )

            assertTrue(
                tokens.any { token ->
                    token.kind == CodeTokenKind.Keyword &&
                        text.substring(token.range.start, token.range.end) == "import"
                },
                "import 关键字应保持关键字高亮",
            )
        } finally {
            session.close()
        }
    }

    private fun assertHasPrimitiveKeywordToken(
        tokens: List<CodeTokenSpan>,
        textLength: Int,
    ) {
        assertTrue(
            tokens.any { token ->
                token.kind == CodeTokenKind.KeywordType &&
                    token.range.start == 0 &&
                    token.range.end == textLength
            },
            "完整 primitive type 应被识别为类型关键字",
        )
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
            "应存在关键字 $tokenText 的高亮",
        )
    }

    private fun assertHasCommentTokenContaining(
        tokens: List<CodeTokenSpan>,
        text: String,
        snippet: String,
    ) {
        assertTrue(
            tokens.any { token ->
                token.kind == CodeTokenKind.Comment &&
                    text.substring(token.range.start, token.range.end).contains(snippet)
            },
            "应存在包含 $snippet 的注释高亮",
        )
    }

    private fun List<CodeTokenSpan>.hasTypeNameTokenFor(
        tokenText: String,
        text: String,
    ): Boolean {
        return any { token ->
            token.kind == CodeTokenKind.TypeName &&
                text.substring(token.range.start, token.range.end) == tokenText
        }
    }
}
