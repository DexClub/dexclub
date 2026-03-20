package io.github.dexclub.codeview.core.surface

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public enum class CodeSurfaceState {
    Idle,
    Loading,
    Ready,
    Degraded,
    Failed,
}
