package io.github.dexclub.codeview.runtime.surface

import io.github.dexclub.codeview.core.annotation.CodeAnnotation
import io.github.dexclub.codeview.core.api.InternalCodeViewApi
import io.github.dexclub.codeview.core.diagnostic.CodeDiagnostic
import io.github.dexclub.codeview.core.document.CodeDocument
import io.github.dexclub.codeview.core.document.CodeDocumentSnapshot
import io.github.dexclub.codeview.core.surface.CodeSurfaceState
import io.github.dexclub.codeview.core.token.CodeTokenSpan
import io.github.dexclub.codeview.language.addon.CodeAddons
import io.github.dexclub.codeview.language.session.CodeLanguageSession
import io.github.dexclub.codeview.runtime.degrade.DefaultSurfaceDegradePolicy
import io.github.dexclub.codeview.runtime.degrade.DegradeDecision
import io.github.dexclub.codeview.runtime.fallback.PlainTextFallback
import io.github.dexclub.codeview.runtime.reconcile.CapabilityResultReconciler
import io.github.dexclub.codeview.runtime.session.LanguageSessionHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@InternalCodeViewApi
internal class DefaultCodeSurfaceController(
    private val document: CodeDocument,
    private val addons: CodeAddons,
    private val sessionHost: LanguageSessionHost,
) : CodeSurfaceController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val degradePolicy = DefaultSurfaceDegradePolicy()
    private val reconciler = CapabilityResultReconciler()
    private val refreshMutex = Mutex()

    private val _state = MutableStateFlow<CodeSurfaceState>(CodeSurfaceState.Idle)
    override val state: StateFlow<CodeSurfaceState> = _state

    private val _diagnostics = Channel<CodeDiagnostic>(Channel.BUFFERED)
    override val diagnostics: Flow<CodeDiagnostic> = _diagnostics.receiveAsFlow()

    private val _tokens = MutableStateFlow<List<CodeTokenSpan>>(emptyList())
    override val tokens: StateFlow<List<CodeTokenSpan>> = _tokens

    private val _annotations = MutableStateFlow<List<CodeAnnotation>>(emptyList())
    override val annotations: StateFlow<List<CodeAnnotation>> = _annotations

    init {
        scope.launch { refresh() }
    }

    override suspend fun refresh() {
        refreshMutex.withLock {
            withContext(Dispatchers.Default) {
                when (val degradeDecision = degradePolicy.evaluate(document.snapshots.value)) {
                    is DegradeDecision.None -> processWithSession()
                    is DegradeDecision.LargeFile -> processWithSession(degradeDecision)
                    is DegradeDecision.LongLines -> processWithSession(degradeDecision)
                    is DegradeDecision.OversizedFile -> processFallback(degradeDecision)
                }
            }
        }
    }

    private suspend fun processWithSession(
        degradeDecision: DegradeDecision? = null,
    ) {
        _state.value = CodeSurfaceState.Loading
        val session = sessionHost.getOrCreateSession(document, addons)
        if (session == null) {
            processFallback(degradeDecision ?: DegradeDecision.None)
            return
        }

        val snapshot = document.snapshots.value
        val requestId = reconciler.nextSequence()
        val tokenResult = runCatching { session.highlightTokens(snapshot) }
        val annotationResult = runCatching { session.annotations(snapshot) }
        if (!reconciler.shouldAccept(document.documentId, snapshot.revision, requestId)) {
            return
        }

        applyResults(
            snapshot = snapshot,
            tokenResult = tokenResult,
            annotationResult = annotationResult,
        )
        _state.value = when {
            degradeDecision != null -> CodeSurfaceState.Degraded
            tokenResult.isFailure -> CodeSurfaceState.Failed
            annotationResult.isFailure -> CodeSurfaceState.Degraded
            else -> CodeSurfaceState.Ready
        }
        if (degradeDecision != null) {
            emitDegradeWarning(degradeDecision)
        }
    }

    private fun applyResults(
        snapshot: CodeDocumentSnapshot,
        tokenResult: Result<List<CodeTokenSpan>>,
        annotationResult: Result<List<CodeAnnotation>>,
    ) {
        _tokens.value = tokenResult.resolveTokens(snapshot)
        _annotations.value = annotationResult.getOrElse { error ->
            emitDiagnostic(
                level = CodeDiagnostic.Level.Warning,
                code = "ANNOTATION_FAILED",
                message = "Annotation build failed: ${error.message}",
            )
            emptyList()
        }
    }

    private fun Result<List<CodeTokenSpan>>.resolveTokens(
        snapshot: CodeDocumentSnapshot,
    ): List<CodeTokenSpan> {
        val previousTokens = _tokens.value
        return getOrElse { error ->
            sessionHost.invalidateSession(document.documentId)
            emitDiagnostic(
                level = CodeDiagnostic.Level.Error,
                code = "HIGHLIGHT_FAILED",
                message = "Highlight failed: ${error.message}",
            )
            previousTokens.ifEmpty {
                PlainTextFallback.generateTokens(snapshot)
            }
        }
    }

    private fun processFallback(decision: DegradeDecision) {
        val snapshot = document.snapshots.value
        _tokens.value = PlainTextFallback.generateTokens(snapshot)
        _annotations.value = emptyList()
        _state.value = CodeSurfaceState.Degraded
        emitDegradeWarning(decision)
    }

    private fun emitDegradeWarning(decision: DegradeDecision) {
        val message = when (decision) {
            is DegradeDecision.LargeFile -> "Large file (${decision.sizeBytes} bytes)"
            is DegradeDecision.OversizedFile -> "File too large (${decision.sizeBytes} bytes)"
            is DegradeDecision.LongLines -> "Long lines detected"
            DegradeDecision.None -> "Language support unavailable"
        }
        emitDiagnostic(
            level = CodeDiagnostic.Level.Warning,
            code = "DEGRADED",
            message = message,
        )
    }

    private fun emitDiagnostic(
        level: CodeDiagnostic.Level,
        code: String,
        message: String,
    ) {
        _diagnostics.trySend(
            CodeDiagnostic(
                level = level,
                code = code,
                message = message,
            )
        )
    }

    internal fun close() {
        scope.cancel()
        _diagnostics.close()
    }
}
