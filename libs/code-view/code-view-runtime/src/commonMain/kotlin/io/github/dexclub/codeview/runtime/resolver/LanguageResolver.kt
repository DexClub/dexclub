package io.github.dexclub.codeview.runtime.resolver

import io.github.dexclub.codeview.core.api.InternalCodeViewApi
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.text.CodeTextValue
import io.github.dexclub.codeview.language.addon.CodeAddons
import io.github.dexclub.codeview.language.install.CodeLanguageInstallable

@InternalCodeViewApi
internal class LanguageResolver {
    fun resolve(
        document: CodeDocument,
        addons: CodeAddons,
    ): CodeLanguageInstallable? {
        val snapshot = document.snapshots.value
        val textValue = CodeTextValue(
            text = snapshot.text,
            language = snapshot.languageId,
        )
        return addons.languages.firstOrNull { language ->
            val matcher = language.matcher
            when {
                matcher != null -> matcher.matches(textValue)
                else -> language.descriptor.languageId == snapshot.languageId
            }
        }
    }
}
