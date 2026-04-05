package io.github.dexclub.codeview.compose.internal.viewport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
internal fun rememberCodeViewerVerticalScrollState(): CodeViewerVerticalScrollState {
    return remember { CodeViewerVerticalScrollState() }
}

internal class CodeViewerVerticalScrollState {
    var value by mutableFloatStateOf(0f)
        private set

    private var maxValue by mutableFloatStateOf(0f)

    fun updateBounds(maxValue: Float) {
        val normalizedMaxValue = maxValue.coerceAtLeast(0f)
        this.maxValue = normalizedMaxValue
        if (value > normalizedMaxValue) {
            value = normalizedMaxValue
        }
    }

    suspend fun scrollTo(targetValue: Float) {
        value = targetValue.coerceIn(0f, maxValue)
    }

    fun dispatchRawDelta(delta: Float): Float {
        if (delta == 0f) return 0f
        val previousValue = value
        val nextValue = (previousValue + delta)
            .coerceIn(0f, maxValue)
        value = nextValue
        return nextValue - previousValue
    }
}
