package com.hdn.adsmodule.base.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// Ẩn navigation bar cho dialog full màn hình. Bản rút gọn, không phụ thuộc BarsUtil.
internal class ImmersiveNavigationDelegate(context: Context) {

    private val hostActivity = context.findActivity()

    @Suppress("DEPRECATION")
    fun applyTo(window: Window?) {
        window ?: return

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        window.decorView.systemUiVisibility =
            (hostActivity?.window?.decorView?.systemUiVisibility ?: 0) or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    fun restoreHost() {
        hostActivity?.window?.let(::applyTo)
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
