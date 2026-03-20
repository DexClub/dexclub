package io.github.dexclub.android

import android.app.Application
import io.github.dexclub.Env
import io.github.dexclub.codeview.treesitter.CodeTreeSitterAndroid

class DexClubApplication : Application() {
    companion object {
        private lateinit var _instance: DexClubApplication

        val instance: DexClubApplication
            get() = _instance
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this
        initKMP()
    }

    private fun initKMP() {
        Env.application = this
        CodeTreeSitterAndroid.initialize(this)
        Env.onInit()
    }
}
