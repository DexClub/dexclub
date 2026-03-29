package io.github.shadcn.ui.compose

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

@Immutable
actual class DialogProperties actual constructor(
    dismissOnBackPress: Boolean,
    dismissOnClickOutside: Boolean,
    usePlatformDefaultWidth: Boolean,
    usePlatformInsets: Boolean,
    useSoftwareKeyboardInset: Boolean,
    scrimColor: Color
) {
    actual val dismissOnBackPress: Boolean = dismissOnBackPress
    actual val dismissOnClickOutside: Boolean = dismissOnClickOutside
    actual val usePlatformDefaultWidth: Boolean = usePlatformDefaultWidth
    actual val usePlatformInsets: Boolean = usePlatformInsets
    actual val useSoftwareKeyboardInset: Boolean = useSoftwareKeyboardInset
    actual val scrimColor: Color = scrimColor
}

@Composable
actual fun Dialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    properties: DialogProperties,
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                // 让内容延伸进系统栏区域 (Edge-to-Edge)
                WindowCompat.setDecorFitsSystemWindows(window, false)

                // 设置系统栏颜色与对比度
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }

                // 刘海屏延伸
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.attributes.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }

                // 屏蔽系统自动调整
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

                // 清除窗口全屏标志(防止状态栏隐藏)
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }

            onDispose {}
        }

        val sonnerState = rememberSonnerState()
        CompositionLocalProvider(LocalSonner provides sonnerState) {
            SonnerBox(
                state = sonnerState,
                modifier = Modifier.imePadding(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    content()
                }
            }
        }
    }
}