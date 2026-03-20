package io.github.dexclub

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

actual object Env {
    // Only Android
    lateinit var application: Application

    var activityResultRegistry: ActivityResultRegistry? = null
        private set

    private val activityLifecycleCallbacks by lazy {
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
                if (activity is ComponentActivity) {
                    if (activityResultRegistry == null) {
                        activityResultRegistry = activity.activityResultRegistry
                            .also { FileKit.init(it) }
                    }
                } else {
                    throw Exception("Activity must extend ComponentActivity")
                }
            }

            override fun onActivityDestroyed(activity: Activity) {

            }

            override fun onActivityPaused(activity: Activity) {

            }

            override fun onActivityResumed(activity: Activity) {

            }

            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {

            }

            override fun onActivityStarted(activity: Activity) {

            }

            override fun onActivityStopped(activity: Activity) {

            }
        }
    }

    actual val configsDir: String
        get() = application.filesDir.resolve(".dexclub").absolutePath

    actual val workspaceDir: String
        get() = application.getExternalFilesDir("DexClubProjects")!!.absolutePath

    actual val platform: String
        get() = "Android"

    actual fun onInit() {
        DexClubCrashHandler.install()
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        DexClubLogger.initialize()
    }
}
