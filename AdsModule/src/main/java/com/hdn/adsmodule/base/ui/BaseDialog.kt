package com.hdn.adsmodule.base.ui

import android.app.Dialog
import android.content.Context
import androidx.annotation.StyleRes

open class BaseDialog @JvmOverloads constructor(
    context: Context,
    @StyleRes themeResId: Int = 0
) : Dialog(context, themeResId) {

    private val immersiveNavigation = ImmersiveNavigationDelegate(context)

    override fun show() {
        immersiveNavigation.applyTo(window)
        super.show()
        window?.decorView?.post { immersiveNavigation.applyTo(window) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) immersiveNavigation.applyTo(window)
    }

    override fun dismiss() {
        super.dismiss()
        immersiveNavigation.restoreHost()
    }
}
