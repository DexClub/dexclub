package io.github.dexclub.app.scene.workspace

sealed interface WorkspaceUiEffect {
    data class ShowMessage(val message: String) : WorkspaceUiEffect
}
