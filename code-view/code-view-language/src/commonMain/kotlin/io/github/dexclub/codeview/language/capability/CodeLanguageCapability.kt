package io.github.dexclub.codeview.language.capability

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public enum class CodeLanguageCapability {
    Highlight,
    Structure,
    Navigation,
    Diagnostics,
}
