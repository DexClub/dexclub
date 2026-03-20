package io.github.dexclub.codeview.runtime

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.language.addon.CodeAddons
import io.github.dexclub.codeview.runtime.surface.CodeSurfaceController

@CodeViewApi
public interface CodeRuntime {
    public fun getSurfaceController(
        document: CodeDocument,
        addons: CodeAddons,
    ): CodeSurfaceController

    public fun releaseDocument(documentId: DocumentId)

    public fun close()
}
