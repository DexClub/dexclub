package io.github.dexclub.codeview.treesitter

import android.content.Context
import android.content.res.AssetManager

object CodeTreeSitterAndroid {
    @Volatile
    private var _assetManager: AssetManager? = null

    fun initialize(context: Context) {
        _assetManager = context.applicationContext.assets
    }

    internal fun requireAssetManager(): AssetManager = _assetManager ?: error(
        "CodeTreeSitterAndroid 尚未初始化，请先在 Android Application 中调用 CodeTreeSitterAndroid.initialize(context)",
    )
}
