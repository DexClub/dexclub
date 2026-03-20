package io.github.dexclub.codeview.runtime.degrade

import io.github.dexclub.codeview.core.api.InternalCodeViewApi
import io.github.dexclub.codeview.core.document.CodeDocumentSnapshot

@InternalCodeViewApi
internal interface SurfaceDegradePolicy {
    fun evaluate(snapshot: CodeDocumentSnapshot): DegradeDecision
}

@InternalCodeViewApi
internal sealed interface DegradeDecision {
    data object None : DegradeDecision
    data class LargeFile(val sizeBytes: Int) : DegradeDecision
    data class OversizedFile(val sizeBytes: Int) : DegradeDecision
    data class LongLines(val lineIndices: List<Int>) : DegradeDecision
}
