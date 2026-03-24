package io.github.dexclub.app.scene.workspace

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier

internal actual fun Modifier.workspaceSceneSystemBarsPadding(): Modifier {
    return this
        .statusBarsPadding()
        .navigationBarsPadding()
}
