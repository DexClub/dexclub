package io.github.dexclub.codeview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.language.addon.CodeAddons

@CodeViewApi
@Composable
public fun rememberCodeAddons(
    vararg keys: Any?,
    block: CodeAddons.Builder.() -> Unit,
): CodeAddons {
    return remember(*keys) {
        CodeAddons.build(block)
    }
}
