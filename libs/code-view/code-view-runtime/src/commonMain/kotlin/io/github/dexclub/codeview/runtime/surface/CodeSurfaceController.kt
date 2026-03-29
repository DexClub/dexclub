package io.github.dexclub.codeview.runtime.surface

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.diagnostic.CodeDiagnostic
import io.github.dexclub.codeview.core.surface.CodeSurfaceState
import io.github.dexclub.codeview.core.token.CodeTokenSpan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@CodeViewApi
public interface CodeSurfaceController {
    public val state: StateFlow<CodeSurfaceState>

    public val diagnostics: Flow<CodeDiagnostic>

    public val tokens: StateFlow<List<CodeTokenSpan>>

    public val annotations: StateFlow<List<CodeAnnotation>>

    public suspend fun refresh()
}
