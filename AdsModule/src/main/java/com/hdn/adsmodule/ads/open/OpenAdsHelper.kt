package com.hdn.adsmodule.ads.open

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Created by Huann on 1/13/2026 9:01 AM
 */


class OpenAdsHelper : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
    private var currentActivity: Activity? = null

    fun setup(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        currentActivity?.let { activity ->
            if (!OpenAds.disableClasses.contains(activity.javaClass)) {
                Overlay.start(activity)
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        updateCurrentActivity(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        Log.w("ActivityTracker", "updateCurrentActivity: ${activity.javaClass.canonicalName}", )

        updateCurrentActivity(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        updateCurrentActivity(activity)
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) currentActivity = null
    }

    private fun updateCurrentActivity(activity: Activity) {
        currentActivity = activity
    }
}
