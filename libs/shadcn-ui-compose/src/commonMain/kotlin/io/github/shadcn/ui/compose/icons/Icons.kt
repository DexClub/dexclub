package io.github.shadcn.ui.compose.icons

sealed class Icons {
    sealed class Rounded : Icons() {
        data object Filled : Rounded()
    }
}