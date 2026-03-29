package io.github.dexclub.codeview.language.install

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.language.addon.CodeAddon
import io.github.dexclub.codeview.language.annotation.CodeAnnotationDecoder
import io.github.dexclub.codeview.language.descriptor.CodeLanguageDescriptor
import io.github.dexclub.codeview.language.interaction.CodeContextActionProvider
import io.github.dexclub.codeview.language.interaction.CodeNavigationResolver
import io.github.dexclub.codeview.language.matcher.CodeLanguageMatcher
import io.github.dexclub.codeview.language.session.CodeLanguageProviderFactory

@CodeViewApi
public interface CodeLanguageInstallable : CodeAddon {
    public val descriptor: CodeLanguageDescriptor

    public val matcher: CodeLanguageMatcher?
        get() = null

    public val providerFactory: CodeLanguageProviderFactory

    public val annotationDecoders: List<CodeAnnotationDecoder>
        get() = emptyList()

    public val navigationResolvers: List<CodeNavigationResolver>
        get() = emptyList()

    public val contextActionProviders: List<CodeContextActionProvider>
        get() = emptyList()
}
