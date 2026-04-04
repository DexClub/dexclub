package io.github.dexclub.codeview.runtime.resolver

import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.language.CodeLanguageFamilyId
import io.github.dexclub.codeview.core.language.CodeLanguageId
import io.github.dexclub.codeview.language.addon.CodeAddons
import io.github.dexclub.codeview.language.descriptor.CodeLanguageDescriptor
import io.github.dexclub.codeview.language.install.CodeLanguageInstallable
import io.github.dexclub.codeview.language.matcher.CodeLanguageMatcher
import io.github.dexclub.codeview.language.session.CodeLanguageProviderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguageResolverTest {
    private val resolver = LanguageResolver()

    @Test
    fun resolvesByDescriptorLanguageIdWhenMatcherIsAbsent() {
        val document = CodeDocument.create(
            languageId = CodeLanguageId("java"),
            initialText = "class Demo {}",
        )
        val language = FakeLanguageInstallable(
            languageId = CodeLanguageId("java"),
            matcher = null,
        )

        val resolved = resolver.resolve(
            document = document,
            addons = CodeAddons.build {
                install(language)
            },
        )

        assertEquals(language, resolved)
    }

    @Test
    fun returnsNullWhenNeitherMatcherNorDescriptorLanguageIdMatches() {
        val document = CodeDocument.create(
            languageId = CodeLanguageId("smali"),
            initialText = ".class public LDemo;",
        )
        val language = FakeLanguageInstallable(
            languageId = CodeLanguageId("java"),
            matcher = null,
        )

        val resolved = resolver.resolve(
            document = document,
            addons = CodeAddons.build {
                install(language)
            },
        )

        assertNull(resolved)
    }

    @Test
    fun matcherStillOverridesDescriptorLanguageIdFallback() {
        val document = CodeDocument.create(
            languageId = CodeLanguageId("smali"),
            initialText = "class Demo {}",
        )
        val language = FakeLanguageInstallable(
            languageId = CodeLanguageId("java"),
            matcher = CodeLanguageMatcher { value ->
                value.text.startsWith("class ")
            },
        )

        val resolved = resolver.resolve(
            document = document,
            addons = CodeAddons.build {
                install(language)
            },
        )

        assertEquals(language, resolved)
    }

    private data class FakeLanguageInstallable(
        private val languageId: CodeLanguageId,
        override val matcher: CodeLanguageMatcher?,
    ) : CodeLanguageInstallable {
        override val descriptor: CodeLanguageDescriptor = CodeLanguageDescriptor(
            languageId = languageId,
            familyId = CodeLanguageFamilyId("test"),
            displayName = languageId.value,
        )

        override val providerFactory: CodeLanguageProviderFactory
            get() = error("providerFactory is not used in LanguageResolverTest")
    }
}
