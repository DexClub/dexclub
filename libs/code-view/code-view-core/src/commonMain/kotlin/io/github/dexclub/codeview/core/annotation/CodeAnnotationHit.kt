package io.github.dexclub.codeview.core.annotation

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.core.document.DocumentId
import io.github.dexclub.codeview.core.document.DocumentRevision
import io.github.dexclub.codeview.core.text.TextOffsetRange

@CodeViewApi
public data class CodeAnnotationHit(
    public val annotation: CodeAnnotation,
    public val range: TextOffsetRange,
    public val trigger: CodeInteractionTrigger,
    public val documentId: DocumentId,
    public val revision: DocumentRevision,
)
